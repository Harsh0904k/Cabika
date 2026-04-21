package com.hk.vcab

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DriverDashboardActivity : BaseActivity() {

    private lateinit var btnAvailableRides: Button
    private lateinit var btnMyAcceptedRide: Button
    private lateinit var btnPastRides: Button
    private lateinit var btnLogout: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_dashboard)

        btnAvailableRides = findViewById(R.id.btnAvailableRides)
        btnMyAcceptedRide = findViewById(R.id.btnMyAcceptedRide)
        btnPastRides = findViewById(R.id.btnPastRides)
        btnLogout = findViewById(R.id.btnLogout)

        btnAvailableRides.setOnClickListener {
            startActivity(Intent(this, DriverBookingActivity::class.java))
        }

        btnMyAcceptedRide.setOnClickListener {
            val currentDriverId = auth.currentUser?.uid ?: return@setOnClickListener

            // Query bookings where this driver is assigned
            db.collection("bookings")
                .whereEqualTo("driverId", currentDriverId)
                .get()
                .addOnSuccessListener { docs ->
                    if (!docs.isEmpty) {
                        val rideDoc = docs.documents[0] // assuming only one accepted ride
                        val rideId = rideDoc.id

                        // Open DriverAcceptedRideActivity with rideId
                        val intent = Intent(this, DriverAcceptedRideActivity::class.java)
                        intent.putExtra("rideId", rideId)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "No accepted rides found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error fetching accepted ride", Toast.LENGTH_SHORT).show()
                }
        }

        btnPastRides.setOnClickListener {
            // Open DriverPastRidesActivity
            startActivity(Intent(this, DriverPastRidesActivity::class.java))
        }

        val btnMyRides: MaterialButton = findViewById(R.id.btnMyRides)
        btnMyRides.setOnClickListener {
            val intent = Intent(this, DriverAllRidesActivity::class.java)
            intent.putExtra("driverEmail", auth.currentUser?.email ?: "")
            startActivity(intent)
        }


        btnLogout.setOnClickListener {
            // Clear cached role for driver
            val rolePref = getSharedPreferences("vcab_prefs", MODE_PRIVATE)
            rolePref.edit().remove("user_role").apply()

            // Firebase logout
            auth.signOut()

            // Navigate to HomeActivity (not LoginActivity directly)
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
