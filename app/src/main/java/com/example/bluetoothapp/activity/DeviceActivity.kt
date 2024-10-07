package com.example.bluetoothapp.activity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothSocket
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetoothapp.MainActivity
import com.example.bluetoothapp.R
import com.example.bluetoothapp.adapter.DeviceDataAdapter
import com.example.bluetoothapp.databinding.ActivityDeviceBinding
import java.util.UUID


class DeviceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceBinding
    private val TAG = "DeviceActivity"
    private lateinit var dataAdapter: DeviceDataAdapter
    private  val mSocket: BluetoothSocket? = null
    private var device: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null

    val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    val CHARACTER_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

    private val dataList = ArrayList<String>()

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val deviceName = intent.getStringExtra("DeviceName")
        Log.d(TAG, "onCreate: $deviceName")
        binding.tvDeviceName.text = deviceName

        gatt = MainActivity.gatt

        binding.rvDeviceDataList.layoutManager = LinearLayoutManager(this)

        setupData()

        binding.btWrite.setOnClickListener{
            writeCharacteristic(SERVICE_UUID, CHARACTER_UUID)
            binding.etData.text?.clear()
        }

        binding.btRead.setOnClickListener { readCharacteristic(SERVICE_UUID, CHARACTER_UUID) }


    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    private fun setupData(){

        if(MainActivity.connectedStatus){
            Log.d(TAG, "setupData: connected")
            binding.btDisconnect.setImageDrawable(getDrawable(R.drawable.link))
        } else {
            Log.d(TAG, "setupData: not connected")
            binding.btDisconnect.setImageDrawable(getDrawable(R.drawable.unlink))
        }

        binding.btDisconnect.setOnClickListener {
            gatt?.disconnect()
            binding.btDisconnect.setImageDrawable(getDrawable(R.drawable.unlink))
        }

        binding.tvAddress.text = "Address: " + gatt?.device?.address.toString()

        val deviceType = gatt?.device?.type
        var typeString = "Unknown"
        if(deviceType == 1) typeString = "Classic"
        else if(deviceType == 2) typeString = "LE"
        else if(deviceType == 3) typeString = "Dual"

        binding.tvType.text = "Type: $typeString"
        binding.tvAlias.text = "Alias: " + gatt?.device?.alias.toString()
    }


    @SuppressLint("MissingPermission")
    //For writing to Ble device
    fun writeCharacteristic(serviceUUID: UUID, characteristicUUID: UUID) {
        val service = gatt?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(characteristicUUID)

        if (characteristic != null) {
            // First write the new value to our local copy of the characteristic
            val writeChar = binding.etData.text.toString()
            characteristic.value = writeChar.toByteArray()

            //...Then send the updated characteristic to the device
            val success = gatt?.writeCharacteristic(characteristic)

            Log.v("DeviceActivity", "Write status: $success")
        }
    }

    @SuppressLint("MissingPermission")
    //For reading from Ble device
    fun readCharacteristic(serviceUUID: UUID, characteristicUUID: UUID) {
        val service = gatt?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(characteristicUUID)

        if (characteristic != null) {
            val success = gatt?.readCharacteristic(characteristic)
//            binding.tvReceivedData.setText(String(characteristic.value))
            val data = String(characteristic.value)
            dataList.add(data)

            dataAdapter = DeviceDataAdapter(dataList)
            binding.rvDeviceDataList.adapter = dataAdapter
            Log.v("DeviceActivity", "Read status: $success")
        }
    }

}