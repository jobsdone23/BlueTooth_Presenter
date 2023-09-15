package com.example.bluetooth_presenter

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.registerReceiver
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetooth_presenter.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.*


class MainActivity : AppCompatActivity() {
    private var devices: Set<BluetoothDevice>? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private lateinit var binding: ActivityMainBinding
    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null //블루투스에 데이터 출력하기 위한 출력스트림

    private var inputStream: InputStream? = null //블루투스에 데이터 입력하기 위한 입력스트림

    private lateinit var bAdapter : com.example.bluetooth_presenter.BluetoothAdapter

    private var workerThread: Thread? = null //문자열 수신에 사용되는 쓰레드

    private lateinit var readBuffer //수신된 문자열 저장 버퍼
            : ByteArray
    private var readBufferPosition //버퍼 내 문자 저장 위치
            = 0
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

        bAdapter = BluetoothAdapter { device ->
            Log.e("bluetooth",device.name + device.address)
            Toast.makeText(this, device.name, Toast.LENGTH_SHORT).show()
            MakeSocket(device)
        }


        binding.permissionButton.setOnClickListener {
            checkPermission()
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activityResultLauncher.launch(enableBtIntent)
        }
        Toast.makeText(this, "블루투스 활성화", Toast.LENGTH_SHORT).show()

        //selectBluetoothDevice()


        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bAdapter
        }

        binding.searchButton.setOnClickListener {
            discoveredDevices.clear()
            bAdapter.notifyDataSetChanged()
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            registerReceiver(receiver, filter)
            bluetoothAdapter?.startDiscovery()
        }

        binding.searchButton2.setOnClickListener {
            val discoverableIntent: Intent =
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                }
            startActivity(discoverableIntent)
        }
        binding.btnSendData.setOnClickListener {
            sendData(binding.tvSendData.getText().toString())
            Toast.makeText(this,"Data를 보냈습니다.",Toast.LENGTH_SHORT).show()
        }

        binding.findlist.setOnClickListener {
            selectBluetoothDevice()
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
                    if(deviceName == null)return
                    val deviceHardwareAddress = device.address // MAC address
                    device?.let {
                        discoveredDevices.add(it)
                        Toast.makeText(
                            applicationContext,
                            it.name + "추가",
                            Toast.LENGTH_SHORT
                        ).show()
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
    private fun selectBluetoothDevice() {
        //이미 페어링 되어있는 블루투스 기기를 탐색
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
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
            //모든 디바이스의 이름을 리스트에 추가
            for (bluetoothDevice in devices as MutableSet<BluetoothDevice>) {
                list.add(bluetoothDevice.name)
            }
            list.add("취소")

            //list를 Charsequence 배열로 변경
            val charSequences = list.toTypedArray<CharSequence>()
            list.toTypedArray<CharSequence>()

            //해당 항목을 눌렀을 때 호출되는 이벤트 리스너
            builder.setItems(charSequences, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    //해당 디바이스와 연결하는 함수 호출
                    connectDevice(charSequences[which].toString())
                }
            })
            //뒤로가기 버튼 누를때 창이 안닫히도록 설정
            builder.setCancelable(false)
            //다이얼로그 생성
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }


    @SuppressLint("MissingPermission")
    private fun connectDevice(deviceName: String) {

        //페어링 된 디바이스 모두 탐색
        for (temp in devices as MutableSet<BluetoothDevice>) {
            Log.e("bluetoothcheck", temp.name.toString())
            if (temp.name.equals(deviceName)) {
                bluetoothDevice = temp
                break
            }
        }

        if (bluetoothDevice == null) return;

        Toast.makeText(
            getApplicationContext(),
            bluetoothDevice?.getName() + " 연결 완료!",
            Toast.LENGTH_SHORT
        ).show();
        MakeSocket(bluetoothDevice!!)
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


    companion object {
        const val REQUEST_ENABLE_BT = 1
    }

    @SuppressLint("MissingPermission")
    private fun MakeSocket(bluetooth : BluetoothDevice){
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        //connect_status = true;
        //Rfcomm 채널을 통해 블루투스 디바이스와 통신하는 소켓 생성

        //connect_status = true;
        //Rfcomm 채널을 통해 블루투스 디바이스와 통신하는 소켓 생성

        try {
            bluetoothSocket = bluetooth!!.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.getOutputStream()
            inputStream = bluetoothSocket?.getInputStream()
            receiveData()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun receiveData() {
        val handler = Handler()

        // 데이터를 수신하기 위한 버퍼를 생성
        readBufferPosition = 0
        readBuffer = ByteArray(1024)


        // 데이터를 수신하기 위한 쓰레드 생성
        workerThread = Thread {
            while (Thread.currentThread().isInterrupted) {
                try {

                    // 데이터를 수신했는지 확인합니다.
                    val byteAvailable = inputStream!!.available()

                    // 데이터가 수신 된 경우
                    if (byteAvailable > 0) {

                        // 입력 스트림에서 바이트 단위로 읽어 옵니다.
                        val bytes = ByteArray(byteAvailable)
                        inputStream!!.read(bytes)

                        // 입력 스트림 바이트를 한 바이트씩 읽어 옵니다.
                        for (i in 0 until byteAvailable) {
                            val tempByte = bytes[i]

                            // 개행문자를 기준으로 받음(한줄)
                            if (tempByte == '\n'.code.toByte()) {

                                // readBuffer 배열을 encodedBytes로 복사
                                val encodedBytes = ByteArray(readBufferPosition)
                                System.arraycopy(
                                    readBuffer,
                                    0,
                                    encodedBytes,
                                    0,
                                    encodedBytes.size
                                )

                                // 인코딩 된 바이트 배열을 문자열로 변환
                                val text = String(encodedBytes, StandardCharsets.US_ASCII)
                                readBufferPosition = 0
                                handler.post(Runnable { // 텍스트 뷰에 출력
                                    binding.tvReceiveData.append(
                                        """
                                        $text
                                        
                                        """.trimIndent()
                                    )
                                })
                            } // 개행 문자가 아닐 경우
                            else {
                                readBuffer[readBufferPosition++] = tempByte
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                try {

                    // 1초마다 받아옴
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        workerThread!!.start()
    }

    fun sendData(text: String) {

        // 문자열에 개행문자("\n")를 추가해줍니다.
        var text = text
        text += "\n"
        try {
            if(outputStream==null)  Log.e("bluetoothcheck", "에러발생")
            // 데이터 송신
            outputStream!!.write(text.toByteArray())
            Log.e("bluetoothcheck", text)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}