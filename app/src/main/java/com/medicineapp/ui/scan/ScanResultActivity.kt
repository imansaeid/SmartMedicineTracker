package com.medicineapp.ui.scan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.medicineapp.R
import com.medicineapp.ui.home.HomeActivity

class ScanResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_result)

        val name        = intent.getStringExtra("med_name")        ?: "Unknown"
        val form        = intent.getStringExtra("med_form")        ?: "N/A"
        val strength    = intent.getStringExtra("med_strength")    ?: "N/A"
        val ingredient  = intent.getStringExtra("med_ingredient")  ?: "N/A"
        val description = intent.getStringExtra("med_description") ?: ""

        findViewById<TextView>(R.id.tvMedName).text        = name
        findViewById<TextView>(R.id.tvMedForm).text        = "💊 Form: $form"
        findViewById<TextView>(R.id.tvMedStrength).text    = "⚖️ Strength: $strength"
        findViewById<TextView>(R.id.tvMedIngredient).text  = "🔬 Active Ingredient: $ingredient"
        findViewById<TextView>(R.id.tvMedDescription).text = description

        // زر "Scan Again" — يرجع لشاشة السكان
        findViewById<Button>(R.id.btnScanAgain).setOnClickListener {
            finish()  // يغلق صفحة النتيجة ويرجع للـ ScanActivity
        }

        // زر "Go to My Medicines" — يروح للهوم
        findViewById<Button>(R.id.btnGoToMedicines).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("navigate_to", "medicines")
            }
            startActivity(intent)
            finish()
        }
    }
}
