package com.example.sendpictureforgoogleglass

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.example.sendpictureforgoogleglass.databinding.ActivityConnectBinding
import com.example.sendpictureforgoogleglass.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class ConnectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConnectBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        binding.ipSend?.setOnClickListener {

            //instance呼び出し
            val aN = AddressNumber.getInstance()
            var regexFlag = true

            val address = GlobalScope.async {
                //入力されたIPアドレスの形が正しいか
                val re1 = Regex("[0-9]{1,}\\.[0-9]{1,}\\.[0-9]{1,}\\.[0-9]{1,}")
                val re2 = Regex("[0-9]{1,5}")
                val addressText = binding.ipAddressForm?.text.toString()
                val portNum = binding.portForm?.text.toString()
                if (addressText.matches(re1)) {
                    //IPアドレスをset
                    aN.ipAddressString = addressText
                } else {
                    regexFlag =false
                }
                //port番号をset(入力を数字のみにしているため，正規表現による確認は行わない)
                if (portNum.matches(re2)) {
                    aN.portNumberInt = portNum.toInt()
                } else {
                    regexFlag =false
                }
            }


            runBlocking {
                try {
                    address.await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }


            if (regexFlag) {
                finish()
            } else {
                Toast.makeText(this, "入力しなおしてください", LENGTH_SHORT).show()
            }

        }

        binding.cancelButton?.setOnClickListener {
            finish()
        }
    }
}