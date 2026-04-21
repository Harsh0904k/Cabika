package com.hk.vcab

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class BookingAdapter(
    private val bookings: List<Booking>,
    private val onRideClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position], onRideClick)
    }

    override fun getItemCount() = bookings.size

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvBookingName)
        private val tvPickupDrop: TextView = itemView.findViewById(R.id.tvBookingPickupDrop)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvBookingDateTime)
        private val tvPassengers: TextView = itemView.findViewById(R.id.tvBookingPassengers)
        private val btnVisibility: Button = itemView.findViewById(R.id.btnToggleVisibility)

        // ✅ New TextView for Driver Required status
        private val tvDriverRequired: TextView? = itemView.findViewById(R.id.tvDriverRequired)

        fun bind(booking: Booking, onRideClick: (Booking) -> Unit) {
            tvName.text = booking.name
            tvPickupDrop.text = "${booking.pickup} → ${booking.drop}"
            tvDateTime.text = "${booking.date} ${booking.time}"
            tvPassengers.text = "Passengers: ${booking.passengerCount}"

            // Update visibility button UI
            updateButtonUI(booking.visibility)

            // Update driver required status UI
            tvDriverRequired?.text = if (booking.driverRequired) "Yes" else "No"
            tvDriverRequired?.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (booking.driverRequired) android.R.color.holo_green_dark else android.R.color.holo_red_dark
                )
            )

            // Toggle visibility
            btnVisibility.setOnClickListener {
                val newVisibility = if (booking.visibility == "yes") "no" else "yes"

                FirebaseFirestore.getInstance().collection("bookings")
                    .document(booking.rideId)
                    .update("visibility", newVisibility)
                    .addOnSuccessListener {
                        booking.visibility = newVisibility
                        updateButtonUI(newVisibility)
                    }
            }

            // Click listener to open ride details
            itemView.setOnClickListener {
                onRideClick(booking)
            }
        }

        private fun updateButtonUI(visibility: String) {
            if (visibility == "yes") {
                btnVisibility.text = "Yes"
                btnVisibility.backgroundTintList =
                    ContextCompat.getColorStateList(itemView.context, android.R.color.holo_green_dark)
            } else {
                btnVisibility.text = "No"
                btnVisibility.backgroundTintList =
                    ContextCompat.getColorStateList(itemView.context, android.R.color.holo_red_dark)
            }
        }
    }
}
