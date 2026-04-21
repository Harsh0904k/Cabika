package com.hk.vcab

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DriverAllRidesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DriverRideAdapter
    private val rideList = ArrayList<Booking>()

    private val db = FirebaseFirestore.getInstance()
    private val currentDriverEmail = FirebaseAuth.getInstance().currentUser?.email
    private val currentDriverId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_all_rides)

        recyclerView = findViewById(R.id.recyclerDriverAllRides)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DriverRideAdapter(rideList)
        recyclerView.adapter = adapter

        fetchDriverRides()
    }

    private fun fetchDriverRides() {
        if (currentDriverEmail == null || currentDriverId == null) return

        rideList.clear()
        val allRides = mutableListOf<Booking>()

        // Helper to sort & update adapter
        fun updateAdapter() {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            allRides.sortBy { ride ->
                try {
                    dateFormat.parse("${ride.date} ${ride.time}")
                } catch (e: Exception) {
                    Date(0)
                }
            }
            rideList.clear()
            rideList.addAll(allRides)
            adapter.notifyDataSetChanged()
        }

        val btnAddManualRide: Button = findViewById(R.id.btnAddManualRide)
        btnAddManualRide.setOnClickListener {
            // Open Add Manual Ride Activity
            startActivity(Intent(this, DriverAddBookingActivity::class.java))
        }

        // 1️⃣ Manual rides added by driver
        db.collection("driverBookings")
            .document(currentDriverEmail)
            .collection("bookings")
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val booking = doc.toObject(Booking::class.java)
                    booking.rideId = doc.id
                    booking.driverEmail = currentDriverEmail
                    booking.driverName = booking.driverName ?: booking.name
                    booking.driverPhone = booking.driverPhone ?: ""
                    allRides.add(booking)
                }
                updateAdapter()
            }

        // 2️⃣ Rides assigned to this driver
        db.collection("bookings")
            .whereEqualTo("driverId", currentDriverId)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val booking = doc.toObject(Booking::class.java)
                    booking.rideId = doc.id

                    // ✅ Mark as Cabika ride
                    booking.isCabika = true

                    allRides.add(booking)
                }
                updateAdapter()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching rides", Toast.LENGTH_SHORT).show()
            }
    }
}
