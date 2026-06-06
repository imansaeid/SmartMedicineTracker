from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.response import Response
from rest_framework import status
from django.contrib.auth.models import User
from django.contrib.auth.hashers import make_password, check_password
from django.db import connection
from rest_framework_simplejwt.tokens import RefreshToken
from .models import Medicine, UserMedicine, MedicineSchedule, DeviceToken, DoseLog
from .serializers import *


# ─── 1. تسجيل مستخدم جديد ───────────────────────────
@api_view(['POST'])
@permission_classes([AllowAny])
def register(request):
    name = request.data.get('name')
    email = request.data.get('email')
    password = request.data.get('password')

    if not name or not email or not password:
        return Response({'error': 'name, email, and password are required'},
                        status=status.HTTP_400_BAD_REQUEST)

    password_hash = make_password(password)

    with connection.cursor() as cursor:
        cursor.execute("SELECT user_id FROM Users WHERE email = %s", [email])
        if cursor.fetchone():
            return Response({'error': 'Email already registered'},
                            status=status.HTTP_400_BAD_REQUEST)

        cursor.execute(
            "INSERT INTO Users (name, email, password_hash) VALUES (%s, %s, %s)",
            [name, email, password_hash]
        )
        cursor.execute("SELECT TOP 1 user_id FROM Users WHERE email = %s", [email])
        custom_user_id = cursor.fetchone()[0]

    return Response({
        'message': 'Account created successfully',
        'user_id': custom_user_id
    }, status=status.HTTP_201_CREATED)


# ─── 2. تسجيل الدخول ────────────────────────────────
@api_view(['POST'])
@permission_classes([AllowAny])
def login_user(request):
    email = request.data.get('email')
    password = request.data.get('password')

    if not email or not password:
        return Response({'error': 'email and password are required'},
                        status=status.HTTP_400_BAD_REQUEST)

    with connection.cursor() as cursor:
        cursor.execute(
            "SELECT user_id, name, email, password_hash FROM Users WHERE email = %s",
            [email]
        )
        row = cursor.fetchone()

    if not row or not check_password(password, row[3]):
        return Response({'error': 'Invalid email or password'},
                        status=status.HTTP_400_BAD_REQUEST)

    custom_user_id, name = row[0], row[1]
    temp_username = f'user_{custom_user_id}'

    temp_user, _ = User.objects.get_or_create(username=temp_username)

    refresh = RefreshToken.for_user(temp_user)
    return Response({
        'token': str(refresh.access_token),
        'user_id': custom_user_id,
        'username': temp_username,
        'name': name
    })


# ─── 3. البحث عن دواء ───────────────────────────────
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def search_medicine(request):
    query = request.GET.get('q', '')
    if not query:
        return Response({'error': 'Please provide a search term'},
                        status=status.HTTP_400_BAD_REQUEST)

    medicines = Medicine.objects.filter(name__icontains=query)
    serializer = MedicineSerializer(medicines, many=True)
    return Response(serializer.data)


# ─── 4. فحص التضارب ─────────────────────────────────
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def check_interaction(request):
    drug1 = request.GET.get('drug1', '')
    drug2 = request.GET.get('drug2', '')

    result = Medicine.objects.raw(
        """
        SELECT TOP 1 * FROM Medicine_Catalog 
        WHERE name LIKE %s 
        AND conflicts_with LIKE %s
        """,
        [f'%{drug1}%', f'%{drug2}%']
    )

    result_list = list(result)

    if result_list:
        r = result_list[0]
        return Response({
            'has_conflict': True,
            'severity': r.interaction_severity,
            'description': r.interaction_description
        })
    return Response({'has_conflict': False})


