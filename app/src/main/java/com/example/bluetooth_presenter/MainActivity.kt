package com.example.bluetooth_presenter

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetooth_presenter.databinding.ActivityMainBinding
import java.util.*


class MainActivity : AppCompatActivity() {
    private var devices: Set<BluetoothDevice>? = null
    private lateinit var binding: ActivityMainBinding
    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private lateinit var serverClass : BluetoothService.ServerClass
    private lateinit var clientClass : BluetoothService.ClientClass
    private lateinit var bluetoothService: BluetoothService
    private lateinit var bAdapter: BtnAdapter
    var sendReceive: BluetoothService.SendReceive?=null

    companion object {
        const val REQUEST_ENABLE_BT = 1
    }

    private val activityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                Toast.makeText(this, "블루투스 활성화", Toast.LENGTH_SHORT).show()
            } else if (it.resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "취소", Toast.LENGTH_SHORT).show()
            }
        }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermission()
        //블루투스 연동 하기

        bluetoothService = BluetoothService(adapter  = bluetoothAdapter!!,mHandler = mHandler)
        bAdapter = BtnAdapter { device ->
            Log.e("bluetooth", device.name + device.address)
            Toast.makeText(this, device.name, Toast.LENGTH_SHORT).show()
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }
            connectDevice(device.address)
        }


        binding.permissionButton.setOnClickListener {
            checkPermission()
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activityResultLauncher.launch(enableBtIntent)
        }
        Toast.makeText(this, "블루투스 활성화", Toast.LENGTH_SHORT).show()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bAdapter
        }

        binding.searchButton.setOnClickListener {
            serverClass = bluetoothService.ServerClass()
            serverClass.start()
            discoveredDevices.clear()
            bAdapter.notifyDataSetChanged()
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            registerReceiver(receiver, filter)
            bluetoothAdapter?.startDiscovery()

        }

        //검색 2분간 허용하는 기능
        binding.searchButton2.setOnClickListener {
            val discoverableIntent: Intent =
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                }
            startActivity(discoverableIntent)
        }

        //데이터 보내기
        binding.btnSendData.setOnClickListener {
            if(sendReceive == null)  sendReceive = bluetoothService.getsSendReceive()
            try {
                sendReceive?.write(binding.tvSendData.getText().toString().toByteArray())
                Toast.makeText(this, "Data를 보냈습니다.", Toast.LENGTH_SHORT).show()
            }catch (e : Exception){
                Toast.makeText(this, "오류 발생 "+e.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        binding.findlist.setOnClickListener {
            selectBluetoothDevice()
        }

        binding.btnConnect.setOnClickListener {
            Toast.makeText(this, "서버 열기", Toast.LENGTH_SHORT).show()
            serverClass = bluetoothService.ServerClass()
            serverClass.start()
        }
    }

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            when (message.what) {
                Contstants.STATE_LISTENING -> binding.BluetoothStatus.text = "Listening"
                Contstants.STATE_CONNECTING -> binding.BluetoothStatus.text = "Connecting"
                Contstants.STATE_CONNECTED -> binding.BluetoothStatus.text = "Connected"
                Contstants.STATE_CONNECTION_FAILED -> binding.BluetoothStatus.text = "Connection Failed"
                Contstants.STATE_MESSAGE_RECEIVED -> {
                    val readBuff = message.obj as ByteArray
                    val tempMsg = String(readBuff, 0, message.arg1)
                    binding.ReceiveData.text = tempMsg
                }
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action.toString()
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    val deviceName = device.name
                    if (deviceName == null) return
                    val deviceHardwareAddress = device.address // MAC address
                    device?.let {
                        discoveredDevices.add(it)
                        bAdapter.submitList(discoveredDevices)
                        bAdapter.notifyDataSetChanged()
                    }

                    Log.e("bluetoothtest", deviceName + deviceHardwareAddress)
                    //모든 디바이스의 이름을 리스트에 추가
                    //findbluetoothlist.add(deviceName)
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // 블루투스 검색이 완료된 후, 모든 디바이스 정보를 UI에 표시
                    Toast.makeText(
                        applicationContext,
                        "검색 완료! ",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    var pairedDeviceCount = 0
    @SuppressLint("MissingPermission")
    private fun selectBluetoothDevice() {
        devices = bluetoothAdapter?.bondedDevices
        //페어링 된 디바이스 크기 저장
        pairedDeviceCount = devices?.size!!
        //페어링 된 장치가 없는 경우
        if (pairedDeviceCount === 0) {
            //페어링 하기 위한 함수 호출
            Toast.makeText(
                applicationContext,
                "먼저 Bluetooth 설정에 들어가 페어링을 진행해 주세요.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            //디바이스를 선택하기 위한 대화상자 생성
            val builder = AlertDialog.Builder(this)
            builder.setTitle("페어링 된 블루투스 디바이스 목록")
            //페어링 된 각각의 디바이스의 이름과 주소를 저장
            val list: MutableList<String> = ArrayList()
            val list2: MutableList<String> = ArrayList()
            //모든 디바이스의 이름을 리스트에 추가
            for (bluetoothDevice in devices as MutableSet<BluetoothDevice>) {
                list.add(bluetoothDevice.name)
                list2.add(bluetoothDevice.address)
            }
            list.add("취소")

            //list를 Charsequence 배열로 변경
            val charSequences = list.toTypedArray<CharSequence>()
            list.toTypedArray<CharSequence>()

//            //해당 항목을 눌렀을 때 호출되는 이벤트 리스너
//            builder.setItems(charSequences, object : DialogInterface.OnClickListener {
//                override fun onClick(dialog: DialogInterface?, which: Int) {
//                    //해당 디바이스와 연결하는 함수 호출
//                    connectDevice(charSequences[which].toString())
//                }
//            })
            //뒤로가기 버튼 누를때 창이 안닫히도록 설정
            builder.setCancelable(false)
            //다이얼로그 생성
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectDevice(deviceAddress: String) {
        bluetoothAdapter?.let { adapter ->
            // 기기 검색을 수행중이라면 취소
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }

            val device = adapter.getRemoteDevice(deviceAddress)
            try {
                clientClass = bluetoothService.ClientClass(device)
                clientClass.start()
                Toast.makeText(this, "${device.name}과 연결되었습니다.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { // 연결에 실패할 경우 호출됨
                Toast.makeText(this, "기기와 연결이 실패했습니다." +e.toString(), Toast.LENGTH_SHORT).show()
                return
            }
        }
    }


    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(this, "동의 완료", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, BluetoothAcitivity::class.java))
            }

            shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT) -> {
                showPermissionInfoDialog()
            }

            else -> {
                requestPermission()
            }
        }
    }

    private fun showPermissionInfoDialog() {
        AlertDialog.Builder(this).apply {
            setMessage("ppt 리모컨 기능을 위해서, 블루투스 권한이 필요합니다.")
            setNegativeButton("취소", null)
            setPositiveButton("동의") { _, _ ->
                requestPermission()
            }
        }.show()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN
            ), REQUEST_ENABLE_BT
        )
    }


//    fun sendData(text: String) {
//
//        if (connectedThread == null) Toast.makeText(this, "에러가 발생했습니다.", Toast.LENGTH_SHORT).show()
//        else connectedThread.write(text.toByteArray())
//    }

}