在Android中服务端继承自Service，在`onCreate()`方法中创建一个**线程**,同时需要实现一个继承自`Runnable`接口的`TcpServer`类。
```
 override fun onCreate() {
        tcpServer = TcpServer();
        object :Thread(tcpServer){}.start()// 开启一个线程不要忘记start()
        super.onCreate()
 }
```
b. TcpServer类的实现
这个类主要创建`ServerSocket(port)`的实例,并监听8688端口，随后等待客户端的连接。没建立连接就会创建一个子线程用来与客户端交互。
```
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
                val client: Socket = serverSocket.accept()// 
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
```
c. responseClient(client)类的实现
基于流实现的，交互方式是**同步阻塞方式**，发送读写操作线程会一直**阻塞**在哪里等待对方的回应，如果不做任何事情会有线程开销
```
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
```
> 使用BIO通信的好处与缺点
>好处：代码简单
>缺点：IO效率和扩展性存在瓶颈
>- InputStreamReader 是字节流通向字符流的桥梁,它将字节流转换为字符流.
>- OutputStreamWriter是字符流通向字节流的桥梁，它将字符流转换为字节流.
>- BufferedReader 由Reader类扩展而来，提供通用的缓冲方式文本读取，readLine读取一个文本行，从字符输入流中读取文本，缓冲各个字符，从而提供字符、数组和行的高效读取。
>- BufferedWriter  由Writer 类扩展而来，提供通用的缓冲方式文本写入， newLine使用平台自己的行分隔符，将文本写入字符输出流，缓冲各个字符，从而提供单个字符、数组和字符串的高效写入。
>- PrintWriter()的作用是为了定义流输出的位置，并且此流可以正常的存储中文，减少乱码输出

d. `针对Android8.0启动后台Server的改动`
如果针对 Android 8.0 的应用尝试在不允许其创建后台服务的情况下使用 startService() 函数，则该函数将引发一个 IllegalStateException。

新的 Context.startForegroundService() 函数将启动一个前台服务。现在，即使应用在后台运行，系统也允许其调用 Context.startForegroundService()。不过，应用必须在创建服务后的五秒内调用该服务的 startForeground() 函数。

```
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
