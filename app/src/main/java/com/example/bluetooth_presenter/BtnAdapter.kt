package com.example.bluetooth_presenter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetooth_presenter.databinding.ItemBluetoothListBinding


class BtnAdapter(private val onClick :(BluetoothDevice) ->Unit) : ListAdapter<BluetoothDevice, BtnAdapter.ViewHolder>(differ) {

    inner class ViewHolder(private val binding: ItemBluetoothListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("MissingPermission")
        fun bind(item: BluetoothDevice) {
            binding.messageTextView.text = item.name
            binding.usernameTextView.text = item.address

            binding.root.setOnClickListener {
                Log.e("bluetooth",item.name + item.address)
                binding.root.setBackgroundColor(Color.parseColor("#E0E0E0"))
                onClick(item)
                Handler(Looper.getMainLooper()).postDelayed({
                    // 클릭한 항목의 스타일을 원래대로 복원
                    binding.root.setBackgroundColor(Color.WHITE)
                }, 50)
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemBluetoothListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }


    companion object {
        val differ = object : DiffUtil.ItemCallback<BluetoothDevice>() {
            override fun areItemsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
                return oldItem == newItem
            }

        }
    }

}