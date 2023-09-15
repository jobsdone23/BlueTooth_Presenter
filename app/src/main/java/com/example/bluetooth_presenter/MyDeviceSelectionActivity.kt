package com.example.bluetooth_presenter

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.util.*
import java.util.regex.Pattern

class MyDeviceSelectionActivity : Activity() {

    private val deviceManager: CompanionDeviceManager by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(CompanionDeviceManager::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceFilter: BluetoothDeviceFilter = BluetoothDeviceFilter.Builder()
            .setNamePattern(Pattern.compile("My device"))
            .addServiceUuid(ParcelUuid(UUID(0x123abcL, -1L)), null)
            .build()

        // The argument provided in setSingleDevice() determines whether a single
        // device name or a list of device names is presented to the user as
        // pairing options.
        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .build()

        // When the app tries to pair with the Bluetooth device, show the
        // appropriate pairing request dialog to the user.
        deviceManager.associate(pairingRequest,
            object : CompanionDeviceManager.Callback() {

                override fun onDeviceFound(chooserLauncher: IntentSender) {
                    startIntentSenderForResult(chooserLauncher,
                       MainActivity.REQUEST_ENABLE_BT, null, 0, 0, 0)
                }

                override fun onFailure(error: CharSequence?) {
                    // Handle failure
                }
            }, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            MainActivity.REQUEST_ENABLE_BT -> when(resultCode) {
                Activity.RESULT_OK -> {
                    // User has chosen to pair with the Bluetooth device.
                    val deviceToPair: BluetoothDevice? =
                        data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Toast.makeText(this,"블루투스 권한을 허용해주세요",Toast.LENGTH_SHORT).show()
                        return
                    }
                    deviceToPair?.createBond()
                    // ... Continue interacting with the paired device.
                }
            }
        }
    }
}
