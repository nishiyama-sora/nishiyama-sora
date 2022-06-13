package com.example.sendpictureforgoogleglass

import android.app.Application
import android.util.Log


class AddressNumber : Application() {
    var ipAddressString : String = "192.168.0.9"
    var portNumberInt : Int = 80

    companion object {
        private var instance : AddressNumber? = null

        fun getInstance():AddressNumber {
            if (instance == null)
                instance = AddressNumber()

            return instance!!
        }
    }
}