package io.almer.appsimulator.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

val TAG = "AppSimulator: Receiver"

class Receiver() : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "received ${intent.getStringExtra("COMMAND")}")
    }
}