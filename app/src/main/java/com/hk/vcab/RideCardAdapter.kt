package com.hk.vcab

import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

data class RideCard(
    val id: String,
    val pickup: String,
    val drop: String,
    val date: String,
    val time: String,
    val passengerCount: Int,
    val creatorId: String,
    val joinedUids: Map<String, Int> = emptyMap(),
    val girlCabOnly: Boolean = false
)

class RideCardAdapter(
    private val context: Context,
    private val rides: List<RideCard>
) : RecyclerView.Adapter<RideCardAdapter.RideCardViewHolder>() {

    private var userGender: String? = null

    fun setUserGender(gender: String?) {
        userGender = gender
    }

    inner class RideCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRideRoute: TextView = itemView.findViewById(R.id.tvRideRoute)
        val tvRideDateTime: TextView = itemView.findViewById(R.id.tvRideDateTime)
        val tvPassengers: TextView = itemView.findViewById(R.id.tvPassengers)
        val btnJoinRide: Button = itemView.findViewById(R.id.btnJoinRide)
        val tvGirlCabLabel: TextView = itemView.findViewById(R.id.tvGirlCabLabel) // ✅ new
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideCardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ride_card, parent, false)
        return RideCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideCardViewHolder, position: Int) {
        val ride = rides[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        holder.tvRideRoute.text = "${ride.pickup} → ${ride.drop}"
        holder.tvRideDateTime.text = "${ride.date} at ${ride.time}"
        holder.tvPassengers.text = "Passengers: ${ride.passengerCount}"

        // 👩‍🦰 Show Girl Cab label if it's a girl-only ride
        if (ride.girlCabOnly) {
            holder.tvGirlCabLabel.visibility = View.VISIBLE
        } else {
            holder.tvGirlCabLabel.visibility = View.GONE
        }

        // 🧍 Disable if user is creator or already joined
        if (currentUserId == ride.creatorId || ride.joinedUids.containsKey(currentUserId)) {
            holder.btnJoinRide.isEnabled = false
            holder.btnJoinRide.text = "Your Booking"
            holder.btnJoinRide.backgroundTintList =
                ContextCompat.getColorStateList(context, android.R.color.darker_gray)
            return
        }

        // 🚫 Male user on girl-only ride
        if (ride.girlCabOnly && userGender?.lowercase() != "female") {
            holder.btnJoinRide.isEnabled = false
            holder.btnJoinRide.text = "Not for you"
            holder.btnJoinRide.backgroundTintList =
                ContextCompat.getColorStateList(context, android.R.color.holo_red_dark)

            holder.btnJoinRide.setOnClickListener {
                Toast.makeText(
                    context,
                    "Only female users can join this ride.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        // ✅ Normal join button
        holder.btnJoinRide.isEnabled = true
        holder.btnJoinRide.text = "Join"
        holder.btnJoinRide.backgroundTintList =
            ContextCompat.getColorStateList(context, android.R.color.holo_green_dark)

        holder.btnJoinRide.setOnClickListener {
            val intent = Intent(context, JoinRideActivity::class.java)
            intent.putExtra("rideId", ride.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = rides.size
}
