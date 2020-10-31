package com.jon.cotbeacon.service.runnables

import android.content.SharedPreferences
import com.jon.common.cot.ChatCursorOnTarget
import com.jon.common.repositories.IDeviceUidRepository
import com.jon.common.repositories.ISocketRepository
import com.jon.cotbeacon.repositories.IChatRepository
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.Socket
import java.net.SocketTimeoutException

class SslListenRunnable(
        prefs: SharedPreferences,
        socketRepository: ISocketRepository,
        chatRepository: IChatRepository,
        deviceUidRepository: IDeviceUidRepository,
) : ChatListenRunnable(prefs, socketRepository, chatRepository, deviceUidRepository) {

    private lateinit var socket: Socket
    private var inputStream: InputStream? = null

    override fun run() {
        safeInitialise {
            socket = socketRepository.getSslSocket()
            inputStream = BufferedInputStream(socket.getInputStream())
        } ?: return

        while (true) {
            postErrorIfThrowable {
                Timber.i("Listening for chat from port %d to %d", socket.port, socket.localPort)
                val bytes = ByteArray(PACKET_BUFFER_SIZE)
                val length = try {
                    inputStream?.read(bytes) ?: return@postErrorIfThrowable
                } catch (e: SocketTimeoutException) {
                    /* SSL sockets have a natural timeout value, so catch this and rerun the loop */
                    return@postErrorIfThrowable
                }
                val receivedBytes = bytes.copyOf(length)
                val xml = String(receivedBytes)
                if (!xml.contains("All Chat Rooms")) {
                    /* Ignore if it's a PLI packet, for example */
                    return@postErrorIfThrowable
                }
                val chat = ChatCursorOnTarget.fromBytes(receivedBytes)
                if (chat?.uid != deviceUid) {
                    /* Only process the packet if it's not a loopback message */
                    dealWithChat(chat)
                }
            } ?: break
        }
        Timber.i("Finishing SslListenRunnable")
        close()
    }

    override fun close() {
        safeClose(socket)
        safeClose(inputStream)
        socketRepository.clearSockets()
    }
}
