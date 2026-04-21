package com.hk.vcab

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class BookRideActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var bookButton: Button
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var tvGreeting: TextView
    private lateinit var switchDriverRequired: Switch
    private lateinit var switchGirlCab: Switch
    private lateinit var tvGirlCabStatus: TextView
    private lateinit var girlCabSwitchListener: CompoundButton.OnCheckedChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_ride)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvGreeting = findViewById(R.id.tvGreeting)
        val pickupSpinner = findViewById<Spinner>(R.id.pickupSpinner)
        val dropSpinner = findViewById<Spinner>(R.id.dropSpinner)
        val dateEditText = findViewById<EditText>(R.id.dateEditText)
        val timeEditText = findViewById<EditText>(R.id.timeEditText)
        val carTypeSpinner = findViewById<Spinner>(R.id.spinnerCarType)
        val passengerCountEditText = findViewById<EditText>(R.id.editPassengerCount)
        switchDriverRequired = findViewById(R.id.switchDriverRequired)
        switchGirlCab = findViewById(R.id.switchGirlCab)
        tvGirlCabStatus = findViewById(R.id.tvGirlCabStatus)
        bookButton = findViewById(R.id.bookButton)
        bottomNavigation = findViewById(R.id.bottomNavigationView)

        // Greeting
        setGreeting()

        // Disable Girl Cab by default
        switchGirlCab.isEnabled = false
        switchGirlCab.alpha = 0.5f
        tvGirlCabStatus.alpha = 0.5f

        // Bottom navigation setup
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    startActivity(Intent(this, BookingActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    finish()
                    true
                }
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

        // Spinners setup
        val locations = listOf(
            "Vit Bhopal", "Bhopal Railway Station", "Bhopal Airport", "Indore",
            "Bhopal Bus Stand", "Rani Kamlapati", "Sant Hirdaram Nagar", "Sehore", "Dewas", "Ujjain"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        pickupSpinner.adapter = adapter
        dropSpinner.adapter = adapter

        val carTypes = resources.getStringArray(R.array.car_type_array)
        val carTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, carTypes)
        carTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        carTypeSpinner.adapter = carTypeAdapter

        // Date picker
        dateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                dateEditText.setText("$day/${month + 1}/$year")
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Time picker
        timeEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                timeEditText.setText(String.format("%02d:%02d", hour, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        // Driver switch UI
        val tvDriverStatus = findViewById<TextView>(R.id.tvDriverStatus)
        fun updateDriverSwitchUI(isChecked: Boolean) {
            if (isChecked) {
                tvDriverStatus.text = "Yes"
                tvDriverStatus.setTextColor(ContextCompat.getColor(this, R.color.green_700))
                switchDriverRequired.thumbTintList = ContextCompat.getColorStateList(this, R.color.green_700)
                switchDriverRequired.trackTintList = ContextCompat.getColorStateList(this, R.color.green_300)
            } else {
                tvDriverStatus.text = "No"
                tvDriverStatus.setTextColor(ContextCompat.getColor(this, R.color.red_700))
                switchDriverRequired.thumbTintList = ContextCompat.getColorStateList(this, R.color.red_700)
                switchDriverRequired.trackTintList = ContextCompat.getColorStateList(this, R.color.red_300)
            }
        }
        updateDriverSwitchUI(switchDriverRequired.isChecked)
        switchDriverRequired.setOnCheckedChangeListener { _, isChecked -> updateDriverSwitchUI(isChecked) }

        // Girl Cab switch UI
        fun updateGirlCabSwitchUI(isChecked: Boolean) {
            if (isChecked) {
                tvGirlCabStatus.text = "Yes"
                tvGirlCabStatus.setTextColor(ContextCompat.getColor(this, R.color.green_700))
                switchGirlCab.thumbTintList = ContextCompat.getColorStateList(this, R.color.green_700)
                switchGirlCab.trackTintList = ContextCompat.getColorStateList(this, R.color.green_300)
            } else {
                tvGirlCabStatus.text = "No"
                tvGirlCabStatus.setTextColor(ContextCompat.getColor(this, R.color.red_700))
                switchGirlCab.thumbTintList = ContextCompat.getColorStateList(this, R.color.red_700)
                switchGirlCab.trackTintList = ContextCompat.getColorStateList(this, R.color.red_300)
            }
        }
        updateGirlCabSwitchUI(switchGirlCab.isChecked)

        // Setup listener for Girl Cab switch
        girlCabSwitchListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AlertDialog.Builder(this)
                    .setTitle("Girl Cab Only")
                    .setMessage("Only female passengers can join this ride, which may increase matching time. Proceed?")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        updateGirlCabSwitchUI(true)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        switchGirlCab.setOnCheckedChangeListener(null)
                        switchGirlCab.isChecked = false
                        updateGirlCabSwitchUI(false)
                        switchGirlCab.setOnCheckedChangeListener(girlCabSwitchListener)
                    }
                    .setCancelable(false)
                    .show()
            } else {
                updateGirlCabSwitchUI(false)
            }
        }
        switchGirlCab.setOnCheckedChangeListener(girlCabSwitchListener)

        // 👩‍🦰 Female-only logic — enable after Firestore check
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val cachedGender = sharedPref.getString("gender", null)
        if (cachedGender != null) {
            if (cachedGender.lowercase(Locale.ROOT) == "female" || cachedGender.lowercase(Locale.ROOT) == "f") {
                enableGirlCabSwitch()
            }
        } else {
            auth.currentUser?.let { user ->
                db.collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        val gender = doc.getString("gender")?.lowercase(Locale.ROOT) ?: "unknown"

                        // Cache gender
                        with(sharedPref.edit()) {
                            putString("gender", gender)
                            apply()
                        }

                        if (gender == "female" || gender == "f") {
                            enableGirlCabSwitch()
                        }
                    }
            }
        }

        // Book ride button
        bookButton.setOnClickListener {
            val pickup = pickupSpinner.selectedItem.toString()
            val drop = dropSpinner.selectedItem.toString()
            val date = dateEditText.text.toString()
            val time = timeEditText.text.toString()
            val carType = carTypeSpinner.selectedItem.toString()
            val passengerCountStr = passengerCountEditText.text.toString()
            val driverRequired = switchDriverRequired.isChecked
            val girlCabOnly = switchGirlCab.isChecked

            if (pickup == drop) {
                Toast.makeText(this, "Pickup and drop cannot be the same", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pickup.isEmpty() || drop.isEmpty() || date.isEmpty() || time.isEmpty() || passengerCountStr.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val passengerCount = passengerCountStr.toIntOrNull()
            if (passengerCount == null || passengerCount <= 0) {
                Toast.makeText(this, "Enter a valid passenger count", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val maxSeats = when (carType.lowercase()) {
                "5-seater" -> 5
                "7-seater" -> 7
                else -> Int.MAX_VALUE
            }
            if (passengerCount > maxSeats) {
                Toast.makeText(this, "Max $maxSeats passengers for $carType", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage("Please book only if your ride is confirmed. Fake or incomplete bookings may lead to a ban. Proceed?")
                .setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss()
                    performBooking(pickup, drop, date, time, carType, passengerCount, driverRequired, girlCabOnly)
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .setCancelable(false)
                .show()
        }
    }

    private fun enableGirlCabSwitch() {
        switchGirlCab.isEnabled = true
        switchGirlCab.alpha = 1f
        tvGirlCabStatus.alpha = 1f
    }

    private fun performBooking(
        pickup: String,
        drop: String,
        date: String,
        time: String,
        carType: String,
        passengerCount: Int,
        driverRequired: Boolean,
        girlCabOnly: Boolean
    ) {
        bookButton.isEnabled = false
        hideKeyboard()

        val currentUser = auth.currentUser!!
        val userId = currentUser.uid
        val rideId = db.collection("bookings").document().id

        val intent = Intent(this, MatchedRidesActivity::class.java).apply {
            putExtra("rideId", rideId)
            putExtra("pickup", pickup)
            putExtra("drop", drop)
            putExtra("date", date)
            putExtra("time", time)
            putExtra("carType", carType)
            putExtra("passengerCount", passengerCount)
            putExtra("driverRequired", driverRequired)
            putExtra("girlCabOnly", girlCabOnly)
            putExtra("isFromBooking", true)
        }
        startActivity(intent)

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val userName = userDoc.getString("name") ?: "Unknown"
                val booking = hashMapOf(
                    "rideId" to rideId,
                    "uid" to userId,
                    "name" to userName,
                    "pickup" to pickup,
                    "drop" to drop,
                    "date" to date,
                    "time" to time,
                    "carType" to carType,
                    "passengerCount" to passengerCount,
                    "driverRequired" to driverRequired,
                    "girlCabOnly" to girlCabOnly,
                    "joinedUids" to listOf(userId),
                    "driverId" to null,
                    "driverName" to null
                )
                db.collection("bookings").document(rideId).set(booking)
            }
    }

    private fun setGreeting() {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val cachedName = sharedPref.getString("name", null)
        if (cachedName != null) {
            tvGreeting.text = "Hi, $cachedName! 👋 Where will your ride start?"
        } else {
            auth.currentUser?.let { user ->
                db.collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("name") ?: "User"
                        tvGreeting.text = "Hi, $name! 👋 Where will your ride start?"
                        with(sharedPref.edit()) {
                            putString("name", name)
                            apply()
                        }
                    }
                    .addOnFailureListener {
                        tvGreeting.text = "Hi! 👋 Where will your ride start?"
                    }
            } ?: run {
                tvGreeting.text = "Hi! 👋 Where will your ride start?"
            }
        }
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        bookButton.isEnabled = true
    }
}
