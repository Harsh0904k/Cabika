package com.hk.vcab

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditAccountActivity : AppCompatActivity() {

    private lateinit var edtName: EditText
    private lateinit var edtPhone: EditText
    private lateinit var edtRegNo: EditText
    private lateinit var radioMale: RadioButton
    private lateinit var radioFemale: RadioButton
    private lateinit var btnSave: Button
    private lateinit var genderGroup: RadioGroup

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account)

        edtName = findViewById(R.id.edtName)
        edtPhone = findViewById(R.id.edtPhone)
        edtRegNo = findViewById(R.id.edtRegNo)
        radioMale = findViewById(R.id.radioMale)
        radioFemale = findViewById(R.id.radioFemale)
        genderGroup = findViewById(R.id.genderGroup)
        btnSave = findViewById(R.id.btnSave)

        val uid = auth.currentUser?.uid ?: return

        // Fetch current user data
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    edtName.setText(doc.getString("name") ?: "")
                    edtPhone.setText(doc.getString("phone") ?: "")
                    edtRegNo.setText(doc.getString("regNo") ?: "")
                    val gender = doc.getString("gender")?.lowercase() ?: ""
                    if (gender == "male" || gender == "m") {
                        radioMale.isChecked = true
                    } else if (gender == "female" || gender == "f") {
                        radioFemale.isChecked = true
                    }
                }
            }

        // Save updated details
        btnSave.setOnClickListener {
            val name = edtName.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val regNo = edtRegNo.text.toString().trim()

            val gender = when (genderGroup.checkedRadioButtonId) {
                R.id.radioMale -> "male"
                R.id.radioFemale -> "female"
                else -> ""
            }

            if (name.isEmpty() || phone.isEmpty() || regNo.isEmpty() || gender.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updateMap = hashMapOf(
                "name" to name,
                "phone" to phone,
                "gender" to gender,
                "regNo" to regNo
            )

            db.collection("users").document(uid).update(updateMap as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Account updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
