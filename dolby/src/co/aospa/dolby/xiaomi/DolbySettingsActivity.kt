/*
 * Copyright (C) 2023-25 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import co.aospa.dolby.xiaomi.preference.DolbySettingsFragment
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity

class DolbySettingsActivity : CollapsingToolbarBaseActivity() {

    private lateinit var dolbyController: DolbyController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(
                com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                DolbySettingsFragment(),
                TAG
            )
            .commit()
        dolbyController = DolbyController.getInstance(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu
            .add(Menu.NONE, MENU_RESET, Menu.NONE, R.string.dolby_reset_all)
            .setIcon(R.drawable.reset_wrench_24px)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            MENU_RESET -> {
                confirmReset()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dolby_reset_all)
            .setMessage(R.string.dolby_reset_all_message)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                dolbyController.resetAllProfiles()
                recreate()
                Toast.makeText(this, getString(R.string.dolby_reset_all_toast), Toast.LENGTH_SHORT)
                    .show()
            }
            .setNegativeButton(android.R.string.no, null)
            .show()
    }

    companion object {
        private const val TAG = "DolbySettingsActivity"
        private const val MENU_RESET = 1001
    }
}
