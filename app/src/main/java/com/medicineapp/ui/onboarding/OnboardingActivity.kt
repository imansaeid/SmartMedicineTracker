package com.medicineapp.ui.onboarding



import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.medicineapp.R
import com.medicineapp.ui.auth.LoginActivity

data class OnboardPage(val emoji: String, val title: String, val description: String)

class OnboardingActivity : AppCompatActivity() {

    private val pages = listOf(
        OnboardPage("💊", "Track Your Medicines", "Keep all your medicines organized in one place. Never miss a dose again."),
        OnboardPage("🔍", "Smart Conflict Check", "We automatically check for dangerous drug interactions to keep you safe."),
        OnboardPage("⏰", "Daily Reminders", "Get timely notifications so you always take your medicine at the right time.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val btnNext   = findViewById<Button>(R.id.btnNext)
        val tvSkip    = findViewById<TextView>(R.id.tvSkip)
        val dotsLayout = findViewById<LinearLayout>(R.id.dotsLayout)

        viewPager.adapter = OnboardingAdapter(pages)
        setupDots(dotsLayout, 0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupDots(dotsLayout, position)
                btnNext.text = if (position == pages.size - 1) "Get Started" else "Next"
            }
        })

        btnNext.setOnClickListener {
            if (viewPager.currentItem < pages.size - 1) {
                viewPager.currentItem++
            } else {
                goToLogin()
            }
        }

        tvSkip.setOnClickListener { goToLogin() }
    }

    private fun goToLogin() {
        getSharedPreferences("MedicineAppPrefs", MODE_PRIVATE)
            .edit().putBoolean("onboarding_seen", true).apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setupDots(layout: LinearLayout, selected: Int) {
        layout.removeAllViews()
        pages.forEachIndexed { i, _ ->
            val dot = View(this)
            val params = LinearLayout.LayoutParams(
                if (i == selected) 28 else 8,
                8
            ).also { it.setMargins(4, 0, 4, 0) }
            dot.layoutParams = params
            dot.background = if (i == selected)
                getDrawable(R.drawable.bg_dot_active)
            else
                getDrawable(R.drawable.bg_dot_inactive)
            layout.addView(dot)
        }
    }
}

class OnboardingAdapter(private val pages: List<OnboardPage>) :
    RecyclerView.Adapter<OnboardingAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvEmoji      : TextView = v.findViewById(R.id.tvEmoji)
        val tvTitle      : TextView = v.findViewById(R.id.tvTitle)
        val tvDescription: TextView = v.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false))

    override fun getItemCount() = pages.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val page = pages[position]
        holder.tvEmoji.text       = page.emoji
        holder.tvTitle.text       = page.title
        holder.tvDescription.text = page.description
    }
}