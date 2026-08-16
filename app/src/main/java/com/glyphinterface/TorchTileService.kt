package com.glyphinterface

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class TorchTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val newState = !NotificationService.isTorchOn
        NotificationService.isTorchOn = newState
        val intent = Intent(this, NotificationService::class.java)
        startService(intent)
        RootUtils.setGlyphBrightness(if (newState) (RootUtils.globalBrightness * 255).toInt() else 0)
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (NotificationService.isTorchOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
