package com.sza.tourist

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sza.tourist.ui.tourist.TouristInfoActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * The program home: the start screen the launcher opens, listing the kit's programs. The dashboard
 * ships with S0004; the lights, SOS and stopwatch rows join with S0005-S0007. Widgets and intents
 * open their program directly, past this screen (owner decision, ticket 3.3).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.row_dashboard).setOnClickListener {
            startActivity(TouristInfoActivity.createIntent(this))
        }
    }
}
