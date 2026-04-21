package com.hk.vcab

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DriverAddBookingActivity : AppCompatActivity() {

    private lateinit var etDriverName: TextInputEditText
    private lateinit var etDriverPhone: TextInputEditText
    private lateinit var etPickup: TextInputEditText
    private lateinit var etDrop: TextInputEditText
    private lateinit var etDate: TextInputEditText
    private lateinit var etTime: TextInputEditText
    private lateinit var btnSave: MaterialButton

    private val db = FirebaseFirestore.getInstance()
    private val currentDriverEmail = FirebaseAuth.getInstance().currentUser?.email

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_add_booking)

        etDriverName = findViewById(R.id.etDriverName)
        etDriverPhone = findViewById(R.id.etDriverPhone)
        etPickup = findViewById(R.id.etPickup)
        etDrop = findViewById(R.id.etDrop)
        etDate = findViewById(R.id.etDate)
        etTime = findViewById(R.id.etTime)
        btnSave = findViewById(R.id.btnSaveBooking)

        // Pre-fill driver info if logged in
        FirebaseAuth.getInstance().currentUser?.let { user ->
            etDriverName.setText(user.displayName ?: "")
            etDriverPhone.setText(user.phoneNumber ?: "")
        }

        setupDatePicker()
        setupTimePicker()

        btnSave.setOnClickListener {
            saveDriverBooking()
        }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        // Prevent keyboard from appearing
        etDate.showSoftInputOnFocus = false
        etDate.setOnClickListener {
            hideKeyboard()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    calendar.set(year, month, day)
                    etDate.setText(sdf.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupTimePicker() {
        val calendar = Calendar.getInstance()
        // Prevent keyboard from appearing
        etTime.showSoftInputOnFocus = false
        etTime.setOnClickListener {
            hideKeyboard()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    etTime.setText(sdf.format(calendar.time))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    // Safely hide keyboard
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: window.decorView
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun saveDriverBooking() {
        val name = etDriverName.text.toString().trim()
        val phone = etDriverPhone.text.toString().trim()
        val pickup = etPickup.text.toString().trim()
        val drop = etDrop.text.toString().trim()
        val date = etDate.text.toString().trim()
        val time = etTime.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty() || pickup.isEmpty() || drop.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentDriverEmail == null) {
            Toast.makeText(this, "Driver not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val bookingData = hashMapOf(
            "driverEmail" to currentDriverEmail,
            "driverName" to name,
            "driverPhone" to phone,
            "pickup" to pickup,
            "drop" to drop,
            "date" to date,
            "time" to time,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("driverBookings")
            .document(currentDriverEmail)
            .collection("bookings")
            .add(bookingData)
            .addOnSuccessListener {
                Toast.makeText(this, "Booking added successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
