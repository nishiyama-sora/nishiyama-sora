package com.example.sendpictureforgoogleglass

import android.Manifest
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sendpictureforgoogleglass.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.io.*
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.net.Socket as Socket


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor:ExecutorService

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
            Manifest.permission.READ_EXTERNAL_STORAGE, //ストレージ読み込み
            Manifest.permission.WRITE_EXTERNAL_STORAGE //ストレージ書き込み
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

            imageCapture = ImageCapture.Builder().build()

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


    private fun takePhoto() {

        val photoTitle = SimpleDateFormat("yy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
            .format(System.currentTimeMillis())


        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, photoTitle)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")


        val outputOption = ImageCapture
            .OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            )
            .build()

        imageCapture?.takePicture(
            outputOption, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                //写真撮影失敗時
                override fun onError(error: ImageCaptureException) {
                    //Toast.makeText(this, "撮影に失敗しました", LENGTH_SHORT).show()
                    error.printStackTrace()
                    println("撮影失敗")
                    //Log.e(TAG, "${error.message}", error)
                }

                //写真撮影成功時
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    //Toast.makeText(this@MainActivity, "撮影完了", LENGTH_SHORT).show()
                    println("撮影完了")
                }

            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


    @OptIn(DelicateCoroutinesApi::class)
    private fun connect() {
        val aN = AddressNumber.getInstance()


        //ストレージ読み込みの許可確認
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //許可済みの場合入力されたサーバーに対して写真撮影&画像送信を行う


            //撮影された画像を特定
            var count = 0
            //撮影した写真を特定する
            File("/storage/emulated/0/Pictures").walk().forEach {
                if (it.isFile) {
                    count++
                }
            }


            //画像が2枚未満の場合，ダミー画像を作成
            if (count < 2) {
                for (i in 2-count downTo 0 step 1) {
                    val bmp = BitmapFactory.decodeResource(resources, R.drawable.dummy)
                    val imgName = "/storage/emulated/0/Pictures/" + "dummy" + i + ".jpg"

                    try {
                        val fileStream = FileOutputStream(imgName)
                        bmp.compress(Bitmap.CompressFormat.JPEG, 70, fileStream)
                        fileStream.flush()
                        fileStream.close()
                    } catch (e:Exception) {
                        e.printStackTrace()
                    }
                    println("ダミー作成")
                }
            }

            //ソケット通信が成功したかの結果を格納する変数
            var success : Boolean = true
            //写真撮影＆送信
            GlobalScope.launch {
                while (situationObserve) {
                    takePhoto()
                    delay(300)
                    success = sendPicture()
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
    private fun sendPicture(): Boolean {

        var tmp : File? = null
        var count = 0

        var result = true

        //撮影した写真を特定する
        File("/storage/emulated/0/Pictures").walk().forEach {
            //count++
            //更新日が一番最近の画像ファイルを取得
            if (it.isFile) {
                count++
                if (tmp == null) {
                    tmp = it
                } else if (it.lastModified() > tmp!!.lastModified()) {
                    tmp = it
                }
            }
        }


        println(count)

        if (count >= 1) {
            try {
                val aN = AddressNumber.getInstance()
                val address = InetSocketAddress(aN.ipAddressString, aN.portNumberInt)
                val socket = Socket()
                //val connection = Socket(aN.ipAddressString, aN.portNumberInt)
                val connection = socket.connect(address, 3000)
                val out = socket.getOutputStream()
                println(tmp?.path)

                val options = BitmapFactory.Options()
                options.inSampleSize = 4

                val img = BitmapFactory.decodeFile(tmp?.path, options)


                out.write(bitmapToByte(img))
                out.close()
                socket.close()
                println("finish")


                //写真削除
                File(tmp?.path.toString()).delete()
                //Files.delete(Paths.get(tmp?.path))

                //ギャラリーからも消す（削除した画像へのパスの消去）
                applicationContext.contentResolver.delete(
                    MediaStore.Files.getContentUri("external"),
                    MediaStore.Files.FileColumns.DATA + "=?",
                    arrayOf(tmp?.path)
                )


            } catch (e: Exception) {
                e.printStackTrace()
                result = false
            }
        }

        return result

    }

    //BitmapをByteに変換
    private fun bitmapToByte(bmp:Bitmap):ByteArray{

        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)

        return stream.toByteArray()
    }
}

