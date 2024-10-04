@file:OptIn(ExperimentalStdlibApi::class)

package com.example.bluetoothapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.icu.text.Transliterator.Position
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetoothapp.activity.DeviceActivity
import com.example.bluetoothapp.adapter.AvailableDeviceAdapter
import com.example.bluetoothapp.adapter.DeviceListAdapter
import com.example.bluetoothapp.databinding.ActivityMainBinding
import com.example.bluetoothapp.fragments.DeviceFragment
import com.example.bluetoothapp.interfaces.DeviceItemListener
import com.example.bluetoothapp.model.DeviceModel


class MainActivity : AppCompatActivity(), DeviceItemListener {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_DISCOVERABILITY = 2
    private lateinit var deviceAdapter: DeviceListAdapter
    private lateinit var availableDeviceAdapter: AvailableDeviceAdapter
    private var isEnable: Boolean = false
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var deviceList: ArrayList<DeviceModel>? = null
    private var availableDeviceList = ArrayList<BluetoothDevice>()
    private var bleDeviceList = ArrayList<BluetoothDevice>()
    private var selectedDevice: BluetoothDevice? = null
    private var isBluetoothPermission = false

    private val scanner = bluetoothManager?.adapter?.bluetoothLeScanner

    private val mReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                val device: BluetoothDevice? =
                    intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, "onReceive: $device")
                if (device?.name != null) {
                    Log.d(TAG, "onReceive device name: ${device.name}")

                    if (!availableDeviceList.contains(device)) {
                        availableDeviceList.add(device)
                    }
                    Log.d(TAG, "onReceive: available device list $availableDeviceList")

                    showAvailableDeviceList()
                }

            }

            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                Log.d("BluetoothState", "onReceive: $state")
                when (state) {
                    BluetoothAdapter.STATE_OFF -> binding.turnOnBluetooth.isChecked = false
                    BluetoothAdapter.STATE_TURNING_OFF -> {}
                    BluetoothAdapter.STATE_ON -> {
                        binding.turnOnBluetooth.isChecked = true
                        setBluetoothTurnOn()
                        showAvailableDeviceList()
                    }
                    BluetoothAdapter.STATE_TURNING_ON -> {}
                }
            }
        }

    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter


        registerReceiver(mReceiver, getIntentFilter())

        if (bluetoothAdapter == null) {
            Log.d(TAG, "onCreate: Bluetooth is not supported")
        }

        if (bluetoothAdapter?.isEnabled == false) {
            requestBluetooth()
            if (isBluetoothPermission) {
                setBluetoothTurnOn()
            }
        } else {
            binding.turnOnBluetooth.isChecked = true
            setBluetoothTurnOn()
        }

        binding.rvDeviceList.layoutManager = LinearLayoutManager(this)
        binding.rvAvailableDeviceList.layoutManager = LinearLayoutManager(this)


        binding.turnOnBluetooth.setOnClickListener {
            if (bluetoothAdapter?.isEnabled == false) {
                requestBluetooth()
                if(isBluetoothPermission) {
                    binding.turnOnBluetooth.isChecked = true
                    setBluetoothTurnOn()
                }
            } else {
                bluetoothAdapter?.disable()
                isEnable = false
                binding.layoutPairedDevice.visibility = View.GONE
                binding.layoutAvailableDevice.visibility = View.GONE
            }
        }


        binding.btRefresh.setOnClickListener {
            if (bluetoothAdapter?.isEnabled == true) {
                deviceList?.clear()
                Log.i("CHECK_LENGTH", deviceList?.size.toString())
                showDeviceList()
                discoverNewDevices()
                startScan()
            } else Toast.makeText(this, "Please, Turn on the Bluetooth", Toast.LENGTH_SHORT).show()
        }

    }

    private fun setBluetoothTurnOn() {
        binding.layoutPairedDevice.visibility = View.VISIBLE
        binding.layoutAvailableDevice.visibility = View.VISIBLE
        showDeviceList()
        discoverNewDevices()
        startScan()
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceList() {
        val pairedDevices: MutableSet<BluetoothDevice>? = bluetoothAdapter?.getBondedDevices()
        Log.d(TAG, "showDeviceList: $pairedDevices")
        if (bluetoothAdapter?.isEnabled == true) {
            deviceList = ArrayList()
            for (bt in pairedDevices!!) {
                deviceList?.add(DeviceModel(bt, false))
                Log.d(TAG, "showDeviceList: ${bt.name}")
            }
            Log.d(TAG, "paired device: $deviceList")
            deviceAdapter = DeviceListAdapter(deviceList!!, this@MainActivity)
            binding.rvDeviceList.adapter = deviceAdapter
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverNewDevices() {

        if (bluetoothAdapter?.isDiscovering == true) {
            Log.d(TAG, "discoverNewDevices: discovering is true")
            bluetoothAdapter?.cancelDiscovery()

            checkPermissionForBT()

            bluetoothAdapter?.startDiscovery()

//            val discoverableIntent =
//                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
//            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
//            startActivityForResult(discoverableIntent, REQUEST_DISCOVERABILITY)

            Log.d(TAG, "discoverNewDevices: register receiver")

        }
        if (bluetoothAdapter?.isDiscovering == false) {
            Log.d(TAG, "discoverNewDevices: discovering is false")

            checkPermissionForBT()
            bluetoothAdapter?.startDiscovery()

//            val discoverableIntent =
//                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
//            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
//            startActivityForResult(discoverableIntent, REQUEST_DISCOVERABILITY)

            Log.d(TAG, "discoverNewDevices: register receiver")
        }

    }

    private fun checkPermissionForBT() {
        Log.d(TAG, "checkPermissionForBT: called")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            + ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                100
            )
        } else {
            Log.d(TAG, "checkPermissionForBT: No need to check permission")
        }
    }

    private fun showAvailableDeviceList() {

        availableDeviceAdapter =
            AvailableDeviceAdapter(this, availableDeviceList, this@MainActivity)
        binding.rvAvailableDeviceList.adapter = availableDeviceAdapter

    }

    private fun requestBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestEnableBluetooth.launch(enableBtIntent)

        }

    }

    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // granted
                Log.d(TAG, "request permission: permission granted")
                isBluetoothPermission = true

            } else {
                // denied
                Log.d(TAG, "request permission: permission denied")
            }

        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("MyTag", "${it.key} = ${it.value}")
            }

        }

    @SuppressLint("MissingPermission")
    override fun onDeviceItemClick(bleDevice: BluetoothDevice) {

        Log.d(TAG, "onDeviceItemClick: $bleDevice")
        selectedDevice = bleDevice
        connect()

        val intent = Intent(this, DeviceActivity::class.java)
        intent.putExtra("DeviceName", bleDevice.name)
        startActivity(intent)
//        discoverServices()
    }


    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mReceiver)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            Log.d(TAG, "onScanResult: ${result?.device?.name}")

            if (result?.device?.name != null && !bleDeviceList.contains(result.device)) {
                bleDeviceList.add(result.device)
            }

        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)

            Log.d(TAG, "onScanFailed: Something wrong")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {

        scanner?.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        scanner?.stopScan(scanCallback)
    }

    private val callback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onConnectionStateChange: ble device is not connected successfully")
                return
            }

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "onConnectionStateChange: device connected")
                Log.d(TAG, "onConnectionStateChange: ${gatt?.device}")
                Log.d(
                    TAG,
                    "onConnectionStateChange: ${gatt?.device?.name} + ${gatt?.device?.bondState}"
                )
                val serviceUuid = gatt?.device?.uuids
                Log.d(TAG, "onConnectionStateChange: ${serviceUuid?.size}")
                stopScan()
                discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)

            services = gatt.services
            Log.d(TAG, "onServicesDiscovered: ${services.size}")

            printAllBleServicesAndCharacteristics(gatt)
            for (service in services) {
                val serviceUuid = service.uuid
                val charUuid = service.getCharacteristic(serviceUuid)
                Log.d(TAG, "onServicesDiscovered: $serviceUuid")
                Log.d(TAG, "onServicesDiscovered: $charUuid")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {

            super.onCharacteristicRead(gatt, characteristic, status)


            val checkuuid = characteristic.uuid

            Log.i("CHECK_UUID", checkuuid.toString())

            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i(
                            "BluetoothGattCallback",
                            "Read characteristic $uuid:\n${value.toHexString()}"
                        )
                    }

                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid!")
                    }

                    else -> {
                        Log.e(
                            "BluetoothGattCallback",
                            "Characteristic read failed for $uuid, error: $status"
                        )
                    }
                }
            }

            Log.d(TAG, "onCharacteristicRead: ${characteristic.uuid}")
            Log.d(TAG, "onCharacteristicRead: ${String(characteristic.value)}")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            Log.d(TAG, "onCharacteristicWrite: $status")
            Log.d(TAG, "onCharacteristicWrite: ${characteristic?.uuid}")
            if (characteristic != null) {
                Log.d(TAG, "onCharacteristicWrite: ${String(characteristic.value)}")
            }

        }
    }

    @SuppressLint("MissingPermission")
    private fun connect() {
        gatt = selectedDevice?.connectGatt(this, false, callback)
        Log.d(TAG, "connected ble device is : ${gatt?.device?.name}")
    }

    @SuppressLint("MissingPermission")
    private fun discoverServices() {
        gatt?.discoverServices()
    }

    private fun printAllBleServicesAndCharacteristics(bluetoothGatt: BluetoothGatt) {
        var s = ""
        var c = ""
        var d = ""

        val servicesList = bluetoothGatt.services
        for (i in servicesList.indices) {
            val bluetoothGattService = servicesList[i]
            s = bluetoothGattService.uuid.toString()
            Log.d(TAG, s)

            val bluetoothGattCharacteristicList = bluetoothGattService.characteristics

            for (bluetoothGattCharacteristic in bluetoothGattCharacteristicList) {
                c = bluetoothGattCharacteristic.uuid.toString()
                Log.d(TAG, "$s->$c")

            }
        }
    }

    private fun getIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)

        return intentFilter
    }

    companion object {
        var gatt: BluetoothGatt? = null
        var services: List<BluetoothGattService> = emptyList()
    }

}