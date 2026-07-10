package ir.hrka.download.manager.internal.work

import okhttp3.Call
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks in-flight HTTP calls so notification pause/stop actions can interrupt them immediately.
 */
internal object ActiveDownloadTransferRegistry {

    private val activeCalls = ConcurrentHashMap<String, Call>()

    fun register(downloadId: String, call: Call) {
        activeCalls[downloadId] = call
    }

    fun unregister(downloadId: String) {
        activeCalls.remove(downloadId)
    }

    fun interrupt(downloadId: String) {
        activeCalls[downloadId]?.cancel()
    }
}
