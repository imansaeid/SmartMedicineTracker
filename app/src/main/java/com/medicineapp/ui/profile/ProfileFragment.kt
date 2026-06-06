package com.medicineapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.medicineapp.R
import com.medicineapp.data.network.RetrofitClient
import com.medicineapp.data.network.SessionManager
import com.medicineapp.ui.home.HomeActivity
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        val tvName    = view.findViewById<TextView>(R.id.tvName)
        val tvEmail   = view.findViewById<TextView>(R.id.tvEmail)
        val etName    = view.findViewById<EditText>(R.id.etNewName)
        val btnSave   = view.findViewById<Button>(R.id.btnSave)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        tvName.text  = session.getName() ?: "User"
        tvEmail.text = session.getEmail() ?: ""

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            if (newName.isEmpty()) { toast("Enter a name"); return@setOnClickListener }

            lifecycleScope.launch {
                try {
                    val resp = RetrofitClient.getApi(requireContext()).updateProfile(
                        session.getBearerToken(),
                        mapOf("name" to newName)
                    )
                    if (resp.isSuccessful) {
                        session.saveSession(session.getToken()!!, session.getUserId(), newName)
                        tvName.text = newName
                        etName.setText("")
                        toast("Profile updated!")
                    }
                } catch (e: Exception) { toast("Update failed") }
            }
        }

        btnLogout.setOnClickListener {
            (activity as? HomeActivity)?.logout()
        }
    }

    private fun showServerUrlDialog() {
        val currentUrl = RetrofitClient.getSavedUrl(requireContext())

        val input = EditText(requireContext()).apply {
            setText(currentUrl)
            hint = "http://192.168.x.x:8000/"
            setPadding(48, 24, 48, 24)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("🌐 Server URL")
            .setMessage("Enter your server IP:\n(مثال: http://192.168.1.46:8000/)")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.startsWith("http")) {
                    RetrofitClient.saveUrl(requireContext(), newUrl)
                    toast("✅ Server URL saved!")
                } else {
                    toast("❌ URL must start with http://")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
