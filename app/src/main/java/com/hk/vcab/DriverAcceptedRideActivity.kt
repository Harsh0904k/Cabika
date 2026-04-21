package com.hk.vcab

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hk.vcab.adapters.DriverAcceptedRideAdapter
import com.hk.vcab.models.Passenger
import com.hk.vcab.models.Ride
import java.text.SimpleDateFormat
import java.util.*

class DriverAcceptedRideActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val currentDriverId = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var btnAddDriverBooking: Button

    private val rideList = mutableListOf<Ride>()
    private val passengersMap = mutableMapOf<String, List<Passenger>>() // rideId -> passengers

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_accepted_ride)

        recyclerView = findViewById(R.id.recyclerAcceptedRides)
        loadingAnimation = findViewById(R.id.loadingAnimation)
       0

        recyclerView.layoutManager = LinearLayoutManager(this)


        loadAcceptedRides()
    }

    private fun loadAcceptedRides() {
        if (currentDriverId == null) return

        loadingAnimation.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        db.collection("bookings")
            .whereEqualTo("driverId", currentDriverId)
            .get()
            .addOnSuccessListener { docs ->
                rideList.clear()
                passengersMap.clear()

                if (docs.isEmpty) {
                    hideLoading()
                    Toast.makeText(this, "No accepted rides", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (doc in docs) {
                    val ride = Ride(
                        id = doc.id,
                        pickup = doc.getString("pickup") ?: "",
                        drop = doc.getString("drop") ?: "",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        creatorId = doc.getString("uid") ?: "",
                        creatorName = doc.getString("creatorName") ?: "",
                        creatorPassengerCount = (doc.getLong("passengerCount")?.toInt() ?: 1),
                        joinedUids = (doc.get("joinedUids") as? Map<String, Long>)?.mapValues { it.value.toInt() }
                            ?: emptyMap()
                    )
                    rideList.add(ride)
                    loadPassengersForRide(ride)
                }
            }
            .addOnFailureListener {
                hideLoading()
                Toast.makeText(this, "Failed to load rides", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPassengersForRide(ride: Ride) {
        val passengers = mutableListOf<Passenger>()

        db.collection("users").document(ride.creatorId).get()
            .addOnSuccessListener { userDoc ->
                val creator = Passenger(
                    uid = ride.creatorId,
                    name = userDoc.getString("name") ?: "",
                    regNo = userDoc.getString("regNo") ?: "",
                    phone = userDoc.getString("phone") ?: "",
                    email = userDoc.getString("email") ?: "",
                    gender = userDoc.getString("gender") ?: "",
                    passengerCount = ride.creatorPassengerCount,
                    isMe = ride.creatorId == currentDriverId
                )
                passengers.add(creator)

                val joinedMap = ride.joinedUids
                if (joinedMap.isEmpty()) {
                    passengersMap[ride.id] = passengers
                    updateRecycler()
                } else {
                    var processed = 0
                    for ((uid, count) in joinedMap) {
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { joinedDoc ->
                                val p = Passenger(
                                    uid = uid,
                                    name = joinedDoc.getString("name") ?: "",
                                    regNo = joinedDoc.getString("regNo") ?: "",
                                    phone = joinedDoc.getString("phone") ?: "",
                                    email = joinedDoc.getString("email") ?: "",
                                    gender = joinedDoc.getString("gender") ?: "",
                                    passengerCount = count,
                                    isMe = uid == currentDriverId
                                )
                                passengers.add(p)
                                processed++
                                if (processed == joinedMap.size) {
                                    passengersMap[ride.id] = passengers
                                    updateRecycler()
                                }
                            }
                    }
                }
            }
    }

    private fun updateRecycler() {
        // ✅ Sort rides by date & time before updating adapter
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        rideList.sortBy { ride ->
            try {
                dateFormat.parse("${ride.date} ${ride.time}")
            } catch (e: Exception) {
                Date(0)
            }
        }

        recyclerView.adapter = DriverAcceptedRideAdapter(rideList, passengersMap) { ride ->
            leaveRide(ride)
        }
        hideLoading()
    }

    private fun leaveRide(ride: Ride) {
        val bookingRef = db.collection("bookings").document(ride.id)
        val updates = mapOf(
            "driverId" to null,
            "driverName" to "",
            "driverPhone" to "",
            "carName" to ""
        )

        bookingRef.update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "You have left the ride", Toast.LENGTH_SHORT).show()
                rideList.remove(ride)
                passengersMap.remove(ride.id)
                updateRecycler()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to leave ride: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hideLoading() {
        loadingAnimation.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}
