package org.techtown.testwebrtc

import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.techtown.testwebrtc.models.MessageModel
import org.techtown.testwebrtc.util.NewMessageInterface
import java.net.URI

class SocketRepository(private val messageInterface: NewMessageInterface) {
    companion object {
        private const val TAG: String = "SocketRepository"
    }

    private lateinit var webSocket: WebSocketClient
    private lateinit var userName: String
    private val gson: Gson by lazy { Gson() }

    fun initSocket(userName: String) {
        this.userName = userName
        // if you are using android emulator your local websocket address is going to be this: "ws://10.0.2.2:3000"
        // if you are using your phone as emulator your local address is going to be this: "ws://192.168.1.3:3000"
        // but if your websocket is deployed you add your websocket address here

        webSocket = object : WebSocketClient(URI("ws://192.168.148.110:3000")) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                sendMessage(
                    MessageModel(
                        type = "store_user",
                        name = userName,
                        target = null,
                        data = null,
                    )
                )
            }

            override fun onMessage(message: String?) {
                try {
                    messageInterface.onNewMessage(gson.fromJson(message, MessageModel::class.java))
                } catch (ex: Exception) {
                    AppData.error(TAG, "WebSocketClient onMessage", ex)
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                AppData.debug(TAG, "onClose: $code, $reason, $remote")
            }

            override fun onError(ex: Exception?) {
                AppData.error(TAG, "WebSocketClient onError", ex)
            }
        }
        webSocket.connect()
    }

    fun sendMessage(message: MessageModel) {
        try {
            webSocket.send(gson.toJson(message))
        } catch (ex: Exception) {
            AppData.error(TAG, "sendMessage error", ex)
        }
    }
}