# ─── 5. إضافة دواء للمستخدم ─────────────────────────
@api_view(['POST'])
@permission_classes([IsAuthenticated])
def add_medicine(request):
    catalog_id = request.data.get('catalog') or request.data.get('catalog_id')

    # جلب الدواء الجديد
    new_medicine = Medicine.objects.filter(catalog_id=catalog_id).first()
    if not new_medicine:
        return Response({'error': 'Medicine not found'},
                        status=status.HTTP_404_NOT_FOUND)
    custom_user_id = int(request.user.username.split('_')[1])

    # جلب أدوية المستخدم الحالية
    user_medicines = UserMedicine.objects.filter(
        user_id=custom_user_id
    ).select_related('catalog')

    # فحص التضارب مع كل دواء موجود
    highest_severity = None
    conflict_description = ''

    print(f"\n=== INTERACTION CHECK ===")
    print(f"New medicine: {new_medicine.name} | active: {new_medicine.active_ingredient} | conflicts_with: {new_medicine.conflicts_with}")

    for um in user_medicines:
        print(f"Checking against: {um.catalog.name} | active: {um.catalog.active_ingredient} | conflicts_with: {um.catalog.conflicts_with}")

        # Get all conflict entries for existing medicine
        existing_conflicts = Medicine.objects.filter(
            name__icontains=um.catalog.name
        )

        conflict = None
        for ec in existing_conflicts:
            if ec.conflicts_with:
                cw = ec.conflicts_with.lower()
                new_active = new_medicine.active_ingredient.lower()
                new_name   = new_medicine.name.lower()
                print(f"  [DIR1] conflicts_with={cw!r} | new_active={new_active!r} | new_name={new_name!r}")
                if cw in new_active or cw in new_name:
                    conflict = ec
                    print(f"  [DIR1] MATCH!")
                    break

        # Check reverse direction
        if not conflict:
            new_conflicts = Medicine.objects.filter(
                name__icontains=new_medicine.name
            )
            for nc in new_conflicts:
                if nc.conflicts_with:
                    cw = nc.conflicts_with.lower()
                    ex_active = um.catalog.active_ingredient.lower()
                    ex_name   = um.catalog.name.lower()
                    print(f"  [DIR2] conflicts_with={cw!r} | ex_active={ex_active!r} | ex_name={ex_name!r}")
                    if cw in ex_active or cw in ex_name:
                        conflict = nc
                        print(f"  [DIR2] MATCH!")
                        break

        if conflict:
            sev = (conflict.interaction_severity or '').strip().capitalize()
            print(f"  CONFLICT FOUND! severity raw={conflict.interaction_severity!r} -> normalized={sev}")
            if sev == 'High':
                highest_severity = 'High'
                conflict_description = conflict.interaction_description
                break
            elif sev == 'Medium':
                highest_severity = 'Medium'
                conflict_description = conflict.interaction_description
            elif sev == 'Low' and highest_severity is None:
                highest_severity = 'Low'
                conflict_description = conflict.interaction_description

    print(f"=== RESULT: highest_severity={highest_severity!r} ===\n")
    # إذا التضارب عالي — ارفض الإضافة
    if highest_severity == 'High':
        return Response({
            'allowed': False,
            'severity': 'High',
            'message': 'Cannot add this medicine due to high-risk interaction!',
            'description': conflict_description
        }, status=status.HTTP_400_BAD_REQUEST)

    # أضف الدواء
    data = request.data.copy()
    data['user_id'] = custom_user_id
    serializer = UserMedicineSerializer(data=data)
    if serializer.is_valid():
        serializer.save()
        response_data = {
            'allowed': True,
            'message': 'Medicine added successfully',
            'data': serializer.data
        }
        if highest_severity:
            response_data['warning'] = True
            response_data['severity'] = highest_severity
            response_data['description'] = conflict_description
        return Response(response_data, status=status.HTTP_201_CREATED)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


# ─── 5b. عدد التعارضات لأدوية المستخدم ─────────────
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def interaction_count(request):
    custom_user_id = int(request.user.username.split('_')[1])
    user_medicines = list(UserMedicine.objects.filter(
        user_id=custom_user_id
    ).select_related('catalog'))

    count = 0
    checked_pairs = set()

    for i, um1 in enumerate(user_medicines):
        for j, um2 in enumerate(user_medicines):
            if i >= j:
                continue
            pair_key = tuple(sorted([um1.catalog.catalog_id, um2.catalog.catalog_id]))
            if pair_key in checked_pairs:
                continue
            checked_pairs.add(pair_key)

            # فحص الاتجاه الأول
            found = False
            for ec in Medicine.objects.filter(name__icontains=um1.catalog.name):
                if ec.conflicts_with:
                    cw = ec.conflicts_with.lower()
                    if cw in um2.catalog.active_ingredient.lower() or cw in um2.catalog.name.lower():
                        count += 1
                        found = True
                        break
            if found:
                continue

            # فحص الاتجاه العكسي
            for ec in Medicine.objects.filter(name__icontains=um2.catalog.name):
                if ec.conflicts_with:
                    cw = ec.conflicts_with.lower()
                    if cw in um1.catalog.active_ingredient.lower() or cw in um1.catalog.name.lower():
                        count += 1
                        break

    return Response({'interaction_count': count})


# ─── 6. قائمة أدوية المستخدم ────────────────────────
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def user_medicines(request):
    custom_user_id = int(request.user.username.split('_')[1])
    medicines = UserMedicine.objects.filter(
        user_id=custom_user_id
    ).select_related('catalog')
    serializer = UserMedicineSerializer(medicines, many=True)
    return Response(serializer.data)


# ─── 7. حذف دواء ────────────────────────────────────
@api_view(['DELETE'])
@permission_classes([IsAuthenticated])
def delete_medicine(request, medicine_id):
    try:
        medicine = UserMedicine.objects.get(
            medicine_id=medicine_id,
            user_id=int(request.user.username.split('_')[1])
        )
        medicine.delete()
        return Response({'message': 'Medicine deleted successfully'})
    except UserMedicine.DoesNotExist:
        return Response({'error': 'Medicine not found'},
                        status=status.HTTP_404_NOT_FOUND)


# ─── 8. إضافة جدول دواء ─────────────────────────────
@api_view(['POST'])
@permission_classes([IsAuthenticated])
def add_schedule(request):
    serializer = MedicineScheduleSerializer(data=request.data)
    if serializer.is_valid():
        serializer.save()
        return Response(serializer.data, status=status.HTTP_201_CREATED)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


