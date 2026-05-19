package com.medicineapp.ui.splash



import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.medicineapp.R
import com.medicineapp.data.network.SessionManager
import com.medicineapp.ui.auth.LoginActivity
import com.medicineapp.ui.home.HomeActivity
import com.medicineapp.ui.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val session = SessionManager(this)
        val prefs = getSharedPreferences("MedicineAppPrefs", MODE_PRIVATE)
        val onboardingSeen = prefs.getBoolean("onboarding_seen", false)

        Handler(Looper.getMainLooper()).postDelayed({
            when {
                session.isLoggedIn() -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                }
                !onboardingSeen -> {
                    startActivity(Intent(this, OnboardingActivity::class.java))
                }
                else -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            }
            finish()
        }, 2000)
    }
}