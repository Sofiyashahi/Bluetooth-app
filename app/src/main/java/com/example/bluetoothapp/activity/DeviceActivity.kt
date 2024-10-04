package com.example.bluetoothapp.activity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetoothapp.MainActivity
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
    private var services: List<BluetoothGattService> = emptyList()

    val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    val CHARACTER_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

    private val dataList = arrayListOf(
        "Hi, there",
        "Hello",
        "Greetings",
        "Random Data",
        "Creating ",
    )

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

        dataAdapter = DeviceDataAdapter(dataList)
        binding.rvDeviceDataList.adapter = dataAdapter



        binding.btWrite.setOnClickListener{
            writeCharacteristic(SERVICE_UUID, CHARACTER_UUID)
            binding.etData.text?.clear()
        }

        binding.btRead.setOnClickListener { readCharacteristic(SERVICE_UUID, CHARACTER_UUID) }
    }


    @SuppressLint("MissingPermission")
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
    fun readCharacteristic(serviceUUID: UUID, characteristicUUID: UUID) {
        val service = gatt?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(characteristicUUID)

        if (characteristic != null) {
            val success = gatt?.readCharacteristic(characteristic)
            binding.tvReceivedData.setText(String(characteristic.value))
            Log.v("DeviceActivity", "Read status: $success")
        }
    }

}