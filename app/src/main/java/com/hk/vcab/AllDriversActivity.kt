package com.hk.vcab

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AllDriversActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_drivers)

        recyclerView = findViewById(R.id.recyclerAllDrivers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        db.collection("Drivers").get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { it.data.toString() }
                recyclerView.adapter = SimpleTextAdapter(list)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading drivers", Toast.LENGTH_SHORT).show()
            }
    }
}
