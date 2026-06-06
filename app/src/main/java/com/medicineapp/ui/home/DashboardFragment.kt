package com.medicineapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medicineapp.R
import com.medicineapp.data.models.UserMedicine
import com.medicineapp.data.network.RetrofitClient
import com.medicineapp.data.network.SessionManager
import com.medicineapp.ui.medicines.MedicinesFragment
import com.medicineapp.ui.scan.ScanActivity
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardFragment : Fragment() {

    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        val tvMedCount   = view.findViewById<TextView>(R.id.tvMedCount)
        val tvWarnCount  = view.findViewById<TextView>(R.id.tvWarnCount)
        val tvUserName   = view.findViewById<TextView>(R.id.tvUserName)
        val tvGreeting   = view.findViewById<TextView>(R.id.tvGreeting)
        val rvMedicines  = view.findViewById<RecyclerView>(R.id.rvMedicines)
        val progressBar  = view.findViewById<ProgressBar>(R.id.progressBar)
        val layoutEmpty  = view.findViewById<LinearLayout>(R.id.layoutEmpty)
        val tvSeeAll     = view.findViewById<TextView>(R.id.tvSeeAll)
        val cardScan     = view.findViewById<LinearLayout>(R.id.cardScan)
        val cardAddMed   = view.findViewById<LinearLayout>(R.id.cardAddMed)
        val btnMenu      = view.findViewById<ImageButton>(R.id.btnMenu)

        // تحية حسب الوقت
        tvGreeting.text = getGreeting()
        tvUserName.text = "${session.getName() ?: "User"} 👋"

        rvMedicines.layoutManager = LinearLayoutManager(requireContext())

        // ── زر Menu ──
        btnMenu.setOnClickListener {
            (activity as? HomeActivity)?.openDrawer()
        }

        // ── زر Scan Medicine ──
        cardScan.setOnClickListener {
            startActivity(Intent(requireContext(), ScanActivity::class.java))
        }

        // ── زر Add Medicine ──
        cardAddMed.setOnClickListener {
            (activity as? HomeActivity)?.navigateToMedicines()
        }

        // ── See all ──
        tvSeeAll.setOnClickListener {
            (activity as? HomeActivity)?.navigateToMedicines()
        }

        // ── Load medicines ──
        loadMedicines(tvMedCount, tvWarnCount, rvMedicines, progressBar, layoutEmpty)
    }

    private fun loadMedicines(
        tvMedCount: TextView,
        tvWarnCount: TextView,
        rvMedicines: RecyclerView,
        progressBar: ProgressBar,
        layoutEmpty: LinearLayout
    ) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApi(requireContext())
                val token = session.getBearerToken()

                // جيب الأدوية
                val response = api.getUserMedicines(token)
                if (response.isSuccessful) {
                    val meds = response.body() ?: emptyList()
                    tvMedCount.text = "${meds.size}"

                    if (meds.isEmpty()) {
                        layoutEmpty.visibility = View.VISIBLE
                        rvMedicines.visibility = View.GONE
                    } else {
                        layoutEmpty.visibility = View.GONE
                        rvMedicines.visibility = View.VISIBLE
                        rvMedicines.adapter    = TodayMedicineAdapter(meds)
                    }
                }

                // جيب عدد التعارضات
                try {
                    val intResp = api.getInteractionCount(token)
                    if (intResp.isSuccessful) {
                        val count = intResp.body()?.get("interaction_count") ?: 0
                        tvWarnCount.text = "$count"
                    }
                } catch (e: Exception) {
                    tvWarnCount.text = "0"
                }

            } catch (e: Exception) {
                // لا تعرض خطأ — بس اتركها فاضية
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun getGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good Morning,"
            in 12..17 -> "Good Afternoon,"
            in 18..21 -> "Good Evening,"
            else      -> "Good Night,"
        }
    }
}

class TodayMedicineAdapter(private val items: List<UserMedicine>) :
    RecyclerView.Adapter<TodayMedicineAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName    : TextView = v.findViewById(R.id.tvMedName)
        val tvDetails : TextView = v.findViewById(R.id.tvMedDetails)
        val tvDosage  : TextView = v.findViewById(R.id.tvDosage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_medicine_card, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val med = items[position]
        holder.tvName.text    = med.medicineName ?: "Medicine"
        holder.tvDetails.text = listOfNotNull(med.medicineForm, med.medicineStrength)
            .filter { it.isNotBlank() }.joinToString(" · ")
        holder.tvDosage.text  = med.dosage ?: ""
    }
}
