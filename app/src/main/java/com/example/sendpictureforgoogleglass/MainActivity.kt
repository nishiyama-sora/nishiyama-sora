package com.example.sendpictureforgoogleglass

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.provider.MediaStore
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.sendpictureforgoogleglass.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.Process
import java.lang.Runnable
import java.net.InetAddress
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Files.exists
import java.nio.file.Paths


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>

    var active: Boolean = true

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 1
        const val STORAGE_PERMISSIONS_REQUEST_CODE = 100
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))

        /*
        //カメラプレビュー
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build().also {
                    it.setSurfaceProvider(binding.previewView.createSurfaceProvider())
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "プレビューの失敗", LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
        */

        //レシーバーの登録
        //registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

        val bluetoothManager = applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        /*
        ストレージ読み込み
        //Bluetooth
        カメラ使用
        */

        //ストレージ読み込み許可確認
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_PERMISSIONS_REQUEST_CODE)
        }

        //ストレージ書き込み許可確認
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSIONS_REQUEST_CODE)
        }


        /*
        //Bluetoothのサポート確認
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", LENGTH_SHORT).show()
        }
        */

        /*
        //Bluetoothを有効にする許可申請
        if (!bluetoothAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
                if (result?.resultCode == Activity.RESULT_OK) {
                    Toast.makeText(applicationContext, "有効", LENGTH_SHORT).show()
                    Log.d("qwert", "有効")
                }
            }
            startForResult.launch(intent)
        }

        */

        binding.sendPic?.isEnabled = false

        //"写真を撮影"ボタン押下時の処理
        binding.launchCameraButton.setOnClickListener {

            try {
                //カメラのパーミッション確認
                if (checkCameraPermission()) {
                    //写真撮影メソッドの呼び出し
                    takePicture()
                } else {
                    //パーミッションを求める
                    grantCameraPermission()
                }
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(this, "camera app error", Toast.LENGTH_LONG).show()
            }


            //送信ボタンを使用可能に変更
            binding.sendPic?.isEnabled = true

        }

        //"ギャラリーを開く"ボタン押下字の処理
        binding.getPicFromGallery.setOnClickListener {
            // 画像フォルダから写真取得
            launcher.launch(arrayOf("image/*"))
            //送信ボタンを使用可能に変更
            binding.sendPic?.isEnabled = true

        }

        /*
        //送信ボタン押下時の処理
        binding.sendPic?.setOnClickListener {
            //ソケット通信
            CoroutineScope(IO).launch {
                client(address, port)
            }
        }
        */


        binding.ipSend?.setOnClickListener {

            val re1 = Regex("[0-9]{1,}\\.[0-9]{1,}\\.[0-9]{1,}\\.[0-9]{1,}")
            val addressText = binding.ipAddressForm?.text.toString()
            val portText = binding.portForm?.text.toString()

            val a = GlobalScope.async {
                    val connection = Socket(addressText, portText.toInt())
                    connection.close()
            }

            //ストレージ読み込みの許可確認
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                //許可済みの場合入力されたIPアドレスに対して画像送信を行うようにして、ギャラリー監視を開始

                if (addressText.matches(re1)) {
                    var flag = false
                    runBlocking {
                        try {
                                a.await()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            flag = true
                        }
                    }
                    if (flag) {
                        println("e")
                        Toast.makeText(this, "入力し直してください", LENGTH_SHORT).show()
                    } else if (!flag){
                        Toast.makeText(this, "接続成功", LENGTH_SHORT).show()
                        CoroutineScope(IO).launch {
                            observeStorage(addressText, portText.toInt())
                        }
                    }
                } else {
                    Toast.makeText(this, "入力し直してください", LENGTH_SHORT).show()
                }

            } else {
                //許可申請
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_PERMISSIONS_REQUEST_CODE)

            }


            /*
            if (active) {
                Toast.makeText(this, "start", LENGTH_SHORT).show()
                bluetoothAdapter.startDiscovery()
                active = false
            }else{
                Toast.makeText(this, "stop", LENGTH_SHORT).show()
                bluetoothAdapter.cancelDiscovery()
                active = true
            }
            */
        }
    }



    //撮影した画像を取得しプレビュー表示
    private val getResult =
        registerForActivityResult (
            ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val image = result.data?.extras?.get("data") as Bitmap
                binding.cameraPreview.setImageBitmap(image)
                //ソケット通信
                /*
                CoroutineScope(IO).launch {
                    client(address, port)
                }
                */
            }
        }


    //カメラアプリで写真撮影を行う
    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        getResult.launch(intent)
    }

    //カメラを選択し，ライフサイクルとユースケースをバインド
    fun bindPreview(cameraProvider: ProcessCameraProvider) {
        var preview : Preview = Preview.Builder().build()

        var cameraSelector : CameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview.setSurfaceProvider(binding.previewView?.getSurfaceProvider())

        var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
    }

    //カメラのパーミッションの確認
    private fun checkCameraPermission() = PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA)

    //カメラのパーミッションの追加要求
    private fun grantCameraPermission() =
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE)

    //パーミッションを得られたか確認
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture()
            }
        }
    }


    //ストレージから画像を選択して表示
    private val launcher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        binding.cameraPreview.setImageURI(it)
    }

    /*
    //ping
    private fun ping(addressText: String): Boolean {
        val runtime = Runtime.getRuntime()
        var proc: Process? = null
        //println("a")
        println("b")
        try {
            val command = "ping " + addressText
            proc = runtime.exec(command)
            println("c")
            val value = proc.waitFor()
            println("d")
            return value == 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    */

    //画像をソケット通信で送信
    private fun sendPicture(address: String, port: Int, path: String?){

        try{
            val connection = Socket(address, port)
            val out = connection.getOutputStream()
            println(path)
            val options = BitmapFactory.Options()
            options.inSampleSize = 2
            val img = BitmapFactory.decodeFile(path, options)

            out.write(bitmapToByte(img))
            out.close()
            connection.close()
            println("finish")
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    //BitmapをByteに変換
    private fun bitmapToByte(bmp:Bitmap):ByteArray{
        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    //ストレージの容量監視
    private fun observeStorage(address: String, port: Int) {

        //val path = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        //val path = applicationContext.filesDir
        //File("/storage/emulated/0/Pictures").name

        var numFile = 0
        var tmp : File? = null
        var count = 0
        while (true) {
            count = 0
            File("/storage/emulated/0/Pictures").walk().forEach {
                count++
                //更新日が一番最近の画像ファイルを取得
                if (it.isFile) {
                    if (tmp == null) {
                        tmp = it
                    } else if (it.lastModified() > tmp!!.lastModified()) {
                        tmp = it
                    }
                }
            }

            //countとnumFileを比較
            //変化があった場合，画像ファイル送信
            if (numFile == 0) {
                numFile = count
            } else if (numFile < count) { //画像ファイルが増加したら
                println(numFile)
                numFile = count
                CoroutineScope(IO).launch {
                    var job = launch {
                        delay(1000)
                    }
                    job.join()
                    sendPicture(address, port, tmp?.path)
                    job.join()
                    try {
                        Files.delete(Paths.get(tmp?.path))
                        numFile -= 1
                        count -= 1
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }


            }


        }

    }

    /*
    //レシーバー
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                //デバイス検知時の処理
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    if (device == null) {
                        Log.d("nullDevice", "Device is null")
                        return
                    }

                    //val deviceName = device?.name
                    val deviceHardwareAddress = device?.address //MAC address
                    //val deviceUUID  = device?.uuids
                    //val deviceRssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                    Toast.makeText(context, deviceHardwareAddress, Toast.LENGTH_SHORT).show()
                    Log.d("device", "アドレス:${deviceHardwareAddress}")
                }
            }
        }
    }
    */

    /*
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (resultCode) {
            RESULT_OK -> {
                Toast.makeText(applicationContext, "有効", Toast.LENGTH_SHORT).show()
                Log.d("qwert", "有効")
            }
            RESULT_CANCELED -> {
                Toast.makeText(applicationContext, "無効", Toast.LENGTH_SHORT).show()
                Log.d("qwert", "無効")
            }
        }
    }
    */

    /*
    //
    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    //bluetoothデバイスを探索
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    //val deviceName = device.name
                    val deviceHardwareAddress = device?.address
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
    }
    */
}