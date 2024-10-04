package com.example.bluetoothapp.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.bluetoothapp.R
import com.example.bluetoothapp.databinding.FragmentDeviceBinding

class DeviceFragment : DialogFragment() {

    private lateinit var binding: FragmentDeviceBinding
    private val TAG = "DeviceFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentDeviceBinding.inflate(inflater, container, false)

        Log.d(TAG, "onCreateView: called")


        return binding.root
    }


}