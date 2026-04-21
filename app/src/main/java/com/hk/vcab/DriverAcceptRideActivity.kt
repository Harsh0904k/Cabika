package com.hk.vcab

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DriverAcceptRideActivity : BaseActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var etDriverName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etConfirmPhone: EditText
    private lateinit var etCarName: EditText
    private lateinit var btnConfirmRide: Button

    private var rideId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_accept_ride)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        etDriverName = findViewById(R.id.etDriverName)
        etPhone = findViewById(R.id.etPhone)
        etConfirmPhone = findViewById(R.id.etConfirmPhone)
        etCarName = findViewById(R.id.etCarName)
        btnConfirmRide = findViewById(R.id.btnConfirmRide)

        rideId = intent.getStringExtra("rideId")
        if (rideId == null) {
            Toast.makeText(this, "Ride not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnConfirmRide.setOnClickListener {
            val name = etDriverName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val confirmPhone = etConfirmPhone.text.toString().trim()
            val carName = etCarName.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || confirmPhone.isEmpty() || carName.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phone != confirmPhone) {
                Toast.makeText(this, "Phone numbers do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            rideId?.let { id ->
                val driverId = auth.currentUser?.uid ?: ""
                val driverData = hashMapOf(
                    "driverId" to driverId,
                    "driverName" to name,
                    "driverPhone" to phone,
                    "carName" to carName
                )

                db.collection("bookings").document(id)
                    .update(driverData as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Ride accepted successfully", Toast.LENGTH_SHORT).show()
                        // Navigate to DriverAcceptedRideActivity
                        val intent = Intent(this, DriverAcceptedRideActivity::class.java)
                        startActivity(intent)
                        finish() // close this page
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to accept ride: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

    }
}
