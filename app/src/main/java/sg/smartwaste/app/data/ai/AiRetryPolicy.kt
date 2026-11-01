package sg.smartwaste.app.data.ai

import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException

/** Decides when trying the next AI model is worth it. */
internal object AiRetryPolicy {

    /**
     * True when the connection itself failed (offline, DNS, refused, or a read/call timeout).
     * Every model goes through the same connection, so trying another one won't help.
     */
    fun isNetworkFailure(e: Throwable): Boolean =
        generateSequence(e) { it.cause }.take(8).any {
            it is UnknownHostException || it is ConnectException || it is NoRouteToHostException || it is InterruptedIOException
        }

    /**
     * Runs [call] for each attempt in order and returns the first success. Model-level errors
     * (HTTP 4xx/5xx, empty or malformed replies) move on to the next attempt. [isFatal] errors
     * and network failures stop immediately so the caller can fall back to on-device AI.
     */
    fun <A, T> firstSuccess(attempts: List<A>, isFatal: (Exception) -> Boolean, call: (A) -> T): T {
        var lastError: Exception? = null
        for (attempt in attempts) {
            try {
                return call(attempt)
            } catch (e: Exception) {
                if (isFatal(e) || isNetworkFailure(e)) throw e
                lastError = e
            }
        }
        throw lastError ?: IOException("AI request failed")
    }
}
