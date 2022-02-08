package ns.fajnet.android.puftocatorprovider.common

import android.util.Log

class LogEx {

    // companion -------------------------------------------------------------------------------------------------------

    companion object {

        // public methods ----------------------------------------------------------------------------------------------

        fun v(tag: String, msg: String) {
            log(Log.VERBOSE, tag, appendThread(msg))
        }

        fun d(tag: String, msg: String) {
            log(Log.DEBUG, tag, appendThread(msg))
        }

        fun i(tag: String, msg: String) {
            log(Log.INFO, tag, appendThread(msg))
        }

        fun w(tag: String, msg: String) {
            log(Log.WARN, tag, appendThread(msg))
        }

        fun e(tag: String, msg: String) {
            log(Log.ERROR, tag, appendThread(msg))
        }

        // private methods ---------------------------------------------------------------------------------------------

        private fun appendThread(msg: String) = "${Thread.currentThread().name} - $msg"

        private fun log(level: Int, tag: String, msg: String) {
            when (level) {
                Log.VERBOSE -> Log.v(tag, msg)
                Log.DEBUG -> Log.d(tag, msg)
                Log.INFO -> Log.i(tag, msg)
                Log.WARN -> Log.w(tag, msg)
                Log.ERROR -> Log.e(tag, msg)
                else -> Log.e(tag, msg)
            }
        }
    }
}
