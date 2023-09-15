package com.example.bluetooth_presenter

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.bluetooth_presenter.databinding.ActivityBluetoothAcitivityBinding
import java.util.*

class BluetoothAcitivity : AppCompatActivity() {
    private lateinit var binding: ActivityBluetoothAcitivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothAcitivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

}