from django.db import models


# جدول Medicine_Catalog — الـ 213 دواء
class Medicine(models.Model):
    catalog_id = models.AutoField(primary_key=True)
    name = models.CharField(max_length=150)
    active_ingredient = models.CharField(max_length=200)
    strength = models.CharField(max_length=50, null=True)
    form = models.CharField(max_length=50, null=True)
    description = models.TextField(null=True)
    conflicts_with = models.TextField(null=True)
    interaction_severity = models.CharField(max_length=20, null=True)
    interaction_description = models.TextField(null=True)

    class Meta:
        db_table = 'Medicine_Catalog'

    def __str__(self):
        return self.name


# جدول Medicines — أدوية كل مستخدم
class UserMedicine(models.Model):
    medicine_id = models.AutoField(primary_key=True)
    user_id = models.IntegerField()
    catalog = models.ForeignKey(Medicine, on_delete=models.CASCADE)
    dosage = models.CharField(max_length=50, null=True)
    instructions = models.CharField(max_length=150, null=True)
    start_date = models.DateField()
    end_date = models.DateField(null=True)
    duration_type = models.CharField(max_length=20, null=True)

    class Meta:
        db_table = 'Medicines'


# جدول Medicine_Schedule — مواعيد الأدوية
class MedicineSchedule(models.Model):
    schedule_id = models.AutoField(primary_key=True)
    medicine = models.ForeignKey(UserMedicine, on_delete=models.CASCADE)
    time_of_day = models.TimeField()
    repeat_type = models.CharField(max_length=20, null=True)
    days_of_week = models.CharField(max_length=50, null=True)
    enabled = models.BooleanField(default=True)

    class Meta:
        db_table = 'Medicine_Schedule'


# جدول Device_Tokens — للـ Firebase
class DeviceToken(models.Model):
    token_id = models.AutoField(primary_key=True)
    user_id = models.IntegerField()
    token = models.CharField(max_length=255)
    device_type = models.CharField(max_length=20, null=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'Device_Tokens'


# جدول Dose_Logs — سجل الجرعات
class DoseLog(models.Model):
    log_id = models.AutoField(primary_key=True)
    user_id = models.IntegerField()
    medicine = models.ForeignKey(UserMedicine, on_delete=models.CASCADE)
    scheduled_time = models.DateTimeField()
    taken = models.BooleanField(default=False)
    taken_at = models.DateTimeField(null=True)

    class Meta:
        db_table = 'Dose_Logs'