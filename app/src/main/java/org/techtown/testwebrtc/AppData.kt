package org.techtown.testwebrtc

import android.util.Log
import android.widget.Toast
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

object AppData {
    private var isDebug = true

    fun debug(tag: String, msg: String) {
        if (isDebug) Log.d(tag, msg)
    }

    fun error(tag: String, msg: String) {
        if (isDebug) Log.e(tag, msg)
    }

    fun error(tag: String, msg: String, ex: Exception?) {
        if (isDebug) Log.e(tag, msg, ex)
    }

    private lateinit var toast: Toast
    fun showToast(msg: String) {
        if (::toast.isInitialized) toast.cancel()
        toast = Toast.makeText(MyApp.getContext(), msg, Toast.LENGTH_SHORT)
        toast.show()
    }

    fun getIP(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val it = en.nextElement()
                val enumIpAddress = it.inetAddresses
                while (enumIpAddress.hasMoreElements()) {
                    val inetAddress = enumIpAddress.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address)
                        return inetAddress.getHostAddress() ?: "unknown IP"
                }
            }
        } catch (_: SocketException) {
        }
        return "unknown IP"
    }
}