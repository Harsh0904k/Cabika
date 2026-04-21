package com.hk.vcab

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MatchedRidesActivity : BaseActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var matchedListView: ListView
    private lateinit var lottieLoading: LottieAnimationView

    private val handler = Handler(Looper.getMainLooper())
    private val retryDelayMillis = 1500L
    private val maxRetries = 10
    private var retryCount = 0
    private var rideId: String? = null
    private var userGender: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matched_rides)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        matchedListView = findViewById(R.id.matchedListView)
        lottieLoading = findViewById(R.id.lottieCarLoading)

        val alternateRidesBtn = findViewById<Button>(R.id.btnAlternateRides)
        alternateRidesBtn.setOnClickListener {
            startActivity(Intent(this, AllRidesActivity::class.java))
        }

        lottieLoading.visibility = View.VISIBLE
        matchedListView.visibility = View.GONE

        rideId = intent.getStringExtra("rideId")
        val userId = auth.currentUser?.uid ?: return

        // Load user gender first
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                userGender = doc.getString("gender")?.lowercase(Locale.ROOT)
                if (rideId != null) waitForBookingAndLoad(rideId!!, userId)
                else loadUserBooking(userId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user profile", Toast.LENGTH_SHORT).show()
                lottieLoading.visibility = View.GONE
            }
    }

    private fun loadUserBooking(userId: String) {
        db.collection("bookings")
            .whereEqualTo("uid", userId)
            .get()
            .addOnSuccessListener { userBookings ->
                if (userBookings.isEmpty) {
                    lottieLoading.visibility = View.GONE
                    Toast.makeText(this, "You have no bookings", Toast.LENGTH_SHORT).show()
                } else {
                    val inflater = LayoutInflater.from(this)
                    handleUserBooking(userBookings.first(), userId, inflater)
                }
            }
            .addOnFailureListener {
                lottieLoading.visibility = View.GONE
                matchedListView.visibility = View.VISIBLE
                Toast.makeText(this, "Failed to load bookings", Toast.LENGTH_SHORT).show()
            }
    }

    private fun waitForBookingAndLoad(rideId: String, userId: String) {
        db.collection("bookings").document(rideId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val inflater = LayoutInflater.from(this)
                    handleUserBooking(document, userId, inflater)
                } else {
                    retryCount++
                    if (retryCount <= maxRetries) {
                        handler.postDelayed({ waitForBookingAndLoad(rideId, userId) }, retryDelayMillis)
                    } else {
                        lottieLoading.visibility = View.GONE
                        matchedListView.visibility = View.VISIBLE
                        Toast.makeText(this, "Booking not found. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener {
                lottieLoading.visibility = View.GONE
                matchedListView.visibility = View.VISIBLE
                Toast.makeText(this, "Error loading booking", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleUserBooking(userBooking: DocumentSnapshot, userId: String, inflater: LayoutInflater) {
        val pickup = userBooking.getString("pickup")
        val drop = userBooking.getString("drop")
        val date = userBooking.getString("date")
        val time = userBooking.getString("time")
        val carType = userBooking.getString("carType")
        val myPassengerCount = userBooking.getLong("passengerCount")?.toInt() ?: 1

        if (pickup == null || drop == null || date == null || time == null || carType == null) {
            Toast.makeText(this, "Your booking data is incomplete", Toast.LENGTH_SHORT).show()
            lottieLoading.visibility = View.GONE
            matchedListView.visibility = View.VISIBLE
            return
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val userTime: Date = timeFormat.parse(time) ?: return
        val calendar = Calendar.getInstance()
        calendar.time = userTime
        calendar.add(Calendar.MINUTE, -180)
        val lowerBound = calendar.time
        calendar.time = userTime
        calendar.add(Calendar.MINUTE, 180)
        val upperBound = calendar.time

        db.collection("bookings")
            .whereEqualTo("pickup", pickup)
            .whereEqualTo("drop", drop)
            .whereEqualTo("date", date)
            .whereEqualTo("carType", carType)
            .get()
            .addOnSuccessListener { results ->
                val matchedRidesView = mutableListOf<View>()

                for (doc in results) {
                    val rideCreatorId = doc.getString("uid") ?: continue
                    val joinedUids = doc.get("joinedUids") as? Map<*, *> ?: emptyMap<String, Long>()

                    // Skip rides already joined or created by this user
                    if (userId == rideCreatorId || joinedUids.containsKey(userId)) continue

                    val matchedTimeStr = doc.getString("time") ?: continue
                    val matchedName = doc.getString("name") ?: "Unknown"
                    val matchedPassengerCount = doc.getLong("passengerCount")?.toInt() ?: 1
                    val matchedRideId = doc.id
                    val girlCabOnly = doc.getBoolean("girlCabOnly") ?: false

                    try {
                        val matchedTime = timeFormat.parse(matchedTimeStr) ?: continue
                        if (matchedTime !in lowerBound..upperBound) continue

                        var totalJoined = 0
                        for ((_, count) in joinedUids) {
                            totalJoined += (count as? Long)?.toInt() ?: 1
                        }

                        val capacity = if (carType == "5-seater") 5 else 7
                        if (totalJoined + matchedPassengerCount + myPassengerCount <= capacity) {
                            val matchedRideView = inflater.inflate(R.layout.matched_ride_item, null)
                            val rideDetailsTextView = matchedRideView.findViewById<TextView>(R.id.rideDetails)
                            val passengerCountText = matchedRideView.findViewById<TextView>(R.id.passengerCountText)
                            val joinRideButton = matchedRideView.findViewById<Button>(R.id.btnJoinRide)

                            val totalPassengers = matchedPassengerCount + totalJoined
                            passengerCountText.text = "Passengers: $totalPassengers/$capacity"

                            if (girlCabOnly) {
                                val pinkLabel = "<font color='#E91E63'>Girl Only Cab 🎀</font>"
                                rideDetailsTextView.text = android.text.Html.fromHtml(
                                    "Name: $matchedName<br>From: $pickup<br>To: $drop<br>Date: $date<br>Time: $matchedTimeStr<br>$pinkLabel",
                                    android.text.Html.FROM_HTML_MODE_LEGACY
                                )
                            } else {
                                rideDetailsTextView.text =
                                    "Name: $matchedName\nFrom: $pickup\nTo: $drop\nDate: $date\nTime: $matchedTimeStr"
                            }

                            if (girlCabOnly && userGender != "female" && userGender != "f") {
                                joinRideButton.isEnabled = false
                                joinRideButton.alpha = 0.5f
                                joinRideButton.text = "Not for you"
                            } else {
                                joinRideButton.isEnabled = true
                                joinRideButton.alpha = 1f

                                joinRideButton.setOnClickListener {
                                    val dialog = AlertDialog.Builder(this)
                                        .setTitle("Confirm Join")
                                        .setMessage("Join only if your ride is confirmed. Fake or false joins may lead to suspension. Proceed?")
                                        .setCancelable(false)
                                        .setPositiveButton("Yes", null)
                                        .setNegativeButton("No", null)
                                        .create()

                                    dialog.setOnShowListener {
                                        val yesBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                        val noBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                        yesBtn.setTextColor(getColor(R.color.green_700))
                                        noBtn.setTextColor(getColor(R.color.red_700))

                                        yesBtn.setOnClickListener {
                                            dialog.dismiss()
                                            joinRide(matchedRideId, userId, myPassengerCount, userBooking.id)
                                        }
                                        noBtn.setOnClickListener { dialog.dismiss() }
                                    }

                                    dialog.show()
                                }
                            }

                            matchedRidesView.add(matchedRideView)
                        }
                    } catch (_: Exception) {
                        continue
                    }
                }

                if (matchedRidesView.isEmpty()) {
                    val emptyView = TextView(this)
                    emptyView.text = "No matched rides found."
                    emptyView.setPadding(16, 16, 16, 16)
                    emptyView.textSize = 18f
                    emptyView.gravity = Gravity.CENTER
                    matchedListView.adapter = object : BaseAdapter() {
                        override fun getCount() = 1
                        override fun getItem(position: Int) = emptyView
                        override fun getItemId(position: Int) = 0L
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup?) = emptyView
                    }
                } else {
                    matchedListView.adapter = object : BaseAdapter() {
                        override fun getCount() = matchedRidesView.size
                        override fun getItem(position: Int) = matchedRidesView[position]
                        override fun getItemId(position: Int) = position.toLong()
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                            val container = LinearLayout(this@MatchedRidesActivity)
                            container.orientation = LinearLayout.VERTICAL
                            container.setPadding(0, 0, 0, 16)
                            container.addView(matchedRidesView[position])
                            return container
                        }
                    }
                }

                lottieLoading.visibility = View.GONE
                matchedListView.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                lottieLoading.visibility = View.GONE
                matchedListView.visibility = View.VISIBLE
                Toast.makeText(this, "Error loading matched rides", Toast.LENGTH_SHORT).show()
            }
    }

    // ✅ Fixed: also delete user's own booking when joining another ride
    private fun joinRide(matchedRideId: String, userId: String, myPassengerCount: Int, myBookingId: String) {
        lottieLoading.visibility = View.VISIBLE
        db.collection("myRides")
            .whereEqualTo("rideId", matchedRideId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { existingDocs ->
                if (existingDocs.isEmpty) {
                    val userName = auth.currentUser?.displayName ?: "Unknown"
                    val userRide = hashMapOf(
                        "rideId" to matchedRideId,
                        "userId" to userId,
                        "userName" to userName,
                        "passengerCount" to myPassengerCount
                    )

                    db.collection("myRides").add(userRide)
                        .addOnSuccessListener {
                            val rideRef = db.collection("bookings").document(matchedRideId)
                            rideRef.get().addOnSuccessListener { rideDoc ->
                                val updatedMap =
                                    (rideDoc.get("joinedUids") as? MutableMap<String, Long>)?.toMutableMap()
                                        ?: mutableMapOf()
                                updatedMap[userId] = myPassengerCount.toLong()
                                rideRef.update("joinedUids", updatedMap)

                                // ✅ Delete user's original booking
                                db.collection("bookings").document(myBookingId).delete()

                                lottieLoading.visibility = View.GONE
                                Toast.makeText(this, "Joined Ride", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MyRidesActivity::class.java))
                                finish()
                            }
                        }
                } else {
                    lottieLoading.visibility = View.GONE
                    Toast.makeText(this, "You already joined this ride", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, BookingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }
}
