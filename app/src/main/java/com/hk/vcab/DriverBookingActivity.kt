package com.hk.vcab

import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DriverBookingActivity : BaseActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rideListView: ListView
    private lateinit var lottieLoading: LottieAnimationView
    private val rideList: MutableList<Map<String, Any>> = mutableListOf()
    private lateinit var adapter: RideAdapter
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_booking)

        db = FirebaseFirestore.getInstance()

        rideListView = findViewById(R.id.rideListView)
        lottieLoading = findViewById(R.id.lottieCarLoading)

        adapter = RideAdapter(this, rideList)
        rideListView.adapter = adapter

        checkIfDriverAndLoad()
    }

    private fun checkIfDriverAndLoad() {
        val currentUser = auth.currentUser
        val email = currentUser?.email?.trim()?.lowercase() ?: ""

        if (email.isEmpty()) {
            Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check Firestore Drivers collection
        db.collection("Drivers")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    loadAvailableRides()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error verifying driver", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadAvailableRides() {
        lottieLoading.visibility = View.VISIBLE

        db.collection("bookings")
            .get()
            .addOnSuccessListener { result ->
                rideList.clear()
                val tempList = mutableListOf<Pair<Map<String, Any>, Date>>()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                for (document in result) {
                    val driverId = document.getString("driverId")
                    val driverRequired = document.getBoolean("driverRequired") ?: false

                    // ✅ Only show rides with no driver AND driverRequired = true
                    if (driverId.isNullOrEmpty() && driverRequired) {
                        val ride = document.data.toMutableMap()
                        ride["id"] = document.id

                        val creatorCount = (ride["passengerCount"] as? Number)?.toInt() ?: 1
                        val joinedUids = ride["joinedUids"] as? Map<String, Number> ?: emptyMap()
                        val totalPassengers = creatorCount + joinedUids.values.sumOf { it.toInt() }
                        ride["totalPassengers"] = totalPassengers

                        val dateStr = ride["date"] as? String ?: "01/01/2000"
                        val timeStr = ride["time"] as? String ?: "00:00"
                        val dateTime = try { dateFormat.parse("$dateStr $timeStr") ?: Date(0) } catch (e: Exception) { Date(0) }
                        ride["dateTime"] = dateTime

                        tempList.add(Pair(ride, dateTime))
                    }
                }

                // Sort rides by dateTime ascending
                tempList.sortBy { it.second }
                rideList.addAll(tempList.map { it.first })

                adapter.notifyDataSetChanged()
                lottieLoading.visibility = View.GONE
            }
            .addOnFailureListener {
                lottieLoading.visibility = View.GONE
                Toast.makeText(this, "Failed to load rides", Toast.LENGTH_SHORT).show()
            }
    }

}
