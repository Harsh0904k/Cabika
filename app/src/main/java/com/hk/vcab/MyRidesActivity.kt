package com.hk.vcab

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MyRidesActivity : BaseActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var myRidesListView: ListView
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var tvNoRides: TextView

    private val ridesList = mutableListOf<Pair<String, CharSequence>>() // Changed to CharSequence for Html
    private val userRideIds = mutableSetOf<String>()
    private val greenHighlightRides = mutableSetOf<String>()
    private val leftRideIds = mutableSetOf<String>()
    private val rejoinedRideIds = mutableSetOf<String>()

    private var joinedRidesLoaded = false
    private var createdRidesLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_rides)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        myRidesListView = findViewById(R.id.myRidesListView)
        loadingAnimation = findViewById(R.id.loadingAnimation)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        tvNoRides = findViewById(R.id.tvNoRides)

        bottomNavigationView.selectedItemId = R.id.servicesFragment
        setupBottomNavigation()
        loadRideHistoryThenRides()
    }

    override fun onResume() {
        super.onResume()
        bottomNavigationView.selectedItemId = R.id.servicesFragment
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
                R.id.servicesFragment -> true
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

    private fun showLoadingAnimation() {
        loadingAnimation.visibility = View.VISIBLE
        loadingAnimation.playAnimation()
    }

    private fun hideLoadingAnimation() {
        loadingAnimation.cancelAnimation()
        loadingAnimation.visibility = View.GONE
    }

    private fun loadRideHistoryThenRides() {
        showLoadingAnimation()
        val currentUser = auth.currentUser ?: return
        leftRideIds.clear()

        db.collection("rideHistory")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) doc.getString("rideId")?.let { leftRideIds.add(it) }
                joinedRidesLoaded = false
                createdRidesLoaded = false
                userRideIds.clear()
                greenHighlightRides.clear()
                rejoinedRideIds.clear()

                loadUserJoinedRides()
                loadUserCreatedRides()
            }
    }

    private fun loadUserJoinedRides() {
        val currentUser = auth.currentUser ?: return

        db.collection("myRides")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val rideId = doc.getString("rideId")
                    if (rideId != null) {
                        rejoinedRideIds.add(rideId)
                        userRideIds.add(rideId)
                    }
                }
                leftRideIds.removeAll(rejoinedRideIds)
                joinedRidesLoaded = true
                maybeLoadRidesDetails()
            }
    }

    private fun loadUserCreatedRides() {
        val currentUser = auth.currentUser ?: return

        db.collection("bookings")
            .whereEqualTo("uid", currentUser.uid)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val rideId = doc.id
                    if (leftRideIds.contains(rideId)) continue
                    val joinedUids = doc.get("joinedUids") as? Map<String, Long> ?: emptyMap()
                    val driverId = doc.getString("driverId") ?: ""
                    if (joinedUids.isNotEmpty() || driverId.isNotEmpty()) {
                        userRideIds.add(rideId)
                    }
                }
                createdRidesLoaded = true
                maybeLoadRidesDetails()
            }
    }

    private fun maybeLoadRidesDetails() {
        if (joinedRidesLoaded && createdRidesLoaded) {
            loadRidesDetails()
        }
    }

    private fun loadRidesDetails() {
        ridesList.clear()
        if (userRideIds.isEmpty()) {
            setupAdapter()
            return
        }

        var processedCount = 0
        val currentUser = auth.currentUser ?: return
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val sixHoursMillis = 6 * 60 * 60 * 1000
        val now = System.currentTimeMillis()

        for (rideId in userRideIds) {
            db.collection("bookings").document(rideId).get()
                .addOnSuccessListener { doc ->
                    if (!doc.exists()) {
                        processedCount++
                        if (processedCount == userRideIds.size) setupAdapter()
                        return@addOnSuccessListener
                    }

                    val date = doc.getString("date") ?: "Unknown"
                    val time = doc.getString("time") ?: "Unknown"

                    val rideMillis = try {
                        val rideDate = sdf.parse("$date $time")
                        rideDate?.time ?: 0L
                    } catch (e: Exception) { 0L }

                    if (rideMillis + sixHoursMillis <= now) {
                        val pastRideData = doc.data?.toMutableMap() ?: mutableMapOf()
                        pastRideData["userId"] = currentUser.uid
                        db.collection("pastRides").add(pastRideData)
                        doc.reference.delete()
                        processedCount++
                        if (processedCount == userRideIds.size) setupAdapter()
                        return@addOnSuccessListener
                    }

                    val pickup = doc.getString("pickup") ?: "Unknown"
                    val drop = doc.getString("drop") ?: "Unknown"
                    val creatorId = doc.getString("uid") ?: ""
                    val driverId = doc.getString("driverId") ?: ""
                    val joinedUids = doc.get("joinedUids") as? Map<String, Long> ?: emptyMap()
                    val carType = doc.getString("carType") ?: "5-seater"
                    val creatorPassengerCount = doc.getLong("passengerCount") ?: 1L
                    val girlCabOnly = doc.getBoolean("girlCabOnly") ?: false

                    if (driverId.isNotEmpty()) greenHighlightRides.add(rideId)

                    val maxSeats = if (carType == "7-seater") 7 else 5
                    val joinedSeats = joinedUids.values.sum()
                    val totalPassengers = creatorPassengerCount + joinedSeats

                    db.collection("users").document(creatorId).get()
                        .addOnSuccessListener { creatorDoc ->
                            val creatorName = creatorDoc.getString("name") ?: "Unknown"
                            val isCreator = creatorId == currentUser.uid
                            val creatorNote = if (isCreator) "Created by me<br>" else ""

                            val rideText: CharSequence = if (girlCabOnly) {
                                Html.fromHtml(
                                    buildString {
                                        append(creatorNote)

                                        append("From: $pickup<br>")
                                        append("To: $drop<br>")
                                        append("Date: $date<br>")
                                        append("Time: $time<br>")
                                        append("Passengers: $totalPassengers/$maxSeats<br>")
                                        append("Creator: $creatorName<br>")
                                        append("<font color='#E91E63'>Girl Only Cab</font>")

                                    },
                                    Html.FROM_HTML_MODE_LEGACY
                                )
                            } else {
                                Html.fromHtml(
                                    buildString {
                                        append(creatorNote)
                                        append("From: $pickup<br>")
                                        append("To: $drop<br>")
                                        append("Date: $date<br>")
                                        append("Time: $time<br>")
                                        append("Passengers: $totalPassengers/$maxSeats<br>")
                                        append("Creator: $creatorName")
                                    },
                                    Html.FROM_HTML_MODE_LEGACY
                                )
                            }

                            addRideToList(rideId, rideText)
                            processedCount++
                            if (processedCount == userRideIds.size) setupAdapter()
                        }
                        .addOnFailureListener {
                            processedCount++
                            if (processedCount == userRideIds.size) setupAdapter()
                        }
                }
        }
    }

    private fun addRideToList(rideId: String, rideText: CharSequence) {
        ridesList.add(Pair(rideId, rideText))
    }

    private fun setupAdapter() {
        val adapter = object : BaseAdapter() {
            override fun getCount(): Int = ridesList.size
            override fun getItem(position: Int): Any = ridesList[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val inflater = LayoutInflater.from(this@MyRidesActivity)
                val view = inflater.inflate(R.layout.my_ride_item, parent, false)

                val rideTextView = view.findViewById<TextView>(R.id.myRideDetails)
                val leaveBtn = view.findViewById<Button>(R.id.btnLeaveRide)
                val viewDetailsBtn = view.findViewById<Button>(R.id.viewDetailsButton)
                val driverBadge = view.findViewById<TextView>(R.id.tvDriverBadge)

                val (rideId, rideInfo) = ridesList[position]
                rideTextView.text = rideInfo

                driverBadge.visibility = if (greenHighlightRides.contains(rideId)) View.VISIBLE else View.GONE

                viewDetailsBtn.setOnClickListener {
                    val intent = Intent(this@MyRidesActivity, RideDetailsActivity::class.java)
                    intent.putExtra("rideId", rideId)
                    startActivity(intent)
                }

                leaveBtn.setOnClickListener {
                    val userId = auth.currentUser?.uid ?: return@setOnClickListener
                    showLeaveConfirmationDialog(userId, rideId)
                }

                return view
            }
        }

        myRidesListView.adapter = adapter
        hideLoadingAnimation()

        if (ridesList.isEmpty()) {
            tvNoRides.visibility = View.VISIBLE
            myRidesListView.visibility = View.GONE
        } else {
            tvNoRides.visibility = View.GONE
            myRidesListView.visibility = View.VISIBLE
        }
    }

    private fun showLeaveConfirmationDialog(currentUserId: String, rideId: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Leave Ride?")
        builder.setMessage(
            "Leaving this ride may cause inconvenience to other passengers. " +
                    "Doing this frequently may block your access to the app. Are you sure you want to leave?"
        )
        builder.setPositiveButton("Yes") { dialog, _ ->
            leaveRide(currentUserId, rideId)
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(true)

        val dialog = builder.create()
        dialog.show()

        // Customize button colors
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            .setTextColor(resources.getColor(R.color.green, theme)) // Use your theme green
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(resources.getColor(R.color.red, theme)) // Use your theme red
    }
    private fun leaveRide(currentUserId: String, rideId: String) {
        val bookingRef = db.collection("bookings").document(rideId)

        bookingRef.get().addOnSuccessListener { bookingDoc ->
            if (!bookingDoc.exists()) return@addOnSuccessListener

            val creatorId = bookingDoc.getString("uid") ?: return@addOnSuccessListener
            val joinedUids = (bookingDoc.get("joinedUids") as? MutableMap<String, Long>) ?: mutableMapOf()
            val isCreator = creatorId == currentUserId

            // Remove from myRides
            db.collection("myRides")
                .whereEqualTo("rideId", rideId)
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener { myRidesSnapshot ->
                    for (doc in myRidesSnapshot.documents) {
                        val leavingPassengerCount = doc.getLong("passengerCount")?.toInt() ?: 1
                        db.collection("myRides").document(doc.id).delete()

                        // Add to rideHistory
                        val historyData = hashMapOf(
                            "rideId" to rideId,
                            "userId" to currentUserId,
                            "passengerCount" to leavingPassengerCount,
                            "timestamp" to System.currentTimeMillis()
                        )
                        db.collection("rideHistory").add(historyData)
                    }

                    if (!isCreator) {
                        if (joinedUids.containsKey(currentUserId)) {
                            joinedUids.remove(currentUserId)
                            bookingRef.update("joinedUids", joinedUids).addOnSuccessListener {
                                Toast.makeText(this@MyRidesActivity, "Left the ride", Toast.LENGTH_SHORT).show()
                                loadRideHistoryThenRides()
                            }
                        }
                        return@addOnSuccessListener
                    }

                    // Creator leaving logic
                    if (joinedUids.isEmpty()) {
                        bookingRef.delete().addOnSuccessListener {
                            Toast.makeText(this@MyRidesActivity, "Ride deleted", Toast.LENGTH_SHORT).show()
                            loadRideHistoryThenRides()
                        }
                    } else {
                        val newCreatorId = joinedUids.keys.first()
                        val newCreatorPassengerCount = joinedUids[newCreatorId] ?: 1
                        joinedUids.remove(newCreatorId)

                        db.collection("users").document(newCreatorId).get().addOnSuccessListener { newUserDoc ->
                            val newCreatorName = newUserDoc.getString("name") ?: "Unknown"
                            bookingRef.update(
                                mapOf(
                                    "uid" to newCreatorId,
                                    "name" to newCreatorName,
                                    "joinedUids" to joinedUids,
                                    "passengerCount" to newCreatorPassengerCount
                                )
                            ).addOnSuccessListener {
                                db.collection("myRides")
                                    .whereEqualTo("rideId", rideId)
                                    .whereEqualTo("userId", newCreatorId)
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        if (snapshot.isEmpty) {
                                            db.collection("myRides").add(
                                                hashMapOf(
                                                    "rideId" to rideId,
                                                    "userId" to newCreatorId,
                                                    "passengerCount" to newCreatorPassengerCount
                                                )
                                            )
                                        }
                                        Toast.makeText(this@MyRidesActivity, "Left ride. New creator assigned.", Toast.LENGTH_SHORT).show()
                                        loadRideHistoryThenRides()
                                    }
                            }
                        }
                    }
                }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, BookingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
