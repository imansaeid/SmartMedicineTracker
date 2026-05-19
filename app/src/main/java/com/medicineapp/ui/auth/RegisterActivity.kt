package com.medicineapp.ui.auth



import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medicineapp.R
import com.medicineapp.data.models.RegisterRequest
import com.medicineapp.data.network.RetrofitClient
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName      = findViewById<EditText>(R.id.etName)
        val etEmail     = findViewById<EditText>(R.id.etEmail)
        val etPassword  = findViewById<EditText>(R.id.etPassword)
        val etConfirm   = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin     = findViewById<TextView>(R.id.tvGoToLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnRegister.setOnClickListener {
            val name     = etName.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirm  = etConfirm.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showToast("Please fill all fields")
                return@setOnClickListener
            }
            if (password != confirm) {
                showToast("Passwords do not match")
                return@setOnClickListener
            }
            if (password.length < 6) {
                showToast("Password must be at least 6 characters")
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.register(
                        RegisterRequest(name, email, password)
                    )
                    if (response.isSuccessful) {
                        showToast("Account created! Please login.")
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        val error = response.errorBody()?.string()
                        showToast(if (error?.contains("Email") == true)
                            "Email already registered" else "Registration failed")
                    }
                } catch (e: Exception) {
                    showToast("Connection error: ${e.message}")
                } finally {
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                }
            }
        }

        tvLogin.setOnClickListener { finish() }
    }

    private fun showToast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}