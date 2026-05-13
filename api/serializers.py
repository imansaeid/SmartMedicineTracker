from rest_framework import serializers
from django.contrib.auth.models import User
from .models import Medicine, UserMedicine, MedicineSchedule, DeviceToken, DoseLog


# تسجيل مستخدم جديد
class RegisterSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True)

    class Meta:
        model = User
        fields = ['username', 'email', 'password']

    def create(self, validated_data):
        user = User.objects.create_user(
            username=validated_data['username'],
            email=validated_data['email'],
            password=validated_data['password']
        )
        return user


# معلومات المستخدم
class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ['id', 'username', 'email']


# الأدوية من الكاتالوج
class MedicineSerializer(serializers.ModelSerializer):
    class Meta:
        model = Medicine
        fields = '__all__'


# أدوية المستخدم
class UserMedicineSerializer(serializers.ModelSerializer):
    medicine_name = serializers.CharField(
        source='catalog.name', read_only=True
    )
    medicine_form = serializers.CharField(
        source='catalog.form', read_only=True
    )
    medicine_strength = serializers.CharField(
        source='catalog.strength', read_only=True
    )

    class Meta:
        model = UserMedicine
        fields = '__all__'


# مواعيد الأدوية
class MedicineScheduleSerializer(serializers.ModelSerializer):
    class Meta:
        model = MedicineSchedule
        fields = '__all__'


# سجل الجرعات
class DoseLogSerializer(serializers.ModelSerializer):
    class Meta:
        model = DoseLog
        fields = '__all__'


# Device Token للـ Firebase
class DeviceTokenSerializer(serializers.ModelSerializer):
    class Meta:
        model = DeviceToken
        fields = '__all__'