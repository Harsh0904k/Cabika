package com.hk.vcab

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat

class RideAdapter(
    private val context: Context,
    private val rideList: List<Map<String, Any>>
) : BaseAdapter() {

    override fun getCount(): Int = rideList.size
    override fun getItem(position: Int): Any = rideList[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_driver_ride, parent, false)

        val ride = rideList[position]

        val txtDetails = view.findViewById<TextView>(R.id.txtRideDetails)
        val btnAccept = view.findViewById<Button>(R.id.btnAccept)

        val pickup = ride["pickup"] as? String ?: "Unknown"
        val drop = ride["drop"] as? String ?: "Unknown"
        val date = ride["date"] as? String ?: "Unknown"
        val time = ride["time"] as? String ?: "Unknown"
        val totalPassengers = (ride["totalPassengers"] as? Number)?.toInt() ?: 1

        txtDetails.text = """
            From: $pickup
            To: $drop
            Date: $date
            Time: $time
            Passengers: $totalPassengers
        """.trimIndent()

        val visibility = ride["visibility"] as? String ?: "no"
        if (visibility == "yes") {
            btnAccept.isEnabled = true
            btnAccept.text = "Join"
            btnAccept.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            btnAccept.backgroundTintList =
                ContextCompat.getColorStateList(context, android.R.color.holo_green_dark)
        } else {
            btnAccept.isEnabled = false
            btnAccept.text = "Can't Join" // ✅ ensure text shows
            btnAccept.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            btnAccept.backgroundTintList =
                ContextCompat.getColorStateList(context, android.R.color.darker_gray)
        }

        btnAccept.setOnClickListener {
            if (visibility == "yes") {
                val intent = Intent(context, DriverAcceptRideActivity::class.java)
                intent.putExtra("rideId", ride["id"] as? String)
                context.startActivity(intent)
            }
        }

        return view
    }
}
