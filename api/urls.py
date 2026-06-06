from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView
from . import views

urlpatterns = [
    # ── Auth ──────────────────────────────
    path('register/', views.register, name='register'),
    path('login/', views.login_user, name='login'),

    # ── Medicines ─────────────────────────
    path('medicines/search/', views.search_medicine, name='search_medicine'),
    path('medicines/add/', views.add_medicine, name='add_medicine'),
    path('medicines/', views.user_medicines, name='user_medicines'),
    path('medicines/<int:medicine_id>/delete/', views.delete_medicine, name='delete_medicine'),

    # ── Interactions ──────────────────────
    path('interactions/check/', views.check_interaction, name='check_interaction'),
    path('interactions/count/', views.interaction_count, name='interaction_count'),

    # ── Schedules ─────────────────────────
    path('schedules/', views.get_schedules, name='get_schedules'),
    path('schedules/add/', views.add_schedule, name='add_schedule'),

    # ── Firebase ──────────────────────────
    path('device-token/', views.save_device_token, name='save_device_token'),
    
    # ── New APIs ──────────────────────────
    path('medicines/<int:medicine_id>/update/', views.update_medicine),
    path('doses/mark/', views.mark_dose),
    path('doses/history/', views.dose_history),
    path('profile/update/', views.update_profile),
    path('send-reminder/', views.send_reminder),
]