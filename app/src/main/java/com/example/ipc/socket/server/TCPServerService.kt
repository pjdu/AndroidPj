package com.example.ipc.socket.server

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.*

import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build




class TCPServerService : Service() {
    private lateinit var tcpServer:TcpServer

    companion object {
        const val TAG = "TCPServerService "
    }

    override fun onCreate() {
        tcpServer = TcpServer();
        object :Thread(tcpServer){}.start()
        Log.d(TAG, "onCreate()")
        super.onCreate()
    }

    // 实现Service必须实现onBind接口
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand(intent: Intent?, flags: Int, startId: Int)")

        if (Build.VERSION.SDK_INT >= 26){// Android 8以上版本处理。
            // 传入参数：通道ID，通道名字，通道优先级（类似曾经的 builder.setPriority()）
            val channel = NotificationChannel("id", "name", NotificationManager.IMPORTANCE_LOW)

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // 创建这个channel
            manager.createNotificationChannel(channel)
            val notification = Notification.Builder(this, "id").build()
            startForeground(1, notification)
        }
       // startForeground(1, Notification.Builder(this).build())
        return super.onStartCommand(intent, flags, startId)
    }



    override fun onDestroy() {
        tcpServer.mIsServiceDestroyed = true
        super.onDestroy()
    }



    class TcpServer : Runnable{
        var mIsServiceDestroyed = false
        private var mDefinedMessages = arrayOf("你好啊", "请问你叫什么名字", "今天北京天气很好 shy", "你知道吗？我可是可以和多个人同时聊天呢", "爱笑的人运气不会太差，不知道真假")


        override fun run() {
            // 监听本地8688端口
            val serverSocket:ServerSocket
            try {
                serverSocket= ServerSocket(8688)
            } catch (e: IOException) {
                Log.e(TAG, "establish tcp server failed ,port 8688")
                e.printStackTrace()
                return
            }


            while(!mIsServiceDestroyed){
                // 接受客户端请求
                val client: Socket = serverSocket.accept()
                Log.d(TAG, "accept()")
                object :Thread(){
                    override fun run() {
                        try {
                            responseClient(client)
                        }catch (e: IOException){
                            e.printStackTrace()
                        }
                    }
                }.start()
            }

        }

        private fun responseClient(client: Socket) {
            // 用于接收客户端消息
            val mIn = BufferedReader(InputStreamReader(client.getInputStream()))
            // 向客户端发送消息
            val mOut = PrintWriter(BufferedWriter(OutputStreamWriter(client.getOutputStream())), true)
            mOut.println("欢迎来到聊天室!")
            while (!mIsServiceDestroyed) {
                val str = mIn.readLine()
                Log.d(TAG, "msg from client:$str")
                if (str == null) { // 断开链接
                    break;
                }
                val i = Random().nextInt(mDefinedMessages.size)
                val msg = mDefinedMessages[i]

                mOut.println(msg)
                Log.d(TAG, "send: $msg")
            }
            Log.d(TAG, "client quit.")
            // 关闭流
            mIn.close()
            mOut.close()
            client.close()
        }
    }
}


