package com.example.bluetoothapp.interfaces

import android.bluetooth.BluetoothDevice

interface DeviceItemListener {
    fun onDeviceItemClick(deviceName: BluetoothDevice)

}