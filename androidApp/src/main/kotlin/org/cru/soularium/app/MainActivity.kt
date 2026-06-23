package org.cru.soularium.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.cru.soularium.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = (application as SoulariumApplication).appGraph
        setContent { App(graph) }
    }
}
