package com.hk.vcab

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setContentView(R.layout.activity_register)

        progressDialog = ProgressDialog(this).apply {
            setMessage("Please wait...")
            setCancelable(false)
        }

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val regNoEditText = findViewById<EditText>(R.id.regNoEditText)
        val phoneEditText = findViewById<EditText>(R.id.editTextPhone)
        val genderRadioGroup = findViewById<RadioGroup>(R.id.genderRadioGroup)
        val registerButton = findViewById<Button>(R.id.registerButton)

        val prefillEmail = intent.getStringExtra("prefill_email")

        val isGoogleSignIn = !prefillEmail.isNullOrEmpty()

        if (!prefillEmail.isNullOrEmpty()) {
            emailEditText.setText(prefillEmail)
            emailEditText.isEnabled = false
        }


        if (isGoogleSignIn) {
            passwordEditText.visibility = android.view.View.GONE
            confirmPasswordEditText.visibility = android.view.View.GONE
        }

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim().lowercase()
            val phone = phoneEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val regNo = regNoEditText.text.toString().trim()

            val selectedGenderId = genderRadioGroup.checkedRadioButtonId
            val gender = if (selectedGenderId != -1) {
                findViewById<RadioButton>(selectedGenderId).text.toString()
            } else {
                ""
            }

            // 🔹 Check empty fields
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(regNo) ||
                TextUtils.isEmpty(gender) || TextUtils.isEmpty(phone) ||
                (!isGoogleSignIn && (TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)))) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 College email validation
            if (!(email.endsWith("@vitbhopal.ac.in") || email.endsWith("@test.ac.in"))) {
                Toast.makeText(this, "Please enter your college email", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!isGoogleSignIn && password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressDialog.show()

            if (isGoogleSignIn) {
                val userId = auth.currentUser?.uid ?: run {
                    progressDialog.dismiss()
                    Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val userMap = hashMapOf(
                    "uid" to userId,
                    "name" to name,
                    "email" to email,
                    "regNo" to regNo,
                    "gender" to gender,
                    "phone" to phone
                )

                db.collection("users").document(userId)
                    .set(userMap)
                    .addOnSuccessListener {
                        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                            db.collection("users").document(userId).update("fcmToken", token)
                        }

                        progressDialog.dismiss()
                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, BookingActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
                    }
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: run {
                                progressDialog.dismiss()
                                Toast.makeText(this, "User ID not found after registration", Toast.LENGTH_SHORT).show()
                                return@addOnCompleteListener
                            }

                            val userMap = hashMapOf(
                                "uid" to userId,
                                "name" to name,
                                "email" to email,
                                "regNo" to regNo,
                                "gender" to gender,
                                "phone" to phone
                            )

                            db.collection("users").document(userId)
                                .set(userMap)
                                .addOnSuccessListener {
                                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                                        db.collection("users").document(userId).update("fcmToken", token)
                                    }

                                    progressDialog.dismiss()
                                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, BookingActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener {
                                    progressDialog.dismiss()
                                    Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            progressDialog.dismiss()
                            Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        val homeButton = findViewById<Button>(R.id.homeButton)

        homeButton.setOnClickListener {
            // Sign out any logged-in user before going to LoginActivity
            auth.signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}
