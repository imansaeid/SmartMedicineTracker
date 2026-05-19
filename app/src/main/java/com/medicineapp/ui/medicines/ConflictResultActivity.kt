package com.medicineapp.ui.medicines


import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.medicineapp.R

class ConflictResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conflict_result)

        val severity    = intent.getStringExtra("severity") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val med1        = intent.getStringExtra("med1") ?: "Medicine 1"
        val med2        = intent.getStringExtra("med2") ?: "Medicine 2"

        val tvRiskEmoji    = findViewById<TextView>(R.id.tvRiskEmoji)
        val tvRiskLevel    = findViewById<TextView>(R.id.tvRiskLevel)
        val tvRiskSubtitle = findViewById<TextView>(R.id.tvRiskSubtitle)
        val tvMed1         = findViewById<TextView>(R.id.tvMed1)
        val tvMed2         = findViewById<TextView>(R.id.tvMed2)
        val tvDescription  = findViewById<TextView>(R.id.tvDescription)
        val btnAddAnyway   = findViewById<Button>(R.id.btnAddAnyway)
        val btnCancel      = findViewById<Button>(R.id.btnCancel)
        val btnBack        = findViewById<View>(R.id.btnBack)

        tvMed1.text        = med1
        tvMed2.text        = med2
        tvDescription.text = description

        when (severity) {
            "High" -> {
                tvRiskEmoji.text    = "🔴"
                tvRiskLevel.text    = "HIGH RISK"
                tvRiskLevel.setTextColor(getColor(R.color.high_text))
                tvRiskSubtitle.text = "This medicine CANNOT be added!"
                btnAddAnyway.visibility = View.GONE
            }
            "Medium" -> {
                tvRiskEmoji.text    = "🟠"
                tvRiskLevel.text    = "Medium Risk"
                tvRiskLevel.setTextColor(getColor(R.color.medium_text))
                tvRiskSubtitle.text = "Added with warning"
                btnAddAnyway.visibility = View.VISIBLE
            }
            "Low" -> {
                tvRiskEmoji.text    = "🟡"
                tvRiskLevel.text    = "Low Risk"
                tvRiskLevel.setTextColor(getColor(R.color.low_text))
                tvRiskSubtitle.text = "Minor interaction detected"
                btnAddAnyway.visibility = View.VISIBLE
            }
        }

        btnBack.setOnClickListener { finish() }
        btnCancel.setOnClickListener { finish() }
        btnAddAnyway.setOnClickListener { finish() }
    }
}