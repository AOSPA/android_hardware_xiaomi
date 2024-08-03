package co.aospa.dolby.xiaomi.geq.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun EqualizerBands(viewModel: EqualizerViewModel) {
    val preset by viewModel.preset.collectAsState()
    val bandGains = preset.bandGains

    Row {
        BandGainSliderLabels()
        LazyRow(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(bandGains.size) { index ->
                BandGainSlider(
                    bandGains[index],
                    onValueChangeFinished = {
                        viewModel.setGain(index, it)
                    }
                )
            }
        }
    }
}
