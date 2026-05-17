from apscheduler.schedulers.background import BackgroundScheduler
from django.utils import timezone
import datetime

def send_medicine_reminders():
    from .models import MedicineSchedule, DeviceToken
    from .firebase import send_notification

    now = timezone.localtime(timezone.now())
    current_time = now.strftime("%H:%M")
    current_day = now.strftime("%a")  # Mon, Tue, Wed...

    print(f"[Scheduler] Checking reminders at {current_time} on {current_day}")

    # Get all enabled schedules
    schedules = MedicineSchedule.objects.filter(enabled=True).select_related('medicine')

    for schedule in schedules:
        # Check if time matches
        schedule_time = schedule.time_of_day.strftime("%H:%M")
        if schedule_time != current_time:
            continue

        # Check if day matches
        if schedule.days_of_week:
            days = [d.strip() for d in schedule.days_of_week.split(',')]
            if current_day not in days:
                continue

        # Get user's device token
        user_id = schedule.medicine.user_id
        try:
            device = DeviceToken.objects.get(user_id=user_id)
        except DeviceToken.DoesNotExist:
            print(f"[Scheduler] No device token for user {user_id}")
            continue

        # Get medicine name
        medicine_name = schedule.medicine.catalog.name

        # Send notification
        send_notification(
            device_token=device.token,
            title="💊 Medicine Reminder",
            body=f"Time to take {medicine_name}!"
        )
        print(f"[Scheduler] Sent reminder for {medicine_name} to user {user_id}")


def start():
    import os
    if os.environ.get('RUN_MAIN') != 'true':
        return

    scheduler = BackgroundScheduler()
    scheduler.add_job(
        send_medicine_reminders,
        'interval',
        minutes=1,
        id='medicine_reminders',
        replace_existing=True
    )
    scheduler.start()
    print("[Scheduler] Medicine reminder scheduler started!")