package com.jnetai.stopwatch.utils

/**
 * ErrorLogger - Persistent error tracking and debugging system.
 * Generates error codes, logs stack traces, and stores diagnostic information
 * for all failures, exceptions, and unexpected states in the application.
 *
 * Error codes follow format: E-XXX-YYY where:
 *   XXX = module code (e.g., SWP=StopWatch, CDN=Countdown, ALM=Alarm, SET=Settings)
 *   YYY = sequential error number
 */
object ErrorLogger {

    private const val TAG = "StopWatch-DEBUG"
    private val errorLog = mutableListOf<ErrorRecord>()

    data class ErrorRecord(
        val errorCode: String,
        val message: String,
        val exception: Throwable?,
        val timestamp: Long,
        val threadName: String,
        val stackTrace: String
    )

    // --- Module Error Codes ---
    object Codes {
        const val SWP_START_FAILED = "E-SWP-001"
        const val SWP_PAUSE_FAILED = "E-SWP-002"
        const val SWP_RESET_FAILED = "E-SWP-003"
        const val SWP_LAP_FAILED = "E-SWP-004"
        const val SWP_TIMER_INIT = "E-SWP-005"
        const val SWP_PERSISTENCE_SAVE = "E-SWP-006"
        const val SWP_PERSISTENCE_LOAD = "E-SWP-007"

        const val CDN_INVALID_INPUT = "E-CDN-001"
        const val CDN_START_FAILED = "E-CDN-002"
        const val CDN_FINISHED = "E-CDN-003"
        const val CDN_PAUSE_FAILED = "E-CDN-004"
        const val CDN_RESET_FAILED = "E-CDN-005"

        const val ALM_SET_FAILED = "E-ALM-001"
        const val ALM_TRIGGER_FAILED = "E-ALM-002"
        const val ALM_SOUND_FAILED = "E-ALM-003"
        const val ALM_SCHEDULE_FAILED = "E-ALM-004"
        const val ALM_CANCEL_FAILED = "E-ALM-005"
        const val ALM_SERVICE_FAILED = "E-ALM-006"
        const val ALM_NOTIFICATION_FAILED = "E-ALM-007"

        const val SET_LOAD_FAILED = "E-SET-001"
        const val SET_SAVE_FAILED = "E-SET-002"
        const val SET_SOUND_PICK_FAILED = "E-SET-003"
        const val SET_VOLUME_INVALID = "E-SET-004"
        const val SET_SOUND_UPLOAD_FAILED = "E-SET-005"

        const val GEN_BOOT_FAILED = "E-GEN-001"
        const val GEN_UI_THREAD = "E-GEN-002"
        const val GEN_MEDIA_PLAYER = "E-GEN-003"
        const val GEN_FILE_ACCESS = "E-GEN-004"
        const val GEN_PERMISSION = "E-GEN-005"
        const val GEN_SYSTEM_SERVICE = "E-GEN-006"
        const val GEN_VIEW_BINDING = "E-GEN-007"
        const val GEN_UPDATE_CHECK = "E-GEN-008"
        const val GEN_UNEXPECTED = "E-GEN-999"
    }

    /**
     * Log an error with a specific error code, message, and optional exception.
     */
    fun log(errorCode: String, message: String, exception: Throwable? = null) {
        val stackInfo = if (exception != null) {
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            exception.printStackTrace(pw)
            sw.toString()
        } else {
            Throwable().stackTraceToString()
        }

        val record = ErrorRecord(
            errorCode = errorCode,
            message = message,
            exception = exception,
            timestamp = System.currentTimeMillis(),
            threadName = Thread.currentThread().name,
            stackTrace = stackInfo
        )

        errorLog.add(record)
        android.util.Log.e(TAG, "[$errorCode] $message")
        if (exception != null) {
            android.util.Log.e(TAG, "[$errorCode] Exception:", exception)
        }
        android.util.Log.e(TAG, "[$errorCode] Stack:\n$stackInfo")

        // Keep log size manageable
        if (errorLog.size > 1000) {
            errorLog.removeAt(0)
        }
    }

    /**
     * Log an error code with formatted message.
     */
    fun log(errorCode: String, format: String, vararg args: Any?) {
        log(errorCode, format.format(*args))
    }

    /**
     * Get all logged errors since app start.
     */
    fun getErrorLog(): List<ErrorRecord> = errorLog.toList()

    /**
     * Get the most recent errors.
     */
    fun getRecentErrors(count: Int = 10): List<ErrorRecord> =
        errorLog.takeLast(count).reversed()

    /**
     * Clear the error log.
     */
    fun clearLog() {
        errorLog.clear()
    }

    /**
     * Validate that an object is not null, logging an error if it is.
     * @return true if not null, false if null
     */
    fun requireNotNull(errorCode: String, obj: Any?, context: String): Boolean {
        if (obj == null) {
            log(errorCode, "Null check failed: %s", context)
            return false
        }
        return true
    }

    /**
     * Format error details as a string for debugging displays.
     */
    fun formatErrorReport(errorCode: String, message: String, exception: Throwable?): String {
        val sb = StringBuilder()
        sb.appendLine("════════════════════════════════════════")
        sb.appendLine("ERROR REPORT")
        sb.appendLine("════════════════════════════════════════")
        sb.appendLine("Error Code: $errorCode")
        sb.appendLine("Message:    $message")
        sb.appendLine("Timestamp:  ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",
            java.util.Locale.US).format(java.util.Date(System.currentTimeMillis()))}")
        sb.appendLine("Thread:     ${Thread.currentThread().name}")
        if (exception != null) {
            sb.appendLine("Exception:  ${exception.javaClass.name}: ${exception.message}")
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            exception.printStackTrace(pw)
            sb.appendLine("Stack Trace:\n${sw.toString().take(2000)}")
        }
        sb.appendLine("════════════════════════════════════════")
        return sb.toString()
    }

    /**
     * Try-catch wrapper for running code that might throw.
     * Returns null on failure after logging.
     */
    inline fun <T> tryOrNull(errorCode: String, message: String, block: () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            log(errorCode, message, e)
            null
        }
    }
}
