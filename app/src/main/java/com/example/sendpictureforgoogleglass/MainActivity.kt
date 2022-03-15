package com.example.sendpictureforgoogleglass

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.sendpictureforgoogleglass.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    //lateinit var currentPhotoPath: String

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

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
        }

        //"ギャラりーを開く"ボタン押下字の処理
        binding.getPicFromGallery.setOnClickListener {
            // 画像フォルダから写真取得
            launcher.launch(arrayOf("image/*"))
        }

    }

    //撮影した画像を取得しプレビュー表示
    private val getResult =
        registerForActivityResult (
            ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val image = result.data?.extras?.get("data") as Bitmap
                    binding.cameraPreview.setImageBitmap(image)
            }
        }

    //カメラアプリで写真撮影を行う
    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        getResult.launch(intent)
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

    private val launcher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        binding.cameraPreview.setImageURI(it)
    }
}