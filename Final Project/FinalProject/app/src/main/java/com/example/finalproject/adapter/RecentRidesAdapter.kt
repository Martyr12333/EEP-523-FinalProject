package com.example.finalproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.data.Ride
import com.example.finalproject.databinding.ItemRideBinding
import java.text.SimpleDateFormat
import java.util.Locale

class RecentRidesAdapter(
    private val onRideClick: (Ride) -> Unit,
    private val onDeleteClick: (Ride) -> Unit
) : ListAdapter<Ride, RecentRidesAdapter.RideViewHolder>(RideDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val binding = ItemRideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RideViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RideViewHolder(
        private val binding: ItemRideBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRideClick(getItem(position))
                }
            }
            
            binding.btnDeleteRide.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }

        fun bind(ride: Ride) {
            binding.apply {
                // Format date
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                tvRideDate.text = dateFormat.format(ride.startTime)

                // Format distance
                tvRideDistance.text = String.format("%.1f km", ride.distance)

                // Format duration
                val hours = ride.duration / 3600000
                val minutes = (ride.duration % 3600000) / 60000
                val durationText = if (hours > 0) {
                    "${hours}h ${minutes}m"
                } else {
                    "${minutes}m"
                }
                tvRideDuration.text = durationText

                // Format calories
                tvRideCalories.text = "${ride.calories} cal"
            }
        }
    }

    private class RideDiffCallback : DiffUtil.ItemCallback<Ride>() {
        override fun areItemsTheSame(oldItem: Ride, newItem: Ride): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Ride, newItem: Ride): Boolean {
            return oldItem == newItem
        }
    }
} 