package com.medicineapp.data.network



import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // ⚠️ Change this to your machine's IP when running on a real device
    // Use 10.0.2.2 for emulator, or your LAN IP for physical device
    const val BASE_URL = "http://192.168.1.46:8000/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// ─── Session Manager (SharedPreferences) ─────────────
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("MedicineAppPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN   = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME    = "user_name"
        private const val KEY_EMAIL   = "user_email"
    }

    fun saveSession(token: String, userId: Int, name: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_NAME, name)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    /** Returns "Bearer <token>" ready for Retrofit headers */
    fun getBearerToken(): String = "Bearer ${getToken()}"

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun getName(): String? = prefs.getString(KEY_NAME, null)

    fun saveEmail(email: String) = prefs.edit().putString(KEY_EMAIL, email).apply()
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clearSession() = prefs.edit().clear().apply()
}