**2.3 客户端实现**
客户端Activity启动时，会在onCreate中开启一个线程去连接服务端的Socket，为了保证链接成功采用超时**重传**的机制，为了降低开销时间间隔1000毫秒
```
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
```
a. 连接服务端的Socket
服务端连接后，就可以进行通信了，下面代码中while循环不断地去读取服务端发过来的消息，同时Activity退出时，就退出循环终止。
```
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
```
b. 退出Activity 关闭相关操作
```
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
