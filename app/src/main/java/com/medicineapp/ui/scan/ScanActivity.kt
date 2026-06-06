package com.medicineapp.ui.scan

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.medicineapp.R
import com.medicineapp.data.network.RetrofitClient
import com.medicineapp.data.network.SessionManager
import kotlinx.coroutines.launch

class ScanActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    // فتح كاميرا النظام تماماً مثل Add Medicine
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                analyzePhoto(bitmap)
            } else {
                resetUI("❌ No photo captured. Try again.")
            }
        } else {
            resetUI("📷 Press the button to take a photo")
        }
    }

    companion object {

        private fun normalizeText(text: String): String = text
            .replace("İ", "I").replace("ı", "i")
            .replace("Ş", "S").replace("ş", "s")
            .replace("Ğ", "G").replace("ğ", "g")
            .replace("Ü", "U").replace("ü", "u")
            .replace("Ö", "O").replace("ö", "o")
            .replace("Ç", "C").replace("ç", "c")

        private fun isDosageWord(word: String): Boolean {
            val dosage = Regex(
                """^\d+[\.,]?\d*\s*(mg|ml|mcg|g|iu|%|tablet|kapsul|film|coated)?$""",
                RegexOption.IGNORE_CASE
            )
            val attached = Regex(""".*\d+[\.,]?\d*(mg|ml|mcg|g|iu|%).*""", RegexOption.IGNORE_CASE)
            return dosage.matches(word) || attached.matches(word) || word.matches(Regex("\\d+"))
        }

        /**
         * يرجع قائمة من المرشحين مرتبة حسب حجم الـ text block (الأكبر أولاً)
         * لأن اسم الدواء عادةً يكون بأكبر خط على العلبة
         */
        fun extractCandidates(ocrResult: Text): List<String> {
            val seen       = mutableSetOf<String>()
            val candidates = mutableListOf<String>()

            // رتب الـ blocks حسب المساحة (الأكبر = الأهم)
            val sortedBlocks = ocrResult.textBlocks.sortedByDescending {
                it.boundingBox?.let { bb -> bb.width() * bb.height() } ?: 0
            }

            for (block in sortedBlocks) {
                normalizeText(block.text)
                    .split(Regex("[\\s\\n/|\\\\]+"))
                    .map { it.replace(Regex("[^a-zA-Z0-9]"), "").trim() }
                    .filter { w -> w.length >= 3 && w.any { it.isLetter() } && !isDosageWord(w) }
                    .forEach { c ->
                        if (c.uppercase() !in seen) {
                            seen.add(c.uppercase())
                            candidates.add(c)
                        }
                    }
            }
            return candidates
        }

        // للاستخدام في MedicinesFragment (backward compat)
        fun extractMedicineName(rawText: String): String {
            return normalizeText(rawText)
                .split(Regex("[\\s/|\\\\]+"))
                .map { it.replace(Regex("[^a-zA-Z0-9]"), "").trim() }
                .filter { w -> w.length >= 3 && w.any { it.isLetter() } && !isDosageWord(w) }
                .firstOrNull() ?: ""
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        session = SessionManager(this)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnCapture).setOnClickListener { openCamera() }
    }

    private fun openCamera() {
        cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    }

    private fun analyzePhoto(bitmap: Bitmap) {
        val btnCapture   = findViewById<Button>(R.id.btnCapture)
        val progressScan = findViewById<ProgressBar>(R.id.progressScan)
        val tvStatus     = findViewById<TextView>(R.id.tvScanStatus)

        btnCapture.isEnabled    = false
        progressScan.visibility = View.VISIBLE
        tvStatus.text           = "⏳ Reading medicine name..."

        // Scale up for better OCR
        val scaledBitmap = if (bitmap.width < 640) {
            val scale = 640f / bitmap.width
            Bitmap.createScaledBitmap(bitmap, 640, (bitmap.height * scale).toInt(), true)
        } else bitmap

        val image      = InputImage.fromBitmap(scaledBitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { result ->
                val candidates = extractCandidates(result)
                if (candidates.isEmpty()) {
                    tvStatus.text = "❌ Could not read text. Try again."
                    resetUI(null)
                } else {
                    // جرب كل مرشح حتى تلاقي دواء في الـ API
                    searchByCandidates(candidates)
                }
            }
            .addOnFailureListener {
                tvStatus.text = "❌ OCR failed. Try again."
                resetUI(null)
            }
    }

    private fun searchByCandidates(candidates: List<String>) {
        val tvStatus = findViewById<TextView>(R.id.tvScanStatus)

        lifecycleScope.launch {
            for (name in candidates) {
                try {
                    runOnUiThread { tvStatus.text = "🔍 Trying: $name..." }

                    val resp    = RetrofitClient.getApi(this@ScanActivity)
                        .searchMedicine(session.getBearerToken(), name)
                    val results = resp.body()

                    if (resp.isSuccessful && !results.isNullOrEmpty()) {
                        val med    = results[0]
                        val intent = Intent(this@ScanActivity, ScanResultActivity::class.java).apply {
                            putExtra("med_name",        med.name ?: name)
                            putExtra("med_form",        med.form ?: "N/A")
                            putExtra("med_strength",    med.strength ?: "N/A")
                            putExtra("med_ingredient",  med.activeIngredient ?: "N/A")
                            putExtra("med_description", med.description ?: "")
                        }
                        startActivity(intent)
                        finish()
                        return@launch   // وجدنا — وقف
                    }
                } catch (e: Exception) {
                    // جرب المرشح التالي
                }
            }

            // كل المرشحين جربناهم وما لقينا شي
            runOnUiThread {
                tvStatus.text = "❌ Medicine not found. Try typing manually."
                resetUI(null)
            }
        }
    }

    private fun resetUI(statusText: String?) {
        val btnCapture   = findViewById<Button>(R.id.btnCapture)
        val progressScan = findViewById<ProgressBar>(R.id.progressScan)
        val tvStatus     = findViewById<TextView>(R.id.tvScanStatus)

        btnCapture.isEnabled    = true
        progressScan.visibility = View.GONE
        if (statusText != null) tvStatus.text = statusText
    }
}