# ─── 9. قائمة جداول الأدوية ─────────────────────────
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def get_schedules(request):
    custom_user_id = int(request.user.username.split('_')[1])
    schedules = MedicineSchedule.objects.filter(
        medicine__user_id=custom_user_id
    )
    serializer = MedicineScheduleSerializer(schedules, many=True)
    return Response(serializer.data)


# ─── 10. حفظ Device Token للـ Firebase ──────────────
@api_view(['POST'])
@permission_classes([IsAuthenticated])
def save_device_token(request):
    token = request.data.get('token')
    device_type = request.data.get('device_type', 'android')

    custom_user_id = int(request.user.username.split('_')[1])
    DeviceToken.objects.update_or_create(
        user_id=custom_user_id,
        defaults={'token': token, 'device_type': device_type}
    )
    return Response({'message': 'Token saved successfully'})


# ─── 11. تحديث دواء ─────────────────────────────────
@api_view(['PUT'])
@permission_classes([IsAuthenticated])
def update_medicine(request, medicine_id):
    custom_user_id = int(request.user.username.split('_')[1])
    try:
        medicine = UserMedicine.objects.get(
            medicine_id=medicine_id,
            user_id=custom_user_id
        )
    except UserMedicine.DoesNotExist:
        return Response({'error': 'Medicine not found'},
                        status=status.HTTP_404_NOT_FOUND)

    # Update allowed fields
    medicine.dosage = request.data.get('dosage', medicine.dosage)
    medicine.instructions = request.data.get('instructions', medicine.instructions)
    medicine.start_date = request.data.get('start_date', medicine.start_date)
    medicine.end_date = request.data.get('end_date', medicine.end_date)
    medicine.duration_type = request.data.get('duration_type', medicine.duration_type)
    medicine.save()

    serializer = UserMedicineSerializer(medicine)
    return Response({
        'message': 'Medicine updated successfully',
        'data': serializer.data
    })


# ─── 12. تسجيل أخذ جرعة ─────────────────────────────
@api_view(['POST'])
@permission_classes([IsAuthenticated])
def mark_dose(request):
    from django.utils import timezone
    custom_user_id = int(request.user.username.split('_')[1])

    medicine_id = request.data.get('medicine_id')
    scheduled_time = request.data.get('scheduled_time')
    taken = request.data.get('taken', True)

    if not medicine_id or not scheduled_time:
        return Response({'error': 'medicine_id and scheduled_time are required'},
                        status=status.HTTP_400_BAD_REQUEST)

    log = DoseLog.objects.create(
        user_id=custom_user_id,
        medicine_id=medicine_id,
        scheduled_time=scheduled_time,
        taken=taken,
        taken_at=timezone.now() if taken else None
    )

    return Response({
        'message': 'Dose logged successfully',
        'log_id': log.log_id,
        'taken': taken
    }, status=status.HTTP_201_CREATED)


# ─── 13. تاريخ الجرعات ──────────────────────────────
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def dose_history(request):
    custom_user_id = int(request.user.username.split('_')[1])
    logs = DoseLog.objects.filter(user_id=custom_user_id).order_by('-scheduled_time')
    serializer = DoseLogSerializer(logs, many=True)
    return Response(serializer.data)


# ─── 14. تحديث بيانات المستخدم ──────────────────────
@api_view(['PUT'])
@permission_classes([IsAuthenticated])
def update_profile(request):
    custom_user_id = int(request.user.username.split('_')[1])
    name = request.data.get('name')
    email = request.data.get('email')

    if not name and not email:
        return Response({'error': 'Provide name or email to update'},
                        status=status.HTTP_400_BAD_REQUEST)

    with connection.cursor() as cursor:
        if name and email:
            cursor.execute(
                "UPDATE Users SET name = %s, email = %s WHERE user_id = %s",
                [name, email, custom_user_id]
            )
        elif name:
            cursor.execute(
                "UPDATE Users SET name = %s WHERE user_id = %s",
                [name, custom_user_id]
            )
        elif email:
            cursor.execute(
                "UPDATE Users SET email = %s WHERE user_id = %s",
                [email, custom_user_id]
            )

    return Response({'message': 'Profile updated successfully'})


# ─── 15. إرسال إشعار تذكير ──────────────────────────
@api_view(['POST'])
@permission_classes([IsAuthenticated])
def send_reminder(request):
    from .firebase import send_notification

    custom_user_id = int(request.user.username.split('_')[1])

    # Get user's device token
    try:
        device = DeviceToken.objects.get(user_id=custom_user_id)
    except DeviceToken.DoesNotExist:
        return Response({'error': 'No device token found'},
                        status=status.HTTP_404_NOT_FOUND)

    medicine_name = request.data.get('medicine_name', 'your medicine')
    time = request.data.get('time', '')

    success = send_notification(
        device_token=device.token,
        title="💊 Medicine Reminder",
        body=f"Time to take {medicine_name}! {time}"
    )

    if success:
        return Response({'message': 'Notification sent successfully'})
    else:
        return Response({'error': 'Failed to send notification'},
                        status=status.HTTP_500_INTERNAL_SERVER_ERROR)