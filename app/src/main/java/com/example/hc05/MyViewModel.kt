package com.example.hc05

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

val TAG = "myTag"
// 55 3F 04 00 14 00 05 A3 90

class MyViewModel : ViewModel() {
    var isPowerOn = false
    var isWriteEanbled = false
    var dataSend = ""
    var coroutineisactived = false
    var btconnectstates = false
    val TAG = "myTag"
    var readableEnable = false
    val reReadListenKey = MutableLiveData<Boolean>()
    var readResult = ArrayList<String>()
    var indexstartbyte = 0
    var weightlivedata = MutableLiveData<Int>()
    var replivedata = MutableLiveData<Int>()
    var numBytes = 0
    var weight = 0
    var rep = 0
    var keepalivetime = 0
    lateinit var context: Context
    var job: Job? = null
    var err01 = false
    var buffer: ByteArray = ByteArray(1024)

    init {
        weightlivedata.value = 0
        replivedata.value = 0

    }

    lateinit var mBluetoothAdapter: BluetoothAdapter
    var btSocket: BluetoothSocket? = null
    //      lateinit var bytes:ByteArray

    //       val myUUID = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b")
    var myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")


    fun CheckBt() {
        Toast.makeText(context, "It has started", Toast.LENGTH_SHORT).show()
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!mBluetoothAdapter.enable()) {
            Toast.makeText(context, "Bluetooth Disabled !", Toast.LENGTH_SHORT).show()
            /* It tests if the bluetooth is enabled or not, if not the app will show a message. */
            //   finish()
        }
        if (mBluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth null !", Toast.LENGTH_SHORT).show()
        }
    }

    fun Connect() {
        val device = mBluetoothAdapter.getRemoteDevice(BT_MAC)   //Renox10 mac  配對
        Log.d(TAG, "Connecting to ... $device")
        Toast.makeText(
            context,
            "Connecting to ... ${device.name} mac: ${device.uuids[0]} address: ${device.address}",
            Toast.LENGTH_LONG
        ).show()
        mBluetoothAdapter.cancelDiscovery()
        try {
            Log.d(TAG, "btScoket 前: ${btSocket} ")
            btSocket = device.createRfcommSocketToServiceRecord(myUUID)
            /* Here is the part the connection is made, by asking the device to create a RfcommSocket (Unsecure socket I guess), It map a port for us or something like that */
            Log.d(TAG, "btScoket 後: ${btSocket} ")
            btSocket?.connect()
            btconnectstates = true
            Log.d(TAG, "Connection made. ${btSocket}")
            Toast.makeText(context, "Connection made.", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            try {
                btSocket?.close()           //可以正常關閉
                btconnectstates = false

            } catch (e2: IOException) {
                Log.d(TAG, "Unable to end the connection")
                Toast.makeText(context, "Unable to end the connection", Toast.LENGTH_SHORT).show()
            }
            Log.d(TAG, "Socket creation failed,${btSocket}")
            Toast.makeText(context, "Socket creation failed", Toast.LENGTH_SHORT).show()
        }
        //beginListenForData()
        /* this is a method used to read what the Arduino says for example when you write Serial.print("Hello world.") in your Arduino code */
    }

    //===============
    private fun writeData(data: String) {
        var outStream = btSocket?.outputStream
        try {
            outStream = btSocket?.outputStream
        } catch (e: IOException) {
            //Log.d(FragmentActivity.TAG, "Bug BEFORE Sending stuff", e)
        }
 //       val msgBuffer = data.toByteArray()         //轉成ASCII處理
                var size = data.length
        val msgBuffer = ByteArray(size-1)    // 16進制處理
        size /= 2
           for (i in 0..size-1) {
               //取值
               msgBuffer[i] = data.subSequence(i * 2, i * 2 + 2)
                   .toString().toInt(16).toByte()
               println ("Hex = ${msgBuffer[i]} ")
           }

        /* 測試值是OK的
        val msgBuffer = ByteArray(7)
        msgBuffer[0]=0x55
        msgBuffer[1]=0x44
        msgBuffer[2]=0x02
        msgBuffer[3]=0x00
        msgBuffer[4]=0x0a
        msgBuffer[5]=0xaf-256
        msgBuffer[6]=0X90-256                 //byte最大值是 -128~+127 之間值 */

        try {
            outStream?.write(msgBuffer)
        } catch (e: IOException) {
            //Log.d(FragmentActivity.TAG, "Bug while sending stuff", e)
        }
    }

    //=====================
    private fun readData() {
        //是否加keepalive , 不然我不知道它斷了, 我直接斷它的電
        //    Log.d(TAG, "btScoket10:${btSocket?}")
        //      if (btSocket!!.isConnected) {

        var inStream = btSocket?.inputStream
        //         Log.d(TAG, "inStream: $inStream")

        try {
            inStream = btSocket?.inputStream
        } catch (e: IOException) {
            println(e.printStackTrace())
            Log.d(TAG, "IOException0: ${e.printStackTrace()}")
        }
//假如buffer內有資料就先處理, 再去讀取

        try {
            if (err01 == false) {
                numBytes = inStream!!.read(buffer)  // bytes returned from read()
                //             Log.d(TAG, "numBytes = $numBytes")
            }
        } catch (e: Exception) {
            //         Log.d(TAG, "numBytes = $numBytes")
            //         Log.d(TAG, "buffer = $buffer, $e")
            Log.d(TAG, "IOException1: ${e.printStackTrace()}")
            Toast.makeText(context, "斷線了", Toast.LENGTH_SHORT).show()
            btSocket?.close()             //關閉連線
            err01 = true
            //處理異常
            //   job.cancel()                   // 取消協程

        }


        if (err01 != true) {           //沒有error 才做
//發生了exception 你還往下執行你是頭腦壞了嗎？
            for (i in 0..numBytes - 1) {   //先知道nubBytes的數字再去讀
                if (buffer[i].toInt() >= 0) {
                    readResult.add(buffer[i].toString())
                } else {                //負數處理
                    val b1 = 256 + buffer[i].toInt()
                    readResult.add(b1.toString())
                }

            }
            if (readResult.size >= 90) {
                readResult.clear()
            } else {
                while (readResult.size >= 9) {
                    ParserStart()       //印出正常處理
                    println(readResult)
                }
                weightlivedata.value = weight       //更新ui
                replivedata.value = rep
            }
        } //if end
    }  // Read data end

//=================================================================

    //  函式開始

    fun ParserStart() {
        indexstartbyte = readResult.indexOf("85")
        if (readResult[indexstartbyte] != "85") {
            delRangeofArrayData(readResult, indexstartbyte, 0)
            return
        }

        if (readResult[indexstartbyte + 1] != "63") {
            delRangeofArrayData(readResult, indexstartbyte, 1)
            return
        }
        if (readResult[indexstartbyte + 2] != "4") {
            delRangeofArrayData(readResult, indexstartbyte, 2)
            return
        }

        if (readResult[indexstartbyte + 8] != "144") {
            delRangeofArrayData(readResult, indexstartbyte, 8)
            return
        }

//parser成功，請資料
        //讀重量
        var weightHibyte = readResult[indexstartbyte + 3].toInt()
        var weightLobyte = readResult[indexstartbyte + 4].toInt()
        weight = (256 * weightHibyte + weightLobyte).toInt()
        //      weightlivedata.value = weight
        //讀次數
        var repHibyte = readResult[indexstartbyte + 5].toInt()
        var repLobyte = readResult[indexstartbyte + 6].toInt()
        rep = 256 * repHibyte + repLobyte
//        replivedata.value = rep

        for (i in 0..indexstartbyte + 8) {
            readResult.removeAt(0)          //清除已處理的資料
        }
        println("正常處理")
    }

    fun delRangeofArrayData(arr: ArrayList<String>, startIndex: Int, num: Int) {
        println("err1 = $arr")
        for (i in 0..startIndex + num) {          //刪除 從頭刪到index + num的值
            arr.removeAt(0)
        }
        println("err2 =$arr")
    }


    //清除及搬移

    fun init() {
        job = viewModelScope.launch(Dispatchers.Main) {
            delay200ms()
        }
// 先執行再delay

    }

    suspend fun delay200ms() {
        //     Log.d(TAG, "delay200ms: ")
        delay(timeMillis = 200)
        if (err01 != true) {              //都不能發生異常情況才執行
            readData()
            keepalivetime++
            keepalivetime = 0
            readableEnable = true
            reReadListenKey.value = reReadListenKey.value
            //
            if (isWriteEanbled) {
 //               writeData("554402000AAF90")  有檢查碼, 檢查碼錯不能用
                println ("dataSend = $dataSend")
                writeData(dataSend)
            }
            isWriteEanbled = false
        }
    }
}
