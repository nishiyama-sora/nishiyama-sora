package com.example.sendpictureforgoogleglass

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.app.ActivityCompat
import com.example.sendpictureforgoogleglass.databinding.ActivityConnectBinding
import com.example.sendpictureforgoogleglass.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class ConnectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectBinding
    private val intentFilter = IntentFilter()
    private val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }
    var mChannel: WifiP2pManager.Channel? = null
    private var mReceiver: BroadcastReceiver? = null
    private val peers = mutableListOf<WifiP2pDevice>()
    val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)

            //(listAdapter as WiFiPeerListAdapter).notifyDataSetChanged()
        }

        if (peers.isEmpty()) {
            Log.d(ContentValues.TAG, "No devices found")
            return@PeerListListener
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConnectBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        println("e")

        mChannel = manager?.initialize(this, mainLooper, null)
        mChannel?.also { channel ->
            mReceiver = WiFiDirectBroadcastReceiver(manager, channel, this)
        }

        println("f")

        val intentFilter = IntentFilter().apply {
            //Wi-Fi P2P が有効かどうかを示す
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            //利用可能なピアのリストが変更されたことを示す
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            //Wi-Fi P2P接続の状態が変更されたことを示す
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            //デバイスの構成の詳細が変更されたことを示す
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        println("g")

        binding.ipSend?.setOnClickListener {

            println("a")

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                println("c")
                return@setOnClickListener
            }
            println("b")
            manager?.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    println("success")
                    val listView = findViewById<ListView>(R.id.listOfDevice)
                    listView?.adapter = ArrayAdapter(this@ConnectActivity, android.R.layout.simple_list_item_1, peers)
                }

                override fun onFailure(p0: Int) {
                    Toast.makeText(this@ConnectActivity, "検出できませんでした", Toast.LENGTH_LONG).show()
                    println("failed")
                }
            })



            /*
            if (!peers.isNullOrEmpty()) {
                println(peers[0].deviceName)
                val device: WifiP2pDevice = peers[0]
                val config = WifiP2pConfig().apply {
                    deviceAddress = device.deviceAddress
                    wps.setup = WpsInfo.PBC
                }
                config.deviceAddress = device.deviceAddress
                mChannel?.also { channel ->
                    manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            println("接続しました")
                            finish()
                        }

                        override fun onFailure(p0: Int) {
                            Toast.makeText(this@ConnectActivity, "接続に失敗しました", Toast.LENGTH_LONG).show()
                            println("接続できませんでした")
                        }
                    })
                }
            } else {
                println("空でした")
            }

             */
        }

        binding.cancelButton?.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        mReceiver.also { receiver ->
            registerReceiver(receiver, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        mReceiver?.also { receiver ->
            unregisterReceiver(receiver)
        }
    }
}