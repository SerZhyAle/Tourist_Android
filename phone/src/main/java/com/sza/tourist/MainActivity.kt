package com.sza.tourist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sza.tourist.core.variant.Distribution
import com.sza.tourist.core.variant.DistributionInfo
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The skeleton start screen: a placeholder that proves the package installs and opens. The product's
 * own start screen is a UI placement decision of S0004, settled before that phase.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var distributionInfo: DistributionInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<android.widget.TextView>(R.id.distribution).text =
            getString(distributionInfo.distribution.labelRes)
    }
}

private val Distribution.labelRes: Int
    get() = when (this) {
        Distribution.STORE -> R.string.distribution_store
        Distribution.NO_LEGAL -> R.string.distribution_no_legal
    }
