package com.hk.vcab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hk.vcab.adapters.PassengerAdapter
import com.hk.vcab.models.Passenger

class RideDetailsActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var recyclerPassengers: RecyclerView
    private lateinit var txtDriverName: TextView
    private lateinit var txtDriverPhone: TextView
    private lateinit var txtDriverCar: TextView
    private lateinit var btnCallDriver: Button
    private lateinit var btnWhatsAppDriver: Button
    private lateinit var scrollView: ScrollView
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_details)

        recyclerPassengers = findViewById(R.id.recyclerPassengers)
        txtDriverName = findViewById(R.id.txtDriverName)
        txtDriverPhone = findViewById(R.id.txtDriverPhone)
        txtDriverCar = findViewById(R.id.txtDriverCar)
        btnCallDriver = findViewById(R.id.btnCallDriver)
        btnWhatsAppDriver = findViewById(R.id.btnWhatsAppDriver)
        scrollView = findViewById(R.id.scrollView)
        loadingAnimation = findViewById(R.id.loadingAnimation)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        loadingAnimation.visibility = View.VISIBLE
        scrollView.visibility = View.GONE

        // 🔑 Check user role
        val prefs = getSharedPreferences("vcab_prefs", MODE_PRIVATE)
        val role = prefs.getString("user_role", "")

        if (role == "driver"|| role == "admin") {
            // Hide bottom nav for drivers
            bottomNavigationView.visibility = View.GONE
        } else {
            // Setup only for students
            setupBottomNavigation()
        }

        val rideId = intent.getStringExtra("rideId") ?: return finish().also {
            Toast.makeText(this, "Ride ID not found", Toast.LENGTH_SHORT).show()
        }

        loadRideDetails(rideId)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.servicesFragment

        bottomNavigationView.setOnItemSelectedListener { item ->
            bottomNavigationView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            when (item.itemId) {
                R.id.homeFragment -> {
                    startActivity(Intent(this, BookingActivity::class.java))
                    finish()
                    true
                }
                R.id.servicesFragment -> true // already here
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

    private fun loadRideDetails(rideId: String) {
        db.collection("bookings").document(rideId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener hideLoading()

                val driverName = doc.getString("driverName")
                val driverPhone = doc.getString("driverPhone")
                val driverCar = doc.getString("carName")

                // Check if driver is assigned
                if (driverName.isNullOrEmpty()) {
                    txtDriverName.text = "Waiting for driver"
                    txtDriverName.setTextColor(resources.getColor(R.color.red))
                    txtDriverPhone.visibility = View.GONE
                    txtDriverCar.visibility = View.GONE
                    btnCallDriver.visibility = View.GONE
                    btnWhatsAppDriver.visibility = View.GONE
                } else {
                    txtDriverName.text = "Name: $driverName"
                    txtDriverName.setTextColor(resources.getColor(R.color.black))
                    txtDriverPhone.text = "Contact No: ${driverPhone ?: "N/A"}"
                    txtDriverPhone.visibility = View.VISIBLE
                    txtDriverCar.text = "Car: ${driverCar ?: "N/A"}"
                    txtDriverCar.visibility = View.VISIBLE
                    btnCallDriver.visibility = View.VISIBLE
                    btnWhatsAppDriver.visibility = View.VISIBLE

                    // Call driver
                    btnCallDriver.setOnClickListener {
                        val intent = Intent(Intent.ACTION_DIAL)
                        intent.data = Uri.parse("tel:${driverPhone}")
                        startActivity(intent)
                    }

                    // WhatsApp driver
                    btnWhatsAppDriver.setOnClickListener {
                        val phoneNumber = driverPhone?.replace("+", "")?.replace(" ", "") ?: return@setOnClickListener
                        val url = "https://wa.me/$phoneNumber"
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(url)
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Passengers
                val passengerList = mutableListOf<Passenger>()
                val creatorUid = doc.getString("uid")
                val creatorPassengerCount = doc.getLong("passengerCount")?.toInt() ?: 1
                val joinedUidsMap = (doc.get("joinedUids") as? Map<*, *>)?.mapKeys { it.key as String }
                    ?.mapValues { (it.value as? Long)?.toInt() ?: 1 } ?: emptyMap()

                val allUsersWithCount = mutableListOf<Pair<String, Int>>()
                if (creatorUid != null) allUsersWithCount.add(Pair(creatorUid, creatorPassengerCount))
                allUsersWithCount.addAll(joinedUidsMap.entries.map { Pair(it.key, it.value) })

                if (allUsersWithCount.isEmpty()) return@addOnSuccessListener hideLoading()

                var processedCount = 0
                for ((uid, count) in allUsersWithCount) {
                    db.collection("users").document(uid)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("name") ?: "N/A"
                            val regNo = userDoc.getString("regNo") ?: "N/A"
                            val phone = userDoc.getString("phone") ?: "N/A"
                            val email = userDoc.getString("email") ?: "N/A"
                            val gender = userDoc.getString("gender") ?: "N/A"
                            val isMe = uid == currentUserId

                            passengerList.add(Passenger(uid, name, regNo, phone, email, gender, count, isMe))
                            processedCount++

                            if (processedCount == allUsersWithCount.size) {
                                setupPassengerRecycler(passengerList)
                                hideLoading()
                            }
                        }
                }
            }
    }

    private fun setupPassengerRecycler(passengers: List<Passenger>) {
        val adapter = PassengerAdapter(passengers)
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerPassengers.layoutManager = layoutManager
        recyclerPassengers.adapter = adapter

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerPassengers)

        recyclerPassengers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val childCount = rv.childCount
                val center = rv.width / 2f
                for (i in 0 until childCount) {
                    val child = rv.getChildAt(i)
                    val childCenter = (child.left + child.right) / 2f
                    val distance = Math.abs(center - childCenter)
                    val scale = 1 - 0.2f * (distance / center)
                    child.scaleX = scale
                    child.scaleY = scale
                    child.translationY = -20f * (1 - scale)
                }
            }
        })

        recyclerPassengers.setPadding(32, 0, 32, 0)
        recyclerPassengers.clipToPadding = false
    }

    private fun hideLoading() {
        loadingAnimation.visibility = View.GONE
        scrollView.visibility = View.VISIBLE
        loadingAnimation.cancelAnimation()
    }
}
