package com.glyphinterface

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class VisualizerTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val newState = !MusicVisualizerService.isRunning
        val intent = Intent(this, MusicVisualizerService::class.java)
        if (newState) {
            startService(intent)
        } else {
            stopService(intent)
            RootUtils.clearAllLeds()
        }
        Handler(Looper.getMainLooper()).postDelayed({
            updateTile()
        }, 100)
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (MusicVisualizerService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
