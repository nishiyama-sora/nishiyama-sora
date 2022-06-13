package com.example.sendpictureforgoogleglass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StartupReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent?) {
        //端末起動時にMainActivityを起動
        Intent(context, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(this)
        }
    }
}