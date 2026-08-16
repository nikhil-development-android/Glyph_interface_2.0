package com.glyphinterface

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class GlyphTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val newState = !RootUtils.isMainEnabled
        RootUtils.isMainEnabled = newState
        RootUtils.setGlyphOperatingMode(newState)
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (RootUtils.isMainEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
