package com.example.ipc.socket

import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.example.ipc.R
import com.example.ipc.socket.server.TCPServerService
import kotlinx.android.synthetic.main.activity_tcp_main.*
import java.io.*
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

class TCPClientActivity: AppCompatActivity(), View.OnClickListener{

    var mClientSocket: Socket? = null
    var mPrintWriter: PrintWriter? = null

    val mHandler = @SuppressLint("HandlerLeak")
    object : Handler(){
        override fun handleMessage(msg: Message) {
           when(msg.what){
               MESSAGE_RECEIVE_NEWMSG -> {// 接收新消息
                   TvTcp.text = ""+TvTcp.text + msg.obj
               }
               MESSAGE_SOCKET_CONNECTED ->{// 初始化完成才能发送
                    BtTcp.isEnabled = true
               }
               MESSAGE_CLIENT_SEND -> {// 发送消息
                   TvTcp.text = ""+TvTcp.text + msg.obj
                   Log.d(TAG, ""+TvTcp.text +  msg.obj)
               }
           }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tcp_main)

        BtTcp.setOnClickListener (this)// 点击时间
        val service = Intent(this, TCPServerService::class.java)
        // Android 8 不能直接启动Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service)
        } else {
            startService(service)
        }
        // 开启一个线程
        object: Thread(){
            override fun run() {
                connectTCPServer()
            }
        }.start()
    }

    fun connectTCPServer(){

        var socket:Socket? = null
        while(socket == null) {
            try {
                socket = Socket("localhost", 8688)
                mClientSocket = socket
                mPrintWriter = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                mHandler.sendEmptyMessage(MESSAGE_SOCKET_CONNECTED)
                Log.d(TAG, "connect server success")
            }catch (e: IOException) {
                SystemClock.sleep(1000)
                Log.e(TAG, "connect tcp server failed , retry ....")
            }
        }

        try {
            // 接收服务器的消息
            val br = BufferedReader(InputStreamReader(socket.getInputStream()))
            while (!this.isFinishing) {
                Log.d(TAG, "isFinishing: ")
                val msg = br.readLine()
                Log.d(TAG, "receive: $msg")
                if (msg != null) {
                    val time = formatDateTime(System.currentTimeMillis())
                    val showedMsg = "server $time:$msg\n"
                    mHandler.obtainMessage(MESSAGE_RECEIVE_NEWMSG, showedMsg).sendToTarget()
                }
            }
            Log.d(TAG, "quit...")
            mPrintWriter!!.close()
            br.close()
            socket.close()
        }catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onClick(v: View?) {
        if(v == BtTcp) {// 发送按钮的点击事件
            val msg = EtTcp.text.toString()
            EtTcp.setText("")
            Log.d(TAG, "msg = $msg")
            if (!TextUtils.isEmpty(msg) && mPrintWriter !=null) {
                Thread{
                    mPrintWriter!!.println(msg)
                    val time = formatDateTime(System.currentTimeMillis())
                    val showedMsg = "self$time:$msg\n"
                    mHandler.obtainMessage(MESSAGE_CLIENT_SEND, showedMsg).sendToTarget()
                }.start()
            }
        }
    }

    override fun onDestroy() {
        try {
            if (mClientSocket != null) {
                mClientSocket!!.shutdownOutput()
                mClientSocket!!.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    // 获取当前时间
    @SuppressLint("SimpleDateFormat")
    fun formatDateTime(time: Long): String? {
        return SimpleDateFormat("(HH:mm:ss)").format(Date(time))
    }

    companion object {
        const val MESSAGE_RECEIVE_NEWMSG = 1 //
        const val MESSAGE_SOCKET_CONNECTED = 2
        const val MESSAGE_CLIENT_SEND = 3
        const val TAG = "TCPClientActivity"
    }
}