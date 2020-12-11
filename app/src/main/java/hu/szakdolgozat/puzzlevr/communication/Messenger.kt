package hu.szakdolgozat.puzzlevr.communication

import android.util.Log
import hu.szakdolgozat.puzzlevr.ui.vr.VRViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class Messenger( socket: Socket, val viewModel: VRViewModel) : Thread() {
    lateinit var inputStream: InputStream
    lateinit var outputStream: OutputStream

    init {
        try {
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
        } catch (e: Exception) {
            viewModel.tableTop.isMultiPlayer = false
            Log.e("Messenger:", "No socket found!")
        }
    }

    override fun run() {
        val buffer = ByteArray(1024)
        var bytes: Int

        while (true) {
            try {
                bytes = inputStream.read(buffer)
                if (bytes > 0) {
                    viewModel.handler.obtainMessage(1, bytes, -1, buffer).sendToTarget()
                }
            } catch (exception: Exception) {
            }
        }
    }

    fun write(bytes: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                outputStream.write(bytes)
            } catch (exception: Exception) {
                viewModel.tableTop.isMultiPlayer = false
                viewModel.tableTop.release(
                    viewModel.tableTop.playerTwo,
                    viewModel.tableTop.playerOne
                )
            }
        }
    }

}