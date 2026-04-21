package com.hk.vcab

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DriverPastRidesActivity : BaseActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var pastRidesListView: ListView
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var tvNoPastRides: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_past_rides)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        pastRidesListView = findViewById(R.id.pastRidesListView)
        loadingAnimation = findViewById(R.id.loadingAnimationPastRides)
        tvNoPastRides = findViewById(R.id.tvNoPastRides)

        showLoadingAnimation()
        loadPastRides()
    }

    private fun showLoadingAnimation() {
        loadingAnimation.visibility = View.VISIBLE
        loadingAnimation.playAnimation()
        pastRidesListView.visibility = View.GONE
        tvNoPastRides.visibility = View.GONE
    }

    private fun hideLoadingAnimation() {
        loadingAnimation.cancelAnimation()
        loadingAnimation.visibility = View.GONE
    }

    private fun loadPastRides() {
        val currentUser = auth.currentUser ?: return
        val currentUserId = currentUser.uid

        db.collection("pastRides")
            .get()
            .addOnSuccessListener { result ->
                val pastRides = mutableListOf<String>()

                for (doc in result) {
                    val creatorId = doc.getString("uid") ?: ""
                    val joinedUids = doc.get("joinedUids") as? Map<String, Number> ?: emptyMap()
                    val driverId = doc.getString("driverId")

                    // Show ride if user is creator, joined user, or assigned driver
                    if (currentUserId != creatorId && !joinedUids.containsKey(currentUserId) && currentUserId != driverId) {
                        continue
                    }

                    val pickup = doc.getString("pickup") ?: "Unknown"
                    val drop = doc.getString("drop") ?: "Unknown"
                    val date = doc.getString("date") ?: "Unknown"
                    val time = doc.getString("time") ?: "Unknown"
                    val carType = doc.getString("carType") ?: "N/A"
                    val creatorName = doc.getString("name") ?: "Unknown"
                    val driverName = doc.getString("driverName") ?: "Not Assigned"
                    val driverCar = doc.getString("carName") ?: "N/A" // optional, if you store it
                    val creatorPassengerCount = doc.getLong("passengerCount")?.toInt() ?: 0
                    val joinedPassengerCount = joinedUids.values.sumOf { it.toInt() }
                    val totalPassengers = creatorPassengerCount + joinedPassengerCount

                    pastRides.add(
                        "From: $pickup\n" +
                                "To: $drop\n" +
                                "Date: $date\n" +
                                "Time: $time\n" +
                                "Passengers: $totalPassengers\n" +
                                "Driver: $driverName\n" +
                                "Car: $driverCar"
                    )
                }

                if (pastRides.isEmpty()) {
                    tvNoPastRides.visibility = View.VISIBLE
                    pastRidesListView.visibility = View.GONE
                } else {
                    tvNoPastRides.visibility = View.GONE
                    pastRidesListView.visibility = View.VISIBLE
                    pastRidesListView.adapter =
                        ArrayAdapter(this, android.R.layout.simple_list_item_1, pastRides)
                }

                hideLoadingAnimation()
            }
            .addOnFailureListener {
                hideLoadingAnimation()
                Toast.makeText(this, "Failed to fetch past rides", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        showLoadingAnimation()
        loadPastRides()
    }
}
