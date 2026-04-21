package com.hk.vcab

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Notification permission granted ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications disabled ❌", Toast.LENGTH_SHORT).show()
            }
        }

    private val PREFS_NAME = "vcab_prefs"
    private val KEY_ROLE = "user_role" // "driver", "student", "admin"
    private val KEY_FIRST_LAUNCH = "is_first_launch"

    // 🔹 Hardcoded admin emails
    private val adminEmails = listOf("vcabadmin1@gmail.com", "admin2@example.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Ask for notification permission if needed
        askNotificationPermission()

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

        val nextButton = findViewById<Button>(R.id.nextButton)
        val bounce = AnimationUtils.loadAnimation(this, R.anim.bounce)
        nextButton.startAnimation(bounce)

        nextButton.setOnClickListener {
            nextButton.startAnimation(bounce)

            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

            // ✅ First launch → go to LoginActivity
            if (isFirstLaunch) {
                prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return@setOnClickListener
            }

            // ✅ Check cached role
            val cachedRole = prefs.getString(KEY_ROLE, "")
            when (cachedRole) {
                "admin" -> {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                    return@setOnClickListener
                }
                "driver" -> {
                    startActivity(Intent(this, DriverDashboardActivity::class.java))
                    finish()
                    return@setOnClickListener
                }
                "student" -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    return@setOnClickListener
                }
            }

            // ✅ No cached role → check Firebase
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                // Not logged in → go to LoginActivity
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return@setOnClickListener
            }

            val email = user.email ?: ""

            // 🔹 Admin check first
            if (adminEmails.contains(email)) {
                prefs.edit().putString(KEY_ROLE, "admin").apply()
                startActivity(Intent(this, AdminDashboardActivity::class.java))
                finish()
                return@setOnClickListener
            }

            val db = FirebaseFirestore.getInstance()

            // Check if user is a driver
            db.collection("Drivers")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { driverResult ->
                    if (!driverResult.isEmpty) {
                        prefs.edit().putString(KEY_ROLE, "driver").apply()
                        startActivity(Intent(this, DriverDashboardActivity::class.java))
                        finish()
                        return@addOnSuccessListener
                    }

                    // Check if user exists in "users" collection
                    db.collection("users").document(user.uid).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                // Existing student
                                prefs.edit().putString(KEY_ROLE, "student").apply()
                                startActivity(Intent(this, DashboardActivity::class.java))
                                finish()
                            } else {
                                // New user → RegisterActivity (never for admin)
                                val intent = Intent(this, RegisterActivity::class.java)
                                intent.putExtra("prefill_email", user.email)
                                intent.putExtra("prefill_name", user.displayName)
                                startActivity(intent)
                                finish()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error checking user data", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error checking driver data", Toast.LENGTH_SHORT).show()
                }
        }

    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
