/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.geq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import co.aospa.dolby.xiaomi.R
import co.aospa.dolby.xiaomi.geq.ui.EqualizerScreen
import co.aospa.dolby.xiaomi.geq.ui.EqualizerViewModel
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold

class EqualizerActivity : ComponentActivity() {

    private val viewModel: EqualizerViewModel by viewModels { EqualizerViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsTheme {
                SettingsScaffold(
                    title = stringResource(id = R.string.dolby_preset)
                ) { paddingValues ->
                    EqualizerScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
