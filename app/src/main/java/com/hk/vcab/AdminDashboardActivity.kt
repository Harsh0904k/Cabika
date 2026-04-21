package com.hk.vcab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var btnViewBookings: Button
    private lateinit var btnViewUsers: Button
    private lateinit var btnViewDrivers: Button
    private lateinit var btnLogout: Button

    // Hardcoded admin emails
    private val adminEmails = listOf("vcabadmin1@gmail.com", "admin2@example.com")

    // SharedPreferences keys
    private val PREFS_NAME = "vcab_prefs"
    private val KEY_ROLE = "user_role"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: ""

        // ✅ Admin check
        if (!adminEmails.contains(email)) {
            Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show()
            finish()  // Close activity immediately if not admin
            return
        }

        btnViewBookings = findViewById(R.id.btnViewBookings)
        btnViewUsers = findViewById(R.id.btnViewUsers)
        btnViewDrivers = findViewById(R.id.btnViewDrivers)
        btnLogout = findViewById(R.id.btnLogout)

        btnViewBookings.setOnClickListener {
            startActivity(Intent(this, AllBookingsActivity::class.java))
        }

        btnViewUsers.setOnClickListener {
            startActivity(Intent(this, AllUsersActivity::class.java))
        }

        btnViewDrivers.setOnClickListener {
            startActivity(Intent(this, AllDriversActivity::class.java))
        }

        btnLogout.setOnClickListener {
            logoutAdmin()
        }
    }

    private fun logoutAdmin() {
        // Clear cached role to prevent wrong redirection
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_ROLE).apply()

        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut()

        // Redirect to LoginActivity
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
