package com.hk.vcab.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hk.vcab.R
import com.hk.vcab.models.Passenger
import com.hk.vcab.models.Ride
import com.hk.vcab.RideDetailsActivity


class DriverAcceptedRideAdapter(
    private val rides: List<Ride>,
    private val passengersMap: Map<String, List<Passenger>>, // rideId -> passengers
    private val onLeaveClick: (Ride) -> Unit
) : RecyclerView.Adapter<DriverAcceptedRideAdapter.RideViewHolder>() {

    class RideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rideDetails: TextView = itemView.findViewById(R.id.rideDetails)
        val leaveButton: Button = itemView.findViewById(R.id.btnLeaveRide)
        val viewDetailsButton: Button = itemView.findViewById(R.id.btnViewDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_driver_accepted_ride, parent, false)
        return RideViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        val ride = rides[position]

        // Show only ride info
        holder.rideDetails.text =
            "Pickup: ${ride.pickup}\nDrop: ${ride.drop}\nDate: ${ride.date}\nTime: ${ride.time}"

        holder.leaveButton.setOnClickListener { onLeaveClick(ride) }

        // Open RideDetailActivity on click
        holder.viewDetailsButton.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, RideDetailsActivity::class.java)
            intent.putExtra("rideId", ride.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = rides.size
}
