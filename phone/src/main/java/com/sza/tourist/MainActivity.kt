package com.sza.tourist

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sza.tourist.ui.flashlight.LightsActivity
import com.sza.tourist.ui.sos.SosActivity
import com.sza.tourist.ui.stopwatch.StopwatchActivity
import com.sza.tourist.ui.tourist.TouristInfoActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * The program home: the start screen the launcher opens, listing the kit's programs. The dashboard
 * ships with S0004, the lights with S0005, SOS with S0006, and the stopwatch with S0007.
 * Widgets and intents open their program directly, past this screen (owner decision, ticket 3.3).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.row_settings).setOnClickListener {
            startActivity(Intent(this, com.sza.tourist.ui.settings.SettingsActivity::class.java))
        }
        findViewById<View>(R.id.row_dashboard).setOnClickListener {
            startActivity(TouristInfoActivity.createIntent(this))
        }
        findViewById<View>(R.id.row_lights).setOnClickListener {
            startActivity(Intent(this, LightsActivity::class.java))
        }
        findViewById<View>(R.id.row_sos).setOnClickListener {
            startActivity(SosActivity.createIntent(this))
        }
        findViewById<View>(R.id.row_stopwatch).setOnClickListener {
            startActivity(StopwatchActivity.createIntent(this))
        }
    }
}
