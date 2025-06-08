/*
 * Copyright (C) 2023-24 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "XiaomiDolby-Boot"

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                // we perform everything in the initializer
                DolbyController.getInstance(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                DolbyController.getInstance(context).onBootCompleted()
            }
            else -> Log.e(TAG, "unhandled intent action")
        }
    }
}
