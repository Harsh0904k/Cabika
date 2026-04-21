package com.hk.vcab

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class JoinRideActivity : BaseActivity() {

    private lateinit var passengersInput: EditText
    private lateinit var btnJoin: Button
    private lateinit var lottieLoading: LottieAnimationView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var matchedRideId: String? = null
    private var myPassengerCount: Int = 1
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_ride)

        passengersInput = findViewById(R.id.editTextPassengers)
        btnJoin = findViewById(R.id.buttonJoinRide)
        lottieLoading = findViewById(R.id.lottieLoading)

        matchedRideId = intent.getStringExtra("rideId")
        userId = auth.currentUser?.uid

        btnJoin.setOnClickListener {
            val passengersStr = passengersInput.text.toString().trim()
            if (passengersStr.isEmpty()) {
                Toast.makeText(this, "Enter number of passengers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val passengers = passengersStr.toIntOrNull()
            if (passengers == null || passengers <= 0) {
                Toast.makeText(this, "Enter valid passenger count", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            myPassengerCount = passengers
            joinRide()
        }
    }

    private fun joinRide() {
        if (matchedRideId == null || userId == null) return

        // Show loading animation and disable button
        lottieLoading.visibility = View.VISIBLE
        btnJoin.isEnabled = false

        val rideRef = db.collection("bookings").document(matchedRideId!!)

        rideRef.get().addOnSuccessListener { rideDoc ->
            if (!rideDoc.exists()) {
                lottieLoading.visibility = View.GONE
                btnJoin.isEnabled = true
                Toast.makeText(this, "Ride does not exist", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val joinedUids = rideDoc.get("joinedUids") as? Map<String, Long> ?: emptyMap()
            val creatorPassengerCount = rideDoc.getLong("passengerCount")?.toInt() ?: 1

            if (joinedUids.containsKey(userId)) {
                lottieLoading.visibility = View.GONE
                btnJoin.isEnabled = true
                Toast.makeText(this, "You have already joined this ride", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val totalJoined = joinedUids.values.sumOf { it.toInt() }
            val rideCarType = rideDoc.getString("carType") ?: "5-seater"
            val capacity = if (rideCarType == "5-seater") 5 else 7

            if (totalJoined + creatorPassengerCount + myPassengerCount <= capacity) {
                val updatedMap = joinedUids.toMutableMap()
                updatedMap[userId!!] = myPassengerCount.toLong()

                rideRef.update("joinedUids", updatedMap)
                    .addOnSuccessListener {
                        val myRideDoc = hashMapOf(
                            "rideId" to matchedRideId,
                            "userId" to userId,
                            "userName" to (auth.currentUser?.displayName ?: "Unknown"),
                            "passengerCount" to myPassengerCount
                        )

                        db.collection("myRides").add(myRideDoc)
                            .addOnSuccessListener {
                                lottieLoading.visibility = View.GONE
                                Toast.makeText(this, "Ride joined successfully", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MyRidesActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                            }
                            .addOnFailureListener {
                                lottieLoading.visibility = View.GONE
                                btnJoin.isEnabled = true
                                Toast.makeText(this, "Failed to add ride to MyRides", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        lottieLoading.visibility = View.GONE
                        btnJoin.isEnabled = true
                        Toast.makeText(this, "Failed to join ride", Toast.LENGTH_SHORT).show()
                    }
            } else {
                lottieLoading.visibility = View.GONE
                btnJoin.isEnabled = true
                Toast.makeText(this, "Not enough seats available", Toast.LENGTH_SHORT).show()
            }

        }.addOnFailureListener {
            lottieLoading.visibility = View.GONE
            btnJoin.isEnabled = true
            Toast.makeText(this, "Failed to fetch ride", Toast.LENGTH_SHORT).show()
        }
    }
}
