package co.aospa.dolby.xiaomi.geq

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.fragment.app.Fragment
import co.aospa.dolby.xiaomi.R
import co.aospa.dolby.xiaomi.geq.ui.EqualizerScreen
import co.aospa.dolby.xiaomi.geq.ui.EqualizerViewModel
import com.android.settingslib.spa.framework.theme.SettingsTheme

class EqualizerFragment(
    private val viewModel: EqualizerViewModel
) : Fragment() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SettingsTheme {
                    EqualizerScreen(
                        viewModel = viewModel,
                        // apply toolbar height as bottom padding in order to centre it
                        modifier = Modifier.padding(
                            bottom = dimensionResource(R.dimen.settingslib_toolbar_layout_height)
                        )
                    )
                }
            }
        }
    }
}
