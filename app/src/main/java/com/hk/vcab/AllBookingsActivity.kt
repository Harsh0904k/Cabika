package com.hk.vcab

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AllBookingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Hardcoded admin emails
    private val adminEmails = listOf("vcabadmin1@gmail.com", "admin2@example.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Admin check
        val currentUser = auth.currentUser
        val email = currentUser?.email ?: ""
        if (!adminEmails.contains(email)) {
            Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show()
            finish() // Close activity immediately if not admin
            return
        }

        setContentView(R.layout.activity_all_bookings)

        recyclerView = findViewById(R.id.recyclerAllBookings)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadBookings()
    }

    private fun loadBookings() {
        db.collection("bookings")
            .get()
            .addOnSuccessListener { snapshot ->
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                val bookings = snapshot.documents.mapNotNull { doc ->
                    try {
                        val dateStr = doc.getString("date") ?: ""
                        val timeStr = doc.getString("time") ?: ""
                        val dateTime = dateFormat.parse("$dateStr $timeStr") ?: Date(0)

                        val creatorCount = (doc.getLong("passengerCount") ?: 0).toInt()
                        val joinedUsersMap = doc.get("joinedUids") as? Map<String, Number> ?: emptyMap()
                        val joinedCount = joinedUsersMap.values.sumOf { it.toInt() }
                        val totalPassengers = creatorCount + joinedCount

                        Booking(
                            rideId = doc.id,
                            name = doc.getString("name") ?: "",
                            pickup = doc.getString("pickup") ?: "",
                            drop = doc.getString("drop") ?: "",
                            date = dateStr,
                            time = timeStr,
                            passengerCount = totalPassengers.toLong(),
                            carType = doc.getString("carType") ?: "",
                            uid = doc.getString("uid") ?: "",
                            driverId = doc.getString("driverId"),
                            driverName = doc.getString("driverName"),
                            driverRequired = doc.getBoolean("driverRequired") ?: true // ✅ Boolean
                        ) to dateTime
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                    .sortedBy { it.second }
                    .map { it.first }

                recyclerView.adapter = BookingAdapter(bookings) { booking ->
                    val intent = Intent(this, RideDetailsActivity::class.java)
                    intent.putExtra("rideId", booking.rideId)
                    startActivity(intent)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading bookings", Toast.LENGTH_SHORT).show()
            }
    }

}
