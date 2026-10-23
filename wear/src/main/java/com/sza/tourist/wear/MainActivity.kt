package com.sza.tourist.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sza.tourist.wear.core.variant.Distribution
import com.sza.tourist.wear.core.variant.DistributionInfo
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The skeleton wear screen: a placeholder that proves the package installs and opens. The watch app's
 * own surface (the Tourist dashboard with its touch lock) is ported in S0008.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var distributionInfo: DistributionInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            androidx.wear.compose.material.Text(
                text = getString(R.string.app_name) + " - " + getString(distributionInfo.distribution.labelRes),
            )
        }
    }
}

private val Distribution.labelRes: Int
    get() = when (this) {
        Distribution.STORE -> R.string.distribution_store
        Distribution.NO_LEGAL -> R.string.distribution_no_legal
    }
