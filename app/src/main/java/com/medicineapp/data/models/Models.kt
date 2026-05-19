package com.medicineapp.data.models



import com.google.gson.annotations.SerializedName

// ─── Auth ────────────────────────────────────────────
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String?,
    val user_id: Int?,
    val username: String?,
    val name: String?,
    val message: String?,
    val error: String?
)

// ─── Medicine Catalog ────────────────────────────────
data class Medicine(
    @SerializedName("catalog_id") val catalogId: Int,
    val name: String,
    @SerializedName("active_ingredient") val activeIngredient: String?,
    val strength: String?,
    val form: String?,
    val description: String?,
    @SerializedName("conflicts_with") val conflictsWith: String?,
    @SerializedName("interaction_severity") val interactionSeverity: String?,
    @SerializedName("interaction_description") val interactionDescription: String?
)

// ─── User Medicine ───────────────────────────────────
data class AddMedicineRequest(
    val catalog: Int,
    val user_id: Int,
    val dosage: String?,
    val instructions: String?,
    val start_date: String,
    val end_date: String?,
    val duration_type: String?
)

data class UserMedicine(
    @SerializedName("medicine_id") val medicineId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("catalog") val catalogId: Int,
    val dosage: String?,
    val instructions: String?,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("duration_type") val durationType: String?,
    // read-only fields from serializer
    @SerializedName("medicine_name") val medicineName: String?,
    @SerializedName("medicine_form") val medicineForm: String?,
    @SerializedName("medicine_strength") val medicineStrength: String?
)

data class AddMedicineResponse(
    val allowed: Boolean?,
    val message: String?,
    val error: String?,
    val warning: Boolean?,
    val severity: String?,      // "Low", "Medium", "High"
    val description: String?,
    val data: UserMedicine?
)

// ─── Interaction ─────────────────────────────────────
data class InteractionResponse(
    @SerializedName("has_conflict") val hasConflict: Boolean,
    val severity: String?,
    val description: String?
)

// ─── Schedule ────────────────────────────────────────
data class AddScheduleRequest(
    val medicine: Int,
    @SerializedName("time_of_day") val timeOfDay: String,
    @SerializedName("repeat_type") val repeatType: String?,
    @SerializedName("days_of_week") val daysOfWeek: String?,
    val enabled: Boolean = true
)

data class MedicineSchedule(
    @SerializedName("schedule_id") val scheduleId: Int,
    val medicine: Int,
    @SerializedName("time_of_day") val timeOfDay: String,
    @SerializedName("repeat_type") val repeatType: String?,
    @SerializedName("days_of_week") val daysOfWeek: String?,
    val enabled: Boolean
)

// ─── Dose Log ────────────────────────────────────────
data class MarkDoseRequest(
    val medicine_id: Int,
    val scheduled_time: String,
    val taken: Boolean = true
)

data class DoseLog(
    @SerializedName("log_id") val logId: Int,
    @SerializedName("user_id") val userId: Int,
    val medicine: Int,
    @SerializedName("scheduled_time") val scheduledTime: String,
    val taken: Boolean,
    @SerializedName("taken_at") val takenAt: String?
)

// ─── Device Token ────────────────────────────────────
data class DeviceTokenRequest(
    val token: String,
    val device_type: String = "android"
)

data class GenericResponse(
    val message: String?,
    val error: String?
)