package com.hk.vcab

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hk.vcab.adapters.OngoingRidesAdapter
import com.hk.vcab.models.OngoingRide
import java.util.Locale

class BookingActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvGreeting: TextView
    private lateinit var llWhereTo: LinearLayout
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var recyclerOngoing: RecyclerView

    private val rides = mutableListOf<OngoingRide>()
    private lateinit var adapter: OngoingRidesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvGreeting = findViewById(R.id.tvGreeting)
        llWhereTo = findViewById(R.id.llWhereTo)
        bottomNavigation = findViewById(R.id.bottomNavigationView)
        recyclerOngoing = findViewById(R.id.recyclerOngoingRides)

        val btnViewAllRides = findViewById<Button>(R.id.btnViewAllRides)
        btnViewAllRides.setOnClickListener {
            val intent = Intent(this, AllRidesActivity::class.java)
            startActivity(intent)
        }

        // 🌟 Horizontal stacked card layout
        adapter = OngoingRidesAdapter(rides)
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerOngoing.layoutManager = layoutManager
        recyclerOngoing.adapter = adapter

        // 🔄 Snap cards to center
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerOngoing)

        // 🎬 Smooth scale animation while scrolling
        recyclerOngoing.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val childCount = recyclerView.childCount
                val recyclerWidth = recyclerView.width
                val center = recyclerWidth / 2f

                for (i in 0 until childCount) {
                    val child = recyclerView.getChildAt(i)
                    val childCenter = (child.left + child.right) / 2f
                    val distance = Math.abs(center - childCenter)
                    val scale = 1 - 0.2f * (distance / center) // max scale difference 0.2
                    child.scaleX = scale
                    child.scaleY = scale
                    child.translationY = -20f * (1 - scale)  // Lift top card slightly
                }
            }
        })

        // Optional: Padding to center edge items
        recyclerOngoing.setPadding(32, 0, 32, 0)
        recyclerOngoing.clipToPadding = false

        setGreeting()
        loadUserOngoingRides()

        llWhereTo.setOnClickListener {
            startActivity(Intent(this, BookRideActivity::class.java))
        }

        bottomNavigation.selectedItemId = R.id.homeFragment
        bottomNavigation.setOnItemSelectedListener { item ->
            bottomNavigation.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            when (item.itemId) {
                R.id.homeFragment -> true
                R.id.servicesFragment -> {
                    startActivity(Intent(this, MyRidesActivity::class.java))
                    finish()
                    true
                }
                R.id.activityFragment -> {
                    startActivity(Intent(this, MyBookingActivity::class.java))
                    finish()
                    true
                }
                R.id.accountFragment -> {
                    startActivity(Intent(this, AccountActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setGreeting() {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val cachedName = sharedPref.getString("name", null)

        val cachedGender = sharedPref.getString("gender", null)

        if (cachedGender == null) {
            auth.currentUser?.let { user ->
                db.collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        val gender = doc.getString("gender") ?: "unknown"
                        with(sharedPref.edit()) {
                            putString("gender", gender.lowercase(Locale.ROOT)) // caching gender
                            apply()
                        }
                    }
            }
        }

        if (cachedName != null) {
            tvGreeting.text = "Hi, $cachedName! 👋 Ready for your next ride?"
        } else {
            auth.currentUser?.let { user ->
                db.collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("name") ?: "User"
                        tvGreeting.text = "Hi, $name! 👋 Ready for your next ride?"
                        with(sharedPref.edit()) {
                            putString("name", name)
                            apply()
                        }
                    }
                    .addOnFailureListener {
                        tvGreeting.text = "Hi! 👋 Ready for your next ride?"
                    }
            } ?: run {
                tvGreeting.text = "Hi! 👋 Ready for your next ride?"
            }
        }
    }

    // ✅ Updated: show rides for both creators and joined users
    private fun loadUserOngoingRides() {
        val currentUser = auth.currentUser ?: return
        rides.clear()

        db.collection("bookings")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val rideId = doc.id
                    val creatorId = doc.getString("uid")
                    val joinedMap = doc["joinedUids"] as? Map<String, Long> ?: emptyMap()
                    val joinedCount = joinedMap.values.sumOf { it.toInt() }

                    val creatorPassengerCount = (doc.getLong("passengerCount") ?: 1L).toInt()
                    val carType = doc.getString("carType") ?: "5-seater"
                    val maxSeats = if (carType == "7-seater") 7 else 5
                    val driverAssigned = !doc.getString("driverId").isNullOrEmpty()

                    if (creatorId == currentUser.uid || joinedMap.containsKey(currentUser.uid)) {
                        val ride = OngoingRide(
                            rideId = doc.getString("rideId") ?: rideId,
                            pickup = doc.getString("pickup") ?: "",
                            drop = doc.getString("drop") ?: "",
                            date = doc.getString("date") ?: "",
                            time = doc.getString("time") ?: "",
                            joinedUsers = joinedCount,
                            creatorPassengerCount = creatorPassengerCount,
                            maxSeats = maxSeats,
                            driverAssigned = driverAssigned
                        )
                        rides.add(ride)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("BookingActivity", "Failed to load rides", e)
            }
    }
}
