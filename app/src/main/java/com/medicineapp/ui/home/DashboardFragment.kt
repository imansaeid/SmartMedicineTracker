package com.medicineapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
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
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        val tvMedCount  = view.findViewById<TextView>(R.id.tvMedCount)
        val tvWarnCount = view.findViewById<TextView>(R.id.tvWarnCount)
        val tvTodayCount = view.findViewById<TextView>(R.id.tvTodayCount)
        val tvUserName  = view.findViewById<TextView>(R.id.tvUserName)
        val rvMedicines = view.findViewById<RecyclerView>(R.id.rvMedicines)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        tvUserName.text = "${session.getName() ?: "User"} 👋"
        rvMedicines.layoutManager = LinearLayoutManager(requireContext())

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getUserMedicines(session.getBearerToken())
                if (response.isSuccessful) {
                    val meds = response.body() ?: emptyList()
                    tvMedCount.text = "${meds.size}"
                    tvTodayCount.text = "${meds.size}"
                    tvWarnCount.text = "0"
                    rvMedicines.adapter = TodayMedicineAdapter(meds)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}

class TodayMedicineAdapter(private val items: List<UserMedicine>) :
    RecyclerView.Adapter<TodayMedicineAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName     : TextView = v.findViewById(R.id.tvMedName)
        val tvDetails  : TextView = v.findViewById(R.id.tvMedDetails)
        val tvDosage   : TextView = v.findViewById(R.id.tvDosage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_medicine_card, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val med = items[position]
        holder.tvName.text    = med.medicineName ?: "Medicine"
        holder.tvDetails.text = "${med.medicineForm ?: ""} · ${med.medicineStrength ?: ""}"
        holder.tvDosage.text  = med.dosage ?: ""
    }
}