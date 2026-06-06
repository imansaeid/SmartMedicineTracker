package com.medicineapp.data.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    const val DEFAULT_BASE_URL = "http://192.168.1.45:8000/"
    private const val PREFS_NAME = "MedicineAppPrefs"
    private const val KEY_SERVER_URL = "server_url"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // للاستخدام القديم — يستخدم الـ URL الافتراضي
    val api: ApiService by lazy {
        buildRetrofit(DEFAULT_BASE_URL).create(ApiService::class.java)
    }

    // الـ API الديناميكي — يقرأ الـ URL المحفوظ من SharedPreferences
    fun getApi(context: Context): ApiService {
        val url = getSavedUrl(context)
        return buildRetrofit(url).create(ApiService::class.java)
    }

    fun getSavedUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SERVER_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun saveUrl(context: Context, url: String) {
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER_URL, cleanUrl)
            .apply()
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

// ─── Session Manager ──────────────────────────────────────────
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
    fun getBearerToken(): String = "Bearer ${getToken()}"
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getName(): String? = prefs.getString(KEY_NAME, null)
    fun saveEmail(email: String) = prefs.edit().putString(KEY_EMAIL, email).apply()
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun isLoggedIn(): Boolean = getToken() != null
    fun clearSession() = prefs.edit().clear().apply()
}
