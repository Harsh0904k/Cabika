package com.hk.vcab.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.hk.vcab.R
import com.hk.vcab.models.Passenger

class PassengerAdapter(private val passengers: List<Passenger>) :
    RecyclerView.Adapter<PassengerAdapter.PassengerViewHolder>() {

    class PassengerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.txtPassengerName)
        val reg: TextView = view.findViewById(R.id.txtPassengerReg)
        val phone: TextView = view.findViewById(R.id.txtPassengerPhone)
        val email: TextView = view.findViewById(R.id.txtPassengerEmail)
        val count: TextView = view.findViewById(R.id.txtPassengerCount)
        val btnCall: Button = view.findViewById(R.id.btnCallPassenger) // Call button
        val btnWhatsApp: Button = view.findViewById(R.id.btnWhatsAppPassenger) // WhatsApp button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PassengerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_passenger_card, parent, false)
        return PassengerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PassengerViewHolder, position: Int) {
        val p = passengers[position]
        val label = if (p.isMe) "Me" else "Co-passenger"
        holder.name.text = "$label • ${p.name}"
        holder.reg.text = "Reg No: ${p.regNo}"
        holder.phone.text = "Phone: ${p.phone}"
        holder.email.text = "Email: ${p.email}"
        holder.count.text = "Passengers: ${p.passengerCount}"

        // Call button
        holder.btnCall.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${p.phone}")
            context.startActivity(intent)
        }

        // WhatsApp button
        holder.btnWhatsApp.setOnClickListener {
            val context = holder.itemView.context
            val number = p.phone.replace("+", "").replace(" ", "")
            val url = "https://wa.me/$number"
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = passengers.size
}
