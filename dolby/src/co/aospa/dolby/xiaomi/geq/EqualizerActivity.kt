/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.geq

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import co.aospa.dolby.xiaomi.geq.ui.EqualizerViewModel
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.widget.R

private const val TAG = "DolbyGeqActivity"

class EqualizerActivity : CollapsingToolbarBaseActivity() {

    private val viewModel: EqualizerViewModel by viewModels { EqualizerViewModel.Factory } 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
                .replace(
                    R.id.content_frame,
                    EqualizerFragment(viewModel),
                    TAG
                )
                .commit()
    }
}
