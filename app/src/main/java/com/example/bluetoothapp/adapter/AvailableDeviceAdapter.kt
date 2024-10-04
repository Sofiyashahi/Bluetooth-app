package com.example.bluetoothapp.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothapp.R
import com.example.bluetoothapp.interfaces.DeviceItemListener

class AvailableDeviceAdapter(private val context: Context, private val availableDeviceList: ArrayList<BluetoothDevice>, private val deviceItemListener: DeviceItemListener): RecyclerView.Adapter<AvailableDeviceAdapter.ViewHolder>() {

    private var selectedIndex = -1

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val tvDeviceName: TextView = itemView.findViewById(R.id.tvAvailableDeviceName)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.available_device_items, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return availableDeviceList.size
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val availableDeviceItems = availableDeviceList[position]

        holder.tvDeviceName.text = availableDeviceItems.name

        if(selectedIndex == position){
            holder.tvDeviceName.setTextColor(ContextCompat.getColor(context, R.color.green))
        } else {
            holder.tvDeviceName.setTextColor(ContextCompat.getColor(context, R.color.black))
        }
        holder.tvDeviceName.setOnClickListener {
            selectedIndex = holder.adapterPosition
            deviceItemListener.onDeviceItemClick(availableDeviceItems)
            notifyDataSetChanged()
//            availableDeviceList.clear()
            Log.d("DeviceListAdapter", "onBindViewHolder: device clicked")
        }
    }
}