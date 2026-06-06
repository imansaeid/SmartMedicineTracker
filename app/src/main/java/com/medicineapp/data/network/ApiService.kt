package com.medicineapp.data.network



import com.medicineapp.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────
    @POST("api/register/")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/login/")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    // ── Medicine Search ───────────────────────────────
    @GET("api/medicines/search/")
    suspend fun searchMedicine(
        @Header("Authorization") token: String,
        @Query("q") query: String
    ): Response<List<Medicine>>

    // ── User Medicines ────────────────────────────────
    @GET("api/medicines/")
    suspend fun getUserMedicines(
        @Header("Authorization") token: String
    ): Response<List<UserMedicine>>

    @POST("api/medicines/add/")
    suspend fun addMedicine(
        @Header("Authorization") token: String,
        @Body body: AddMedicineRequest
    ): Response<AddMedicineResponse>

    @DELETE("api/medicines/{id}/delete/")
    suspend fun deleteMedicine(
        @Header("Authorization") token: String,
        @Path("id") medicineId: Int
    ): Response<GenericResponse>

    // ── Interaction ───────────────────────────────────
    @GET("api/interactions/count/")
    suspend fun getInteractionCount(
        @Header("Authorization") token: String
    ): Response<Map<String, Int>>

    @GET("api/interactions/check/")
    suspend fun checkInteraction(
        @Header("Authorization") token: String,
        @Query("drug1") drug1: String,
        @Query("drug2") drug2: String
    ): Response<InteractionResponse>

    // ── Schedules ─────────────────────────────────────
    @GET("api/schedules/")
    suspend fun getSchedules(
        @Header("Authorization") token: String
    ): Response<List<MedicineSchedule>>

    @POST("api/schedules/add/")
    suspend fun addSchedule(
        @Header("Authorization") token: String,
        @Body body: AddScheduleRequest
    ): Response<MedicineSchedule>

    // ── Doses ─────────────────────────────────────────
    @POST("api/doses/mark/")
    suspend fun markDose(
        @Header("Authorization") token: String,
        @Body body: MarkDoseRequest
    ): Response<GenericResponse>

    @GET("api/doses/history/")
    suspend fun getDoseHistory(
        @Header("Authorization") token: String
    ): Response<List<DoseLog>>

    // ── Profile ───────────────────────────────────────
    @PUT("api/profile/update/")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<GenericResponse>

    // ── Firebase ──────────────────────────────────────
    @POST("api/device-token/")
    suspend fun saveDeviceToken(
        @Header("Authorization") token: String,
        @Body body: DeviceTokenRequest
    ): Response<GenericResponse>
}