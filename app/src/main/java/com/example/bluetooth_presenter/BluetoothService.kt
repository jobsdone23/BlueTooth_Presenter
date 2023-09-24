package com.example.bluetooth_presenter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothService(adapter : BluetoothAdapter, mHandler : Handler) {
    private val bluetoothAdapter : BluetoothAdapter = adapter
    private val handler: Handler = mHandler
    lateinit var  sendReceive : SendReceive


    @SuppressLint("MissingPermission")
    inner class ServerClass : Thread() {
        private var serverSocket: BluetoothServerSocket? = null
        override fun run() {
            var socket: BluetoothSocket? = null
            while (socket == null) {
                try {
                    val message = Message.obtain()
                    message.what = Contstants.STATE_CONNECTING
                    handler.sendMessage(message)
                    socket = serverSocket!!.accept()
                } catch (e: IOException) {
                    e.printStackTrace()
                    val message = Message.obtain()
                    message.what = Contstants.STATE_CONNECTION_FAILED
                    handler.sendMessage(message)
                }
                if (socket != null) {
                    val message = Message.obtain()
                    message.what = Contstants.STATE_CONNECTED
                    handler.sendMessage(message)
                    sendReceive = SendReceive(socket)
                    sendReceive.start()
                    break
                }
            }
        }

        init {
            try {
                serverSocket =
                    bluetoothAdapter.listenUsingRfcommWithServiceRecord("Bluetooth Chat App", UUID.fromString(Contstants.UUID))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    inner class ClientClass(private val device: BluetoothDevice) : Thread() {
        private var socket: BluetoothSocket? = null
        override fun run() {
            try {
                socket!!.connect()
                val message = Message.obtain()
                message.what = Contstants.STATE_CONNECTED
                handler.sendMessage(message)
                sendReceive = SendReceive(socket)
                sendReceive.start()
            } catch (e: IOException) {
                e.printStackTrace()
                val message = Message.obtain()
                message.what = Contstants.STATE_CONNECTION_FAILED
                handler.sendMessage(message)
            }
        }

        init {
            try {
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(Contstants.UUID))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    inner class SendReceive(private val bluetoothSocket: BluetoothSocket?) : Thread() {
        private val inputStream: InputStream?
        private val outputStream: OutputStream?
        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = inputStream?.read(buffer)!!
                    Log.d("bt_test", bytes.toString())
                    handler.obtainMessage(Contstants.STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.d("bt_test", e.toString())
                }
            }
        }

        fun write(bytes: ByteArray?) {
            try {
                outputStream?.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        init {
            var tempIn: InputStream? = null
            var tempOut: OutputStream? = null
            try {
                tempIn = bluetoothSocket!!.inputStream
                tempOut = bluetoothSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            inputStream = tempIn
            outputStream = tempOut
        }
    }
    fun getsSendReceive(): SendReceive? {
        return sendReceive
    }

}