package com.hk.vcab

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class DriverRideAdapter(private val rides: MutableList<Booking>) :
    RecyclerView.Adapter<DriverRideAdapter.RideViewHolder>() {

    class RideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLabel: TextView = itemView.findViewById(R.id.tvRideLabel)
        val tvName: TextView = itemView.findViewById(R.id.tvRideName)
        val tvPhoneOrPassengers: TextView = itemView.findViewById(R.id.tvRidePassengers)
        val tvPickupDrop: TextView = itemView.findViewById(R.id.tvRidePickupDrop)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvRideDateTime)
        val btnDeleteRide: Button = itemView.findViewById(R.id.btnDeleteRide)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_driver_all_rides, parent, false)
        return RideViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        val ride = rides[position]

        // Show Cabika label for assigned rides
        holder.tvLabel.visibility = if (ride.isCabika == true) View.VISIBLE else View.GONE
        holder.tvLabel.text = "Cabika"

        // Name & info
        holder.tvName.text = ride.driverName ?: ride.name
        holder.tvPhoneOrPassengers.text =
            if ((ride.passengerCount ?: 0) > 0) "Passengers: ${ride.passengerCount}"
            else "Phone: ${ride.driverPhone ?: "N/A"}"

        holder.tvPickupDrop.text = "${ride.pickup} → ${ride.drop}"
        holder.tvDateTime.text = "${ride.date}\n${ride.time}"

        // Show delete button only for manually added rides
        if (ride.isCabika == false) {
            holder.btnDeleteRide.visibility = View.VISIBLE
            holder.btnDeleteRide.setOnClickListener {
                deleteManualRide(holder.itemView.context, ride, position)
            }
        } else {
            holder.btnDeleteRide.visibility = View.GONE
        }
    }

    private fun deleteManualRide(context: Context, ride: Booking, position: Int) {
        val db = FirebaseFirestore.getInstance()
        val driverEmail = ride.driverEmail ?: return

        db.collection("driverBookings")
            .document(driverEmail)
            .collection("bookings")
            .document(ride.rideId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Ride deleted", Toast.LENGTH_SHORT).show()
                rides.removeAt(position)
                notifyItemRemoved(position)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount() = rides.size
}
