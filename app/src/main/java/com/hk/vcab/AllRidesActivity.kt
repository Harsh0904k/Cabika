package com.hk.vcab

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class AllRidesActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RideCardAdapter
    private val rideList = ArrayList<RideCard>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userGender: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_rides)

        recyclerView = findViewById(R.id.listViewAllRides)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = RideCardAdapter(this, rideList)
        recyclerView.adapter = adapter

        // ✅ Book Ride button
        val bookRideBtn = findViewById<Button>(R.id.btnBookRide)
        bookRideBtn.setOnClickListener {
            val intent = Intent(this, BookRideActivity::class.java)
            startActivity(intent)
        }

        // ✅ Load user gender before showing rides
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { doc ->
                    userGender = doc.getString("gender")?.lowercase(Locale.ROOT)
                    loadAllRides()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                    loadAllRides()
                }
        } else {
            loadAllRides()
        }
    }

    private fun loadAllRides() {
        db.collection("bookings")
            .get()
            .addOnSuccessListener { result ->
                rideList.clear()
                for (document in result) {
                    val pickup = document.getString("pickup") ?: "Unknown"
                    val drop = document.getString("drop") ?: "Unknown"
                    val date = document.getString("date") ?: "Unknown"
                    val time = document.getString("time") ?: "Unknown"
                    val girlCabOnly = document.getBoolean("girlCabOnly") ?: false

                    val creatorId = document.getString("uid") ?: ""
                    val creatorCount = (document.getLong("passengerCount") ?: 1).toInt()
                    val joinedUsersMap =
                        document.get("joinedUids") as? Map<String, Number> ?: emptyMap()
                    val totalPassengers =
                        creatorCount + joinedUsersMap.values.sumOf { it.toInt() }

                    rideList.add(
                        RideCard(
                            id = document.id,
                            pickup = pickup,
                            drop = drop,
                            date = date,
                            time = time,
                            passengerCount = totalPassengers,
                            creatorId = creatorId,
                            joinedUids = joinedUsersMap.mapValues { it.value.toInt() },
                            girlCabOnly = girlCabOnly
                        )
                    )
                }

                // ✅ Sort rides by date then time
                try {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    rideList.sortWith(compareBy<RideCard> {
                        dateFormat.parse(it.date)
                    }.thenBy {
                        timeFormat.parse(it.time)
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                adapter.setUserGender(userGender)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load rides", Toast.LENGTH_SHORT).show()
            }
    }
}
