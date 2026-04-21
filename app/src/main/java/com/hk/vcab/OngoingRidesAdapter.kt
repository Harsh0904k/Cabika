package com.hk.vcab.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hk.vcab.R
import com.hk.vcab.models.OngoingRide

class OngoingRidesAdapter(private val rides: List<OngoingRide>) :
    RecyclerView.Adapter<OngoingRidesAdapter.RideViewHolder>() {

    class RideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pickup: TextView = view.findViewById(R.id.txtPickup)
        val drop: TextView = view.findViewById(R.id.txtDrop)
        val time: TextView = view.findViewById(R.id.txtTime)
        val status: TextView = view.findViewById(R.id.txtStatus)
        val passengers: TextView = view.findViewById(R.id.txtPassengers)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ongoing_ride, parent, false)
        return RideViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        val ride = rides[position]

        // Pickup & Drop
        holder.pickup.text = "From: ${ride.pickup}"
        holder.drop.text = "To: ${ride.drop}"
        holder.time.text = "📅 ${ride.date} • ${ride.time}"

        // Status text with dynamic color
        if (ride.joinedUsers > 0) {
            holder.status.text = "Student Joined"
            holder.status.setTextColor(Color.parseColor("#1976D2")) // blue
        } else {
            holder.status.text = "Waiting"
            holder.status.setTextColor(Color.parseColor("#F44336")) // Red
        }
        holder.status.setTypeface(null, Typeface.BOLD)

        // Passengers text in green
        val totalPassengers = ride.creatorPassengerCount + ride.joinedUsers
        if (ride.joinedUsers > 0) {
            holder.passengers.text = "👤 $totalPassengers/${ride.maxSeats} passengers"
            holder.passengers.setTextColor(Color.parseColor("#4CAF50")) // Green
        } else {
            holder.passengers.text = "Waiting for users to join"
            holder.passengers.setTextColor(Color.parseColor("#F44336")) // Red
        }
    }


    override fun getItemCount() = rides.size
}
