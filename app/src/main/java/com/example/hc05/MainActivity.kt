package com.example.hc05

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hc05.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

const val BT_MAC = "98:D3:31:FC:1B:1B" //""24:79:F3:8E:55:AA"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val databinding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
//       setContentView(R.layout.activity_main)
        val myViewModel = ViewModelProvider(this).get(MyViewModel::class.java)
        myViewModel.context = this
        if (myViewModel.coroutineisactived == true) {
            myViewModel.job?.cancel()
        }
        //    if (myViewModel.isPowerOn == true)  {           //第1次關閉協程
        //        myViewModel.job.cancel()
        //    }


        //     writeData("1")
        //     myViewModel.init()
/* test code below  -> cs 少55算法
       val cs = 0x44+0x02+0x00+0x0a    //80(dec)  不要用16進位算式
        val cshex = cs.toString(16)    //80(dec)= 50(hex)
        val css1 = cshex.length             // 大小 (2)
        val css3 = cshex.subSequence(css1-2, css1)  //ccs1 = 2 不含2  (range=0,2) 印出50(hex)
        val css4 = css3.toString()
        val css5: Int = css4.toInt(16)
        val css6 = (255-css5).toString(16)    //結果af
        println ("ccs6=${css6}")  */
        editTextSetWeight.setText("300")

        // val csa = 68+2+0+10       // 80dec      少了55做運算
        val csx = editTextSetWeight.text.toString().toInt()
        val csa = 68 + 2 + 0 + editTextSetWeight.text.toString()
            .toInt()       // 80dec+data      規格內少了前55做運算
        println("csa = $csa")
        val csb = (65535 - csa).toString(16)      // 取1's
        println("csb = $csb")
        val csc = csb.subSequence(2, 4)          // 已知它是4個數字
        println("csc=$csc")                 //得到它是af   */

//getData
        var data = editTextSetWeight.text.toString()
        //data  處理
        data = data.toInt().toString(16)
        if (data != "") {
            when (data.toInt(16)) {
                in 0..15 -> {
                    data = "00" + "0" + data
                }        //要取4位
                in 16..255 -> {
                    data = "00" + data
                }            //要取4位}
                in 256..(16 * 16 * 15 + 16 * 15 + 15) -> {
                    data = "0" + data
                }
                else -> {
                    data = data
                }
            }
        }
        println("data=$data")




        myViewModel.reReadListenKey.observe(this, androidx.lifecycle.Observer {
            if (myViewModel.readableEnable == true) {
                myViewModel.init()
            }
        })
//按鍵處理
        btnDisconnect.setOnClickListener {
            //          if (myViewModel.btconnectstates == true) {          //連線成功才按
            //            if (myViewModel.coroutineisactived == false) {
            ///              myViewModel.init()            //注意讀寫都去協程做
            //           myViewModel.readableEnable == true
            //         myViewModel.coroutineisactived = true
            //       } //啟動協程旗號
            //        }
            myViewModel.job?.cancel()
            myViewModel.err01 = true
            myViewModel.btSocket?.close() //支援空值的btsocketclose所以一直按不會當
            Log.d(TAG, "btScoket closed, ${myViewModel.btSocket}")
            Log.d(TAG, "isconnected?: ${myViewModel.btSocket?.isConnected}")
        }
//按鍵處理
        btnConnect.setOnClickListener {
            Log.d(TAG, "btsocket status: ${myViewModel.btSocket?.isConnected}")
            if (myViewModel.btSocket?.isConnected == null || myViewModel.btSocket?.isConnected == false) {
                // wait 2sec 再連接, 因為發生了錯誤了 (不要馬上關馬上連會有問題的）
                Toast.makeText(this, "Open Socket", Toast.LENGTH_SHORT).show()
                myViewModel.isPowerOn = true
                myViewModel.err01 = false
                myViewModel.CheckBt()
                myViewModel.Connect()
                if (myViewModel.btconnectstates == true) {
                    myViewModel.init()
                }
            }
        }
//
        btnSendWeight.setOnClickListener {
            var dataString = editTextSetWeight.text.toString()
            if (dataString != "") {
                var dataInt = dataString.toInt()
                //data  處理
                when (dataInt) {
                        in 256..65535 -> {
                        Toast.makeText(this, "資料值太大了", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                         data = dataString.toInt().toString(16)   // 裡面是16進制處理
                        println ("data = $data")
                        println ("dataString.toInt() = ${dataString.toInt()}")
                        when (dataString.toInt()) {    //10進制 = 10
                            in 0..15 -> {
                                dataString = "00" + "0" + data   //data = 0f
                            }        //要取4位
                            in 16..255 -> {
                                dataString = "00" + data
                            }            //
                            in 256..(16 * 16 * 15 + 16 * 15 + 15) -> {
                                dataString = "0" + data
                            }  //超過255後處理
                            else -> {
                                data = data
                            }
                        }
                        println("data=$dataString")

                        // check sum  處理  （裡面是10進制處理）  檢查碼：（命令+長度+數值）取最後2byte 再做1's
                        val dataInt = editTextSetWeight.text.toString().toInt()
                        val datahi = dataInt / 256                   // 將重量折分為2個數字 hi/lo
                        val datalo = dataInt % 256

                        val csa = 68 + 2 + datahi + datalo          //把10進制數值全部相加
                        // 70dec+data      規格內少了前55做運算
                        println("csa = $csa")
                        val csb = (65535 - csa).toString(16)      // 取1's （用最大值65535去減）
                        println("csb=$csb")
                        val cssize =
                            csb.length                         // 取數字的長度, (存在bug, lenght 不可小於2,否則會當機）
                        // 亦輸入的數值為65535 它就當了所以要限制大小
                        val csc = csb.subSequence(cssize - 2, cssize)         // 已知它的結果
                        println("csc=$csc")                                //得到它是af
//範例：55 44 02 00 0A AF 90
                        myViewModel.dataSend =
                            "554402" + dataString + csc + "90"  // 送資料了, 它有做檢查碼阿！  (直接傳16進制）就不用管了
                        println("myViewModel.dataSend = ${myViewModel.dataSend}")

                        myViewModel.isWriteEanbled = true          // 開始送資料
                    }
                }
            } else {
                Toast.makeText(this, "資料不可為空", Toast.LENGTH_SHORT).show()
            }
        }
        //========================================================================
        //監控資料變化
        myViewModel.weightlivedata.observe(this, androidx.lifecycle.Observer
        {
            textViewWeight.text = myViewModel.weightlivedata.value.toString()
        })

        myViewModel.replivedata.observe(this, androidx.lifecycle.Observer
        {
            textViewRep.text = myViewModel.replivedata.value.toString()
        })

    }

    fun sendweight(vm: ViewModel, setWeight: String) {
        GlobalScope.launch {


        }
    }


    override fun onPause() {
        super.onPause()
    }


}