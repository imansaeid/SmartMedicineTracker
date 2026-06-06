package com.medicineapp.ui.medicines

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.common.InputImage
import com.medicineapp.notifications.NotificationHelper
import com.medicineapp.ui.scan.ScanActivity
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.medicineapp.R
import com.medicineapp.data.models.AddMedicineRequest
import com.medicineapp.data.models.AddScheduleRequest
import com.medicineapp.data.models.Medicine
import com.medicineapp.data.models.UserMedicine
import com.medicineapp.data.network.RetrofitClient
import com.medicineapp.data.network.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MedicinesFragment : Fragment() {

    private lateinit var session: SessionManager
    private val userMedicines = mutableListOf<UserMedicine>()
    private lateinit var adapter: UserMedicineAdapter
    private val selectedTimes = mutableListOf<String>()
    private val timingInstructions = mutableListOf<String>()

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            currentPhotoCallback?.invoke(bitmap)
        }
    }

    private var currentPhotoCallback: ((Bitmap?) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_medicines, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        createNotificationChannel()

        // طلب إذن الإشعارات على Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    200
                )
            }
        }

        val rvMedicines = view.findViewById<RecyclerView>(R.id.rvMedicines)
        val btnAdd      = view.findViewById<Button>(R.id.btnAddMedicine)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        adapter = UserMedicineAdapter(userMedicines) { med -> confirmDelete(med) }
        rvMedicines.layoutManager = LinearLayoutManager(requireContext())
        rvMedicines.adapter = adapter

        btnAdd.setOnClickListener { showAddMedicineDialog() }
        loadMedicines(progressBar)
    }

    private fun loadMedicines(progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.getApi(requireContext()).getUserMedicines(session.getBearerToken())
                if (resp.isSuccessful) {
                    userMedicines.clear()
                    userMedicines.addAll(resp.body() ?: emptyList())
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) { toast("Connection error") }
            finally { progressBar.visibility = View.GONE }
        }
    }

    private fun showAddMedicineDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_medicine, null)

        val btnTabType        = dialogView.findViewById<Button>(R.id.btnTabType)
        val btnTabPhoto       = dialogView.findViewById<Button>(R.id.btnTabPhoto)
        val layoutType        = dialogView.findViewById<LinearLayout>(R.id.layoutTypeMethod)
        val layoutPhoto       = dialogView.findViewById<LinearLayout>(R.id.layoutPhotoMethod)
        val etSearch          = dialogView.findViewById<EditText>(R.id.etSearchMedicine)
        val btnSearch         = dialogView.findViewById<Button>(R.id.btnSearch)
        val btnTakePhoto      = dialogView.findViewById<Button>(R.id.btnTakePhoto)
        val ivCaptured        = dialogView.findViewById<ImageView>(R.id.ivCapturedPhoto)
        val layoutPlaceholder = dialogView.findViewById<LinearLayout>(R.id.layoutPhotoPlaceholder)
        val layoutSelectedMed = dialogView.findViewById<LinearLayout>(R.id.layoutSelectedMed)
        val tvSelectedMedName = dialogView.findViewById<TextView>(R.id.tvSelectedMedName)
        val rbPermanent       = dialogView.findViewById<RadioButton>(R.id.rbPermanent)
        val rbTemporary       = dialogView.findViewById<RadioButton>(R.id.rbTemporary)
        val layoutDuration    = dialogView.findViewById<LinearLayout>(R.id.layoutDuration)
        val btnDur7           = dialogView.findViewById<Button>(R.id.btnDur7)
        val btnDur14          = dialogView.findViewById<Button>(R.id.btnDur14)
        val btnDur30          = dialogView.findViewById<Button>(R.id.btnDur30)
        val etCustomDuration  = dialogView.findViewById<EditText>(R.id.etCustomDuration)
        val etDosage          = dialogView.findViewById<EditText>(R.id.etDosage)
        val btnFreq1          = dialogView.findViewById<Button>(R.id.btnFreq1)
        val btnFreq2          = dialogView.findViewById<Button>(R.id.btnFreq2)
        val btnFreq3          = dialogView.findViewById<Button>(R.id.btnFreq3)
        val btnFreq4          = dialogView.findViewById<Button>(R.id.btnFreq4)
        val layoutTimePickers = dialogView.findViewById<LinearLayout>(R.id.layoutTimePickers)
        val btnConfirm        = dialogView.findViewById<Button>(R.id.btnConfirmAdd)
        val progressBar       = dialogView.findViewById<ProgressBar>(R.id.progressSearchBar)

        var selectedMedicine: Medicine? = null
        var selectedFrequency = 1
        var selectedDuration  = 7
        selectedTimes.clear()
        timingInstructions.clear()

        // ── Tabs ──
        btnTabType.setOnClickListener {
            btnTabType.background  = resources.getDrawable(R.drawable.bg_btn_primary, null)
            btnTabType.setTextColor(0xFFFFFFFF.toInt())
            btnTabPhoto.background = resources.getDrawable(R.drawable.bg_btn_outline_primary, null)
            btnTabPhoto.setTextColor(0xFF6366F1.toInt())
            layoutType.visibility  = View.VISIBLE
            layoutPhoto.visibility = View.GONE
        }
        btnTabPhoto.setOnClickListener {
            btnTabPhoto.background = resources.getDrawable(R.drawable.bg_btn_primary, null)
            btnTabPhoto.setTextColor(0xFFFFFFFF.toInt())
            btnTabType.background  = resources.getDrawable(R.drawable.bg_btn_outline_primary, null)
            btnTabType.setTextColor(0xFF6366F1.toInt())
            layoutType.visibility  = View.GONE
            layoutPhoto.visibility = View.VISIBLE
        }

        // ── Camera + OCR ──
        btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(Manifest.permission.CAMERA), 100
                )
                return@setOnClickListener
            }

            currentPhotoCallback = { bitmap ->
                if (bitmap != null) {
                    // ✅ NO quality check — accept any bitmap from camera
                    ivCaptured.setImageBitmap(bitmap)
                    ivCaptured.visibility = View.VISIBLE
                    layoutPlaceholder.visibility = View.GONE
                    toast("🔍 Reading medicine name...")

                    // Scale up if needed for better OCR
                    val scaledBitmap = if (bitmap.width < 640) {
                        val scale = 640f / bitmap.width
                        Bitmap.createScaledBitmap(
                            bitmap,
                            640,
                            (bitmap.height * scale).toInt(),
                            true
                        )
                    } else bitmap

                    val image = InputImage.fromBitmap(scaledBitmap, 0)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                    recognizer.process(image)
                        .addOnSuccessListener { result ->
                            if (result.text.isEmpty()) {
                                toast("⚠️ Could not read text. Please type manually.")
                                btnTabType.performClick()
                            } else {
                                // اجمع كل النص واستخرج اسم الدواء بشكل صحيح
                                val allText = result.textBlocks.joinToString(" ") { it.text }
                                val cleanName = ScanActivity.extractMedicineName(allText)

                                Log.d("OCR_RESULT", "Clean Name: $cleanName")

                                if (cleanName.isEmpty()) {
                                    toast("⚠️ Could not read name. Please type manually.")
                                    btnTabType.performClick()
                                } else {
                                    lifecycleScope.launch {
                                        try {
                                            progressBar.visibility = View.VISIBLE
                                            // جرب عدة أشكال للكتابة
                                            val variants = listOf(
                                                cleanName,
                                                cleanName.replaceFirstChar { it.uppercase() },
                                                cleanName.uppercase(),
                                                cleanName.lowercase()
                                            ).distinct()

                                            var found = false
                                            for (query in variants) {
                                                val resp = RetrofitClient.getApi(requireContext())
                                                    .searchMedicine(session.getBearerToken(), query)
                                                if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                                                    val med = resp.body()!![0]
                                                    selectedMedicine = med
                                                    etDosage.setText(med.strength ?: "")
                                                    tvSelectedMedName.text = "✅ Found: ${med.name}"
                                                    layoutSelectedMed.visibility = View.VISIBLE
                                                    toast("✅ ${med.name} detected!")
                                                    found = true
                                                    break
                                                }
                                            }
                                            if (!found) {
                                                toast("❌ '$cleanName' not found. Try typing manually.")
                                                btnTabType.performClick()
                                                etSearch.setText(cleanName)
                                            }
                                        } catch (e: Exception) {
                                            toast("Search error: ${e.message}")
                                        } finally {
                                            progressBar.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        }
                        .addOnFailureListener {
                            toast("OCR Error: ${it.message}")
                            btnTabType.performClick()
                        }
                }
            }
            cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        }

        // ── Search — بيجرب عدة أشكال للكتابة تلقائياً ──
        btnSearch.setOnClickListener {
            val raw = etSearch.text.toString().trim()
            if (raw.isEmpty()) { toast("Enter medicine name"); return@setOnClickListener }
            progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                try {
                    // جرب: النص الأصلي، أول حرف كبير، كل الأحرف كبيرة، كل الأحرف صغيرة
                    val variants = listOf(
                        raw,
                        raw.replaceFirstChar { it.uppercase() },
                        raw.uppercase(),
                        raw.lowercase()
                    ).distinct()

                    var found = false
                    for (query in variants) {
                        val resp = RetrofitClient.getApi(requireContext())
                            .searchMedicine(session.getBearerToken(), query)
                        if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                            selectedMedicine = resp.body()!![0]
                            etDosage.setText(selectedMedicine!!.strength ?: "")
                            tvSelectedMedName.text = "✅ Selected: ${selectedMedicine!!.name}"
                            layoutSelectedMed.visibility = View.VISIBLE
                            toast("✅ Found: ${selectedMedicine!!.name}")
                            found = true
                            break
                        }
                    }
                    if (!found) toast("No medicines found for: \"$raw\"")

                } catch (e: Exception) { toast("Search failed") }
                finally { progressBar.visibility = View.GONE }
            }
        }

        // ── Medicine Type ──
        rbPermanent.setOnCheckedChangeListener { _, checked ->
            if (checked) layoutDuration.visibility = View.GONE
        }
        rbTemporary.setOnCheckedChangeListener { _, checked ->
            if (checked) layoutDuration.visibility = View.VISIBLE
        }

        // ── Duration ──
        val durButtons = listOf(btnDur7 to 7, btnDur14 to 14, btnDur30 to 30)
        durButtons.forEach { (btn, days) ->
            btn.setOnClickListener {
                selectedDuration = days
                etCustomDuration.setText("")
                durButtons.forEach { (b, _) ->
                    b.background = resources.getDrawable(R.drawable.bg_btn_outline_primary, null)
                    b.setTextColor(0xFF6366F1.toInt())
                }
                btn.background = resources.getDrawable(R.drawable.bg_btn_primary, null)
                btn.setTextColor(0xFFFFFFFF.toInt())
            }
        }

        // ── Frequency + Time + Dropdown ──
        fun buildTimePickers(freq: Int) {
            selectedTimes.clear()
            timingInstructions.clear()
            layoutTimePickers.removeAllViews()

            val defaultTimes = when (freq) {
                1 -> listOf("08:00")
                2 -> listOf("08:00", "20:00")
                3 -> listOf("08:00", "14:00", "20:00")
                4 -> listOf("06:00", "12:00", "18:00", "22:00")
                else -> listOf("08:00")
            }
            selectedTimes.addAll(defaultTimes)

            val timingOptions = arrayOf(
                "No Special Instruction",
                "Before Meal",
                "After Meal",
                "With Meal",
                "Empty Stomach",
                "Before Sleep"
            )
            timingInstructions.addAll(List(freq) { "No Special Instruction" })

            defaultTimes.forEachIndexed { index, defaultTime ->

                // ✅ Simple row: [08:00 button] [Spinner] — exactly like the photo!
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.setMargins(0, 0, 0, 10) }
                }

                // Time button — dark blue, white text
                val btnTime = Button(requireContext()).apply {
                    text = defaultTime
                    textSize = 15f
                    setTextColor(0xFFFFFFFF.toInt())
                    background = resources.getDrawable(R.drawable.bg_btn_primary, null)
                    stateListAnimator = null
                    isAllCaps = false
                    layoutParams = LinearLayout.LayoutParams(
                        dpToPx(105), dpToPx(52)
                    ).also { it.setMargins(0, 0, 10, 0) }
                }
                btnTime.setOnClickListener {
                    val parts = selectedTimes[index].split(":")
                    val hour   = parts[0].toIntOrNull() ?: 8
                    val minute = parts[1].toIntOrNull() ?: 0
                    TimePickerDialog(requireContext(), { _, h, m ->
                        val time = String.format("%02d:%02d", h, m)
                        selectedTimes[index] = time
                        btnTime.text = time
                    }, hour, minute, true).show()
                }

                // Spinner — خلفية بيضاء + خط أسود
                val spinner = Spinner(requireContext())
                spinner.layoutParams = LinearLayout.LayoutParams(0, dpToPx(52), 1f)
                spinner.setBackgroundColor(android.graphics.Color.WHITE)
                spinner.setPadding(12, 0, 12, 0)

                // Adapter مخصص — خلفية بيضاء + نص أسود في الـ selected view
                val spinnerAdapter = object : ArrayAdapter<String>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    timingOptions
                ) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent)
                        (view as? TextView)?.apply {
                            setTextColor(android.graphics.Color.BLACK)
                            setBackgroundColor(android.graphics.Color.WHITE)
                            textSize = 13f
                            setPadding(8, 0, 8, 0)
                        }
                        return view
                    }
                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getDropDownView(position, convertView, parent)
                        (view as? TextView)?.apply {
                            setTextColor(android.graphics.Color.BLACK)
                            setBackgroundColor(android.graphics.Color.WHITE)
                            textSize = 13f
                            setPadding(16, 12, 16, 12)
                        }
                        return view
                    }
                }
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = spinnerAdapter

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                        timingInstructions[index] = timingOptions[pos]
                        (v as? TextView)?.setTextColor(android.graphics.Color.BLACK)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }

                row.addView(btnTime)
                row.addView(spinner)
                layoutTimePickers.addView(row)
            }
        }

        val freqButtons = listOf(btnFreq1 to 1, btnFreq2 to 2, btnFreq3 to 3, btnFreq4 to 4)
        freqButtons.forEach { (btn, freq) ->
            btn.setOnClickListener {
                selectedFrequency = freq
                buildTimePickers(freq)
                freqButtons.forEach { (b, _) ->
                    b.background = resources.getDrawable(R.drawable.bg_btn_outline_primary, null)
                    b.setTextColor(0xFF6366F1.toInt())
                }
                btn.background = resources.getDrawable(R.drawable.bg_btn_primary, null)
                btn.setTextColor(0xFFFFFFFF.toInt())
            }
        }

        buildTimePickers(1)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        btnConfirm.setOnClickListener {
            val med = selectedMedicine
            if (med == null) { toast("Please select a medicine first"); return@setOnClickListener }

            val isTemporary   = rbTemporary.isChecked
            val finalDuration = if (isTemporary)
                etCustomDuration.text.toString().toIntOrNull() ?: selectedDuration
            else 0

            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val endDate   = if (isTemporary) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, finalDuration)
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            } else null

            val instructionsStr = selectedTimes.mapIndexed { i, time ->
                val instr = timingInstructions.getOrElse(i) { "No Special Instruction" }
                if (instr == "No Special Instruction") time else "$time → $instr"
            }.joinToString(" · ")

            val dosage = etDosage.text.toString().trim()

            lifecycleScope.launch {
                try {
                    val request = AddMedicineRequest(
                        catalog       = med.catalogId,
                        user_id       = session.getUserId(),
                        dosage        = dosage.ifEmpty { null },
                        instructions  = instructionsStr,
                        start_date    = startDate,
                        end_date      = endDate,
                        duration_type = if (isTemporary) "temporary" else "permanent"
                    )
                    val resp = RetrofitClient.getApi(requireContext()).addMedicine(session.getBearerToken(), request)
                    val body = resp.body()

                    // دالة داخلية: أضف للقائمة المحلية وجدول الإشعارات
                    fun finalizeAdd(userMed: com.medicineapp.data.models.UserMedicine) {
                        userMedicines.add(0, userMed)
                        adapter.notifyItemInserted(0)
                        selectedTimes.forEachIndexed { i, time ->
                            lifecycleScope.launch {
                                try {
                                    RetrofitClient.getApi(requireContext()).addSchedule(
                                        session.getBearerToken(),
                                        AddScheduleRequest(
                                            medicine   = userMed.medicineId,
                                            timeOfDay  = time,
                                            repeatType = if (isTemporary) "temporary" else "daily",
                                            daysOfWeek = "1,2,3,4,5,6,7"
                                        )
                                    )
                                } catch (e: Exception) { Log.e("SCHEDULE", "Failed: $time") }
                            }
                            scheduleLocalReminder(
                                medicineId   = userMed.medicineId,
                                medicineName = med.name,
                                timeStr      = time,
                                instruction  = timingInstructions.getOrElse(i) { "" },
                                doseIndex    = i
                            )
                        }
                    }

                    if (resp.isSuccessful && body != null) {

                        Log.d("ADD_MED", "severity=${body.severity} warning=${body.warning} desc=${body.description}")

                        when (body.severity?.trim()?.lowercase()) {

                            // ── تعارض متوسط — اسأل المستخدم ──
                            "medium" -> {
                                AlertDialog.Builder(requireContext())
                                    .setTitle("🟡 Moderate Interaction Detected")
                                    .setMessage(
                                        "${body.description ?: ""}\n\n" +
                                        "Are you sure you want to add this medicine?"
                                    )
                                    .setPositiveButton("✅ Yes, Add") { _, _ ->
                                        body.data?.let { finalizeAdd(it) }
                                        dialog.dismiss()
                                        toast("✅ Medicine added with warning")
                                    }
                                    .setNegativeButton("❌ No, Cancel") { _, _ ->
                                        // احذف من السيرفر لأنه اتضاف قبل تأكيدنا
                                        lifecycleScope.launch {
                                            body.data?.medicineId?.let { id ->
                                                try {
                                                    RetrofitClient.getApi(requireContext())
                                                        .deleteMedicine(session.getBearerToken(), id)
                                                } catch (e: Exception) { }
                                            }
                                        }
                                        toast("❌ Medicine not added")
                                    }
                                    .setCancelable(false)
                                    .show()
                            }

                            // ── تعارض منخفض — اسأل المستخدم ──
                            "low" -> {
                                AlertDialog.Builder(requireContext())
                                    .setTitle("🟢 Minor Interaction Detected")
                                    .setMessage(
                                        "${body.description ?: ""}\n\n" +
                                        "Are you sure you want to add this medicine?"
                                    )
                                    .setPositiveButton("✅ Yes, Add") { _, _ ->
                                        body.data?.let { finalizeAdd(it) }
                                        dialog.dismiss()
                                        toast("✅ Medicine added")
                                    }
                                    .setNegativeButton("❌ No, Cancel") { _, _ ->
                                        lifecycleScope.launch {
                                            body.data?.medicineId?.let { id ->
                                                try {
                                                    RetrofitClient.getApi(requireContext())
                                                        .deleteMedicine(session.getBearerToken(), id)
                                                } catch (e: Exception) { }
                                            }
                                        }
                                        toast("❌ Medicine not added")
                                    }
                                    .setCancelable(false)
                                    .show()
                            }

                            // ── تعارض عالي (200 مش 400) — أضف وحذّر ──
                            "high" -> {
                                body.data?.let { finalizeAdd(it) }
                                dialog.dismiss()
                                AlertDialog.Builder(requireContext())
                                    .setTitle("🔴 High Risk!")
                                    .setMessage("${body.description}\n\nPlease consult your doctor.")
                                    .setPositiveButton("OK", null).show()
                            }

                            // ── لا يوجد تعارض ──
                            else -> {
                                body.data?.let { finalizeAdd(it) }
                                dialog.dismiss()
                                toast("✅ Medicine added!")
                            }
                        }

                    } else if (resp.code() == 400) {
                        // ══ HIGH RISK من السيرفر (400) — لا تتغير ══
                        AlertDialog.Builder(requireContext())
                            .setTitle("🔴 Cannot Add")
                            .setMessage("HIGH RISK!\n\n${body?.description ?: ""}\n\nPlease consult your doctor.")
                            .setPositiveButton("OK", null).show()
                    } else {
                        toast(body?.error ?: "Failed to add")
                    }
                } catch (e: Exception) { toast("Error: ${e.message}") }
            }
        }

        dialog.show()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun scheduleLocalReminder(
        medicineId: Int, medicineName: String,
        timeStr: String, instruction: String, doseIndex: Int
    ) {
        NotificationHelper.scheduleReminder(
            context      = requireContext(),
            medicineId   = medicineId,
            medicineName = medicineName,
            timeStr      = timeStr,
            instruction  = instruction,
            doseIndex    = doseIndex
        )
    }

    private fun createNotificationChannel() {
        NotificationHelper.createChannel(requireContext())
    }

    private fun confirmDelete(med: UserMedicine) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Medicine")
            .setMessage("Remove ${med.medicineName}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val resp = RetrofitClient.getApi(requireContext()).deleteMedicine(
                            session.getBearerToken(), med.medicineId
                        )
                        if (resp.isSuccessful) {
                            val idx = userMedicines.indexOf(med)
                            if (idx >= 0) { userMedicines.removeAt(idx); adapter.notifyItemRemoved(idx) }
                            toast("Removed")
                        }
                    } catch (e: Exception) { toast("Delete failed") }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}

class UserMedicineAdapter(
    private val items: MutableList<UserMedicine>,
    private val onDelete: (UserMedicine) -> Unit
) : RecyclerView.Adapter<UserMedicineAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName    : TextView    = v.findViewById(R.id.tvMedName)
        val tvDetails : TextView    = v.findViewById(R.id.tvMedDetails)
        val tvDosage  : TextView    = v.findViewById(R.id.tvDosage)
        val btnDelete : ImageButton = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicine_card, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val med = items[position]
        holder.tvName.text    = med.medicineName ?: "Medicine"
        holder.tvDetails.text = "${med.medicineForm ?: ""} · ${med.medicineStrength ?: ""}"
        holder.tvDosage.text  = med.dosage ?: "No dosage"
        holder.btnDelete.visibility = View.VISIBLE
        holder.btnDelete.setOnClickListener { onDelete(med) }

        val today     = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val isExpired = med.endDate != null && med.endDate < today
        if (isExpired) {
            holder.itemView.alpha = 0.4f
            holder.tvDosage.text  = "✓ Course Completed"
        } else {
            holder.itemView.alpha = 1.0f
        }
    }
}