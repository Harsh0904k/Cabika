package com.hk.vcab

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.*
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyBookingActivity : BaseActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var bookingListView: ListView
    private lateinit var viewMatchedRidesButton: Button
    private lateinit var viewPastRidesButton: Button  // New button
    private lateinit var lottieProgress: LottieAnimationView
    private lateinit var animationOverlay: View
    private lateinit var contentLayout: LinearLayout
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_booking)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        bookingListView = findViewById(R.id.bookingListView)
        viewMatchedRidesButton = findViewById(R.id.viewMatchedRidesButton)
        viewPastRidesButton = findViewById(R.id.viewPastRidesButton) // Initialize new button
        lottieProgress = findViewById(R.id.lottieProgress)
        animationOverlay = findViewById(R.id.animationOverlay)
        contentLayout = findViewById(R.id.contentLayout)
        bottomNavigation = findViewById(R.id.bottomNavigationView)

        // Highlight current tab
        bottomNavigation.selectedItemId = R.id.activityFragment

        setupBottomNavigation()

        // Open Matched Rides
        viewMatchedRidesButton.setOnClickListener {
            startActivity(Intent(this, MatchedRidesActivity::class.java))
        }

        // Open Past Rides
        viewPastRidesButton.setOnClickListener {
            startActivity(Intent(this, PastRidesActivity::class.java))
        }

        showAnimation()
        loadBookings()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            bottomNavigation.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            when (item.itemId) {
                R.id.homeFragment -> {
                    startActivity(Intent(this, BookingActivity::class.java))
                    finish()
                    true
                }
                R.id.servicesFragment -> {
                    startActivity(Intent(this, MyRidesActivity::class.java))
                    finish()
                    true
                }
                R.id.activityFragment -> true // Already on this page
                R.id.accountFragment -> {
                    startActivity(Intent(this, AccountActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun showAnimation() {
        animationOverlay.visibility = View.VISIBLE
        lottieProgress.playAnimation()
        contentLayout.visibility = View.GONE
    }

    private fun hideAnimation() {
        lottieProgress.cancelAnimation()
        animationOverlay.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE
    }

    private fun loadBookings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            hideAnimation()
            return
        }

        db.collection("bookings")
            .whereEqualTo("uid", currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                val bookings = mutableListOf<String>()
                val bookingIds = mutableListOf<String>()

                for (doc in result) {
                    val joinedUids = doc.get("joinedUids") as? Map<*, *>
                    val driverId = doc.getString("driverId") ?: ""

// Skip showing the ride if it has joined users OR a driver has accepted
                    if ((joinedUids != null && joinedUids.isNotEmpty()) || driverId.isNotEmpty()) continue

                    val pickup = doc.getString("pickup") ?: ""
                    val drop = doc.getString("drop") ?: ""
                    val date = doc.getString("date") ?: ""
                    val time = doc.getString("time") ?: ""
                    val carType = doc.getString("carType") ?: "N/A"
                    val passengerCount = doc.getLong("passengerCount")?.toInt() ?: 0

                    bookings.add(
                        "From: $pickup\n" +
                                "To: $drop\n" +
                                "Date: $date\n" +
                                "Time: $time\n" +
                                "Car Type: $carType\n" +
                                "Passengers: $passengerCount"
                    )
                    bookingIds.add(doc.id)
                }

                if (bookings.isEmpty()) {
                    bookings.add("You don't have any bookings yet")
                    bookingListView.isEnabled = false
                } else {
                    bookingListView.isEnabled = true
                }

                bookingListView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, bookings)
                bookingListView.setOnItemClickListener { _, _, position, _ ->
                    if (bookingListView.isEnabled) {
                        val selectedBookingId = bookingIds[position]
                        val intent = Intent(this, EditBookingActivity::class.java)
                        intent.putExtra("bookingId", selectedBookingId)
                        startActivity(intent)
                    }
                }

                hideAnimation()
            }
            .addOnFailureListener {
                hideAnimation()
                Toast.makeText(this, "Failed to fetch bookings", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, BookingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.activityFragment
        showAnimation()
        loadBookings()
    }
}
