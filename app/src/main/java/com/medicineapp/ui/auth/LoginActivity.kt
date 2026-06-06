package com.medicineapp.ui.auth



import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medicineapp.R
import com.medicineapp.data.models.LoginRequest
import com.medicineapp.data.network.RetrofitClient
import com.medicineapp.data.network.SessionManager
import com.medicineapp.ui.home.HomeActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)

        // Skip login if already logged in
        if (session.isLoggedIn()) {
            goHome()
            return
        }

        setContentView(R.layout.activity_login)

        val etEmail           = findViewById<EditText>(R.id.etEmail)
        val etPassword        = findViewById<EditText>(R.id.etPassword)
        val btnLogin          = findViewById<Button>(R.id.btnLogin)
        val tvRegister        = findViewById<TextView>(R.id.tvGoToRegister)
        val progressBar       = findViewById<ProgressBar>(R.id.progressBar)
        val tvTogglePassword  = findViewById<TextView>(R.id.tvTogglePassword)

        // ── زر العين — إظهار/إخفاء كلمة المرور ──
        var isPasswordVisible = false
        tvTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                // أظهر النص
                etPassword.inputType =
                    android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                tvTogglePassword.text = "🙈"
            } else {
                // أخفيه مجدداً
                etPassword.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                tvTogglePassword.text = "👁"
            }
            // أبقِ المؤشر في نهاية النص
            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Please fill all fields")
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.login(LoginRequest(email, password))
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.token != null && body.user_id != null) {
                            session.saveSession(body.token, body.user_id, body.name ?: "User")
                            session.saveEmail(email)
                            goHome()
                        } else {
                            showToast(body?.error ?: "Login failed")
                        }
                    } else {
                        showToast("Invalid email or password")
                    }
                } catch (e: Exception) {
                    showToast("Connection error: ${e.message}")
                } finally {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                }
            }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}