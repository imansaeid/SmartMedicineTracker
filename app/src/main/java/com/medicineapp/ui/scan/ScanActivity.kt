package com.medicineapp.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.medicineapp.R
import com.medicineapp.data.network.RetrofitClient
import com.medicineapp.data.network.SessionManager
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var session: SessionManager
    private var isProcessing = false

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        session = SessionManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE
            )
        }

        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun startCamera() {
        val previewView = findViewById<PreviewView>(R.id.previewView)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processOCR(imageProxy) // ← غيرنا من Barcode لـ OCR
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Camera failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processOCR(imageProxy: ImageProxy) {
        if (isProcessing) { imageProxy.close(); return }

        val mediaImage = imageProxy.image
        if (mediaImage == null) { imageProxy.close(); return }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        val recognizer = TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )

        recognizer.process(image)
            .addOnSuccessListener { result ->

                // لو ما في نص = إضاءة سيئة
                if (result.text.isEmpty()) {
                    imageProxy.close()
                    return@addOnSuccessListener
                }

                // اخذي أكبر block = اسم الدواء
                val biggestBlock = result.textBlocks
                    .maxByOrNull { it.boundingBox?.width() ?: 0 }

                val rawName = biggestBlock?.text?.split(" ")?.get(0) ?: ""

                // نظفي الحروف التركية
                val cleanName = rawName
                    .replace("İ", "I").replace("ı", "i")
                    .replace("Ş", "S").replace("ş", "s")
                    .replace("Ğ", "G").replace("ğ", "g")
                    .replace("Ü", "U").replace("ü", "u")
                    .replace("Ö", "O").replace("ö", "o")
                    .replace("Ç", "C").replace("ç", "c")
                    .filter { it.isLetter() }

                if (cleanName.isNotEmpty() && !isProcessing) {
                    isProcessing = true
                    searchMedicineByName(cleanName)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun searchMedicineByName(name: String) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.searchMedicine(
                    session.getBearerToken(), name
                )
                val results = resp.body()

                if (resp.isSuccessful && !results.isNullOrEmpty()) {
                    val med = results[0]
                    runOnUiThread {
                        AlertDialog.Builder(this@ScanActivity)
                            .setTitle("💊 Medicine Found!")
                            .setMessage(
                                "Name: ${med.name}\n" +
                                        "Form: ${med.form ?: "N/A"}\n" +
                                        "Strength: ${med.strength ?: "N/A"}\n" +
                                        "Active Ingredient: ${med.activeIngredient ?: "N/A"}\n\n" +
                                        "${med.description ?: ""}"
                            )
                            .setPositiveButton("Go to My Medicines") { _, _ ->
                                finish()
                            }
                            .setNegativeButton("Scan Again") { _, _ ->
                                isProcessing = false
                            }
                            .setCancelable(false)
                            .show()
                    }
                } else {
                    runOnUiThread {
                        AlertDialog.Builder(this@ScanActivity)
                            .setTitle("❌ Not Found")
                            .setMessage(
                                "No medicine found for: $name\n\n" +
                                        "This medicine is not in our database yet.\n" +
                                        "It will be added soon!"
                            )
                            .setPositiveButton("Scan Again") { _, _ ->
                                isProcessing = false
                            }
                            .show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@ScanActivity,
                        "Search error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    isProcessing = false
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(
                this,
                "Camera permission required for scanning",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}