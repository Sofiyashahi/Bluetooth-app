package com.example.bluetoothapp.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothapp.R
import com.example.bluetoothapp.interfaces.DeviceItemListener
import com.example.bluetoothapp.model.DeviceModel

class DeviceListAdapter(private val deviceList: ArrayList<DeviceModel>, private val deviceItemListener: DeviceItemListener) : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val tvDeviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
        val tvConnectedStatus: TextView = itemView.findViewById(R.id.tvConnectedStatus)
        val pairedDevice: LinearLayout = itemView.findViewById(R.id.LLPairedDevice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_items, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val deviceItems = deviceList[position]
        holder.tvDeviceName.text = deviceItems.device.name

        val connectedStatus = deviceItems.device.bondState
        if(connectedStatus == 12){
            holder.tvConnectedStatus.text = "Paired"
        } else {
            holder.tvConnectedStatus.text = "Not Connected"
        }

        holder.pairedDevice.setOnClickListener {
            deviceItemListener.onDeviceItemClick(deviceItems.device)
            Log.d("DeviceListAdapter", "onBindViewHolder: device clicked")
        }
    }
}