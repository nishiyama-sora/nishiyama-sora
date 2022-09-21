package com.example.sendpictureforgoogleglass

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sendpictureforgoogleglass.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private var situationObserve = true


    companion object {
        const val REQUEST_CODE = 1000
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        cameraExecutor = Executors.newSingleThreadExecutor()

        //リクエストするパーミッション
        val permissions = arrayOf(
            Manifest.permission.CAMERA, //カメラ
        )

        checkPermission(permissions, REQUEST_CODE)


        startCamera()
        connect()


        binding.startStopButton?.setOnClickListener{
            if (situationObserve) {
                situationObserve = false
                binding.startStopButton!!.text = getString(R.string.start)
                binding.startStopButton!!.setBackgroundResource(R.drawable.btn_shape)
            } else {
                situationObserve = true
                binding.startStopButton!!.text = getString(R.string.stop)
                binding.startStopButton!!.setBackgroundResource(R.drawable.btn_shape_stop)
                connect()
            }

        }

        binding.startStopButton?.setOnLongClickListener {

            situationObserve = false
            binding.startStopButton!!.text = getString(R.string.start)
            binding.startStopButton!!.setBackgroundResource(R.drawable.btn_shape)

            val intent = Intent(applicationContext, ConnectActivity::class.java)
            startActivity(intent)

            true
        }

    }


    @Suppress("SameParameterValue")
    private fun checkPermission(permissions: Array<out String>, requestCode: Int) {
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {mPreview->
                mPreview.setSurfaceProvider(
                    binding.previewView?.surfaceProvider
                )
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector,
                    preview, imageCapture
                )
            } catch (e: java.lang.Exception) {
                Log.d(TAG, "カメラ起動に失敗しました", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }





    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }



    @Suppress("OPT_IN_IS_NOT_ENABLED")
    @OptIn(DelicateCoroutinesApi::class)
    private fun connect() {
        //ストレージ読み込みの許可確認
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            //許可済みの場合入力されたサーバーに対して写真撮影&画像送信を行う

            //ソケット通信が成功したかの結果を格納する変数
            var success = true
            var tmp : Bitmap? = null
            //写真撮影＆送信
            GlobalScope.launch {

                while (situationObserve) {

                    delay(350)

                    val b = async {

                        imageCapture?.takePicture(
                            ContextCompat.getMainExecutor(this@MainActivity),
                            object : ImageCapture.OnImageCapturedCallback() {
                                @SuppressLint("UnsafeOptInUsageError")
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    println("done")
                                    tmp = imageToToBitmap(image.image!!)
                                    image.close()
                                    //super.onCaptureSuccess(image)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    println("failed")
                                    //super.onError(exception)
                                }
                            })


                        if (tmp != null) {
                            success = sendPicture(bitmapToByte(tmp!!))
                        }
                    }
                    b.await()

                    //写真送信が失敗した場合ループを抜けて，ボタンのテキストを変更
                    if (!success) {
                        situationObserve = false
                        val a = findViewById<Button>(R.id.startStopButton)
                        a.text = getString(R.string.start)
                        a.setBackgroundResource(R.drawable.btn_shape)
                    }
                }
            }
        } else {
            situationObserve = false
            val a = findViewById<Button>(R.id.startStopButton)
            a.text = getString(R.string.start)
            a.setBackgroundResource(R.drawable.btn_shape)
            Toast.makeText(this, "権限の許可を行ってください", LENGTH_LONG).show()
        }
    }



    //画像をソケット通信で送信
    //写真の送信に成功時にtrue，失敗時にfalseを返す
    @Suppress("OPT_IN_IS_NOT_ENABLED")
    @OptIn(DelicateCoroutinesApi::class)
    private fun sendPicture(imageByteArray: ByteArray): Boolean {

        var result = true

        try {
            val aN = AddressNumber.getInstance()
            val address = InetSocketAddress(aN.ipAddressString, aN.portNumberInt)
            val socket = Socket()
            //3秒間の接続時間待ち
            socket.connect(address, 3000)

            //写真送信
            val out = socket.getOutputStream()
            out.write(imageByteArray)
            out.close()
            socket.close()
            println("finish")
        } catch (e: Exception) {
            //送信失敗時テキストを表示し
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "写真の送信に失敗しました", LENGTH_LONG).show()
            }
            e.printStackTrace()
            result = false
        }

        return result

    }

    //BitmapをByteに変換
    private fun bitmapToByte(bmp: Bitmap):ByteArray{

        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream)

        return stream.toByteArray()
    }

    //
    private fun imageToToBitmap(image: Image): Bitmap {
        val data = imageToByteArray(image)
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)

        return Bitmap.createScaledBitmap(bitmap, 200, 200, true)
    }

    // Image → JPEGのバイト配列
    private fun imageToByteArray(image: Image): ByteArray {
        //val data: ByteArray?
        val planes: Array<Image.Plane> = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val data = ByteArray(buffer.capacity())
        buffer[data]

        return data
    }
}

