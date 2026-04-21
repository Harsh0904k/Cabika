    package com.hk.vcab

    import android.content.Intent
    import android.os.Bundle
    import android.view.HapticFeedbackConstants
    import android.view.View
    import android.widget.ArrayAdapter
    import android.widget.ListView
    import android.widget.TextView
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import com.airbnb.lottie.LottieAnimationView
    import com.google.android.material.bottomnavigation.BottomNavigationView
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore

    class PastRidesActivity : BaseActivity() {

        private lateinit var db: FirebaseFirestore
        private lateinit var auth: FirebaseAuth
        private lateinit var pastRidesListView: ListView
        private lateinit var loadingAnimation: LottieAnimationView
        private lateinit var tvNoPastRides: TextView
        private lateinit var bottomNavigation: BottomNavigationView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_past_rides)

            db = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()

            pastRidesListView = findViewById(R.id.pastRidesListView)
            loadingAnimation = findViewById(R.id.loadingAnimationPastRides)
            tvNoPastRides = findViewById(R.id.tvNoPastRides)
            bottomNavigation = findViewById(R.id.bottomNavigationView)

            bottomNavigation.selectedItemId = R.id.activityFragment
            setupBottomNavigation()

            showLoadingAnimation()
            loadPastRides()
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
                    R.id.activityFragment -> true
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
            pastRidesListView.visibility = View.GONE
            tvNoPastRides.visibility = View.GONE
        }

        private fun hideLoadingAnimation() {
            loadingAnimation.cancelAnimation()
            loadingAnimation.visibility = View.GONE
        }

        private fun loadPastRides() {
            val currentUser = auth.currentUser ?: return

            db.collection("pastRides")
                .get()
                .addOnSuccessListener { result ->
                    val pastRides = mutableListOf<String>()
                    val currentUserId = currentUser.uid

                    for (doc in result) {
                        val creatorId = doc.getString("uid") ?: ""
                        val joinedUids = doc.get("joinedUids") as? Map<String, Long> ?: emptyMap()

                        // Include ride if current user is creator OR joined user
                        if (creatorId != currentUserId && !joinedUids.containsKey(currentUserId)) {
                            continue
                        }

                        val pickup = doc.getString("pickup") ?: "Unknown"
                        val drop = doc.getString("drop") ?: "Unknown"
                        val date = doc.getString("date") ?: "Unknown"
                        val time = doc.getString("time") ?: "Unknown"
                        val carType = doc.getString("carType") ?: "N/A"
                        val creatorPassengerCount = doc.getLong("passengerCount")?.toInt() ?: 0
                        val creatorName = doc.getString("name") ?: "Unknown"

                        // Sum of creator + joined users
                        val joinedPassengerCount = joinedUids.values.sumOf { it.toInt() }
                        val totalPassengers = creatorPassengerCount + joinedPassengerCount

                        pastRides.add(
                            "From: $pickup\n" +
                                    "To: $drop\n" +
                                    "Date: $date\n" +
                                    "Time: $time\n" +
                                    "Car Type: $carType\n" +
                                    "Passengers: $totalPassengers\n" +
                                    "Creator: $creatorName"
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
            bottomNavigation.selectedItemId = R.id.activityFragment
            showLoadingAnimation()
            loadPastRides()
        }

        override fun onBackPressed() {
            super.onBackPressed()
            val intent = Intent(this, BookingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
