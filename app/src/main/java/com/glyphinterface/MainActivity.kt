package com.glyphinterface

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glyphinterface.ui.theme.GlyphInterfaceTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "Permissions are required for Glyph features", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AppSettings.init(this)

        val permissions = mutableListOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        requestPermissionLauncher.launch(permissions.toTypedArray())

        if (!isNotificationServiceEnabled()) {
            Toast.makeText(this, "Enable Notification Access for Glyph sync", Toast.LENGTH_SHORT).show()
            try {
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            } catch (e: Exception) {}
        }

        try {
            startService(Intent(this, NotificationService::class.java))
        } catch (e: Exception) {}

        setContent {
            GlyphInterfaceTheme {
                val isDark = when (AppSettings.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                }

                var currentScreen by remember { mutableStateOf(Screen.Main) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Dynamic Wallpaper Canvas Background
                        WallpaperBackground(
                            wallpaperIndex = AppSettings.wallpaperIndex,
                            isDark = isDark,
                            accentColor = AppSettings.currentAccentColor,
                            modifier = Modifier.matchParentSize()
                        )

                        // Main Content Flow
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 86.dp) // Space for Floating Pill Bar
                        ) {
                            when (currentScreen) {
                                Screen.Main -> {
                                    GlyphMainScreen(
                                        isDark = isDark
                                    )
                                }
                                Screen.Call -> {
                                    GlyphCallScreen(
                                        isDark = isDark,
                                        onBack = { currentScreen = Screen.Main }
                                    )
                                }
                                Screen.Timer -> {
                                    GlyphTimerScreen(
                                        isDark = isDark,
                                        onBack = { currentScreen = Screen.Main }
                                    )
                                }
                                Screen.Settings -> {
                                    GlyphSettingsScreen(
                                        isDark = isDark,
                                        onBack = { currentScreen = Screen.Main }
                                    )
                                }
                            }
                        }

                        // Floating Bottom Pill Navigation Bar
                        FloatingBottomBar(
                            currentScreen = currentScreen,
                            onSelectScreen = { currentScreen = it },
                            isDark = isDark,
                            accentColor = AppSettings.currentAccentColor,
                            isGlass = AppSettings.isGlassEffect,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(packageName)
    }
}

/**
 * Dynamic Canvas Backgrounds & Wallpapers
 */
@Composable
fun WallpaperBackground(
    wallpaperIndex: Int,
    isDark: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    when (wallpaperIndex) {
        0 -> {
            // Pitch Black (AMOLED) / Clean Light Surface
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7))
            )
        }
        1 -> {
            // Dot Matrix (Classic Nothing OS Grid)
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF030303) else Color(0xFFF0F0F5))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dotSpacing = 18.dp.toPx()
                    val dotRadius = 1.2.dp.toPx()
                    val numCols = (size.width / dotSpacing).toInt() + 1
                    val numRows = (size.height / dotSpacing).toInt() + 1
                    val dotColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

                    for (i in 0..numCols) {
                        for (j in 0..numRows) {
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = Offset(i * dotSpacing, j * dotSpacing)
                            )
                        }
                    }
                }
            }
        }
        2 -> {
            // Cyber Glow (Radial Ambient Accent Light)
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                                if (isDark) Color(0xFF070707) else Color(0xFFF4F4F8),
                                if (isDark) Color(0xFF000000) else Color(0xFFECECEE)
                            ),
                            center = Offset(400f, 600f),
                            radius = 1200f
                        )
                    )
            )
        }
        3 -> {
            // Frosted Mesh Gradient
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(Color(0xFF140808), Color(0xFF080812), Color(0xFF000000))
                            } else {
                                listOf(Color(0xFFFFFFFF), Color(0xFFF2F5FF), Color(0xFFECEFF5))
                            },
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1800f)
                        )
                    )
            )
        }
        4 -> {
            // Modern Technical Grid
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF050505) else Color(0xFFF4F4F6))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridAlpha = if (isDark) 0.05f else 0.04f
                    val step = 28.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            if (isDark) Color.White.copy(alpha = gridAlpha) else Color.Black.copy(alpha = gridAlpha),
                            Offset(x, 0f),
                            Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            if (isDark) Color.White.copy(alpha = gridAlpha) else Color.Black.copy(alpha = gridAlpha),
                            Offset(0f, y),
                            Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }
                }
            }
        }
    }
}

/**
 * Reusable Glass / Card Modifier
 */
fun Modifier.glyphCardStyle(
    isDark: Boolean,
    isGlass: Boolean,
    cornerRadius: Float = 24f,
    activeBorderColor: Color? = null
): Modifier {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val bgColor = if (isGlass) {
        if (isDark) Color(0x99161616) else Color(0xCCFFFFFF)
    } else {
        if (isDark) Color(0xFF151515) else Color(0xFFFFFFFF)
    }

    val borderColor = activeBorderColor ?: if (isGlass) {
        if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)
    } else {
        if (isDark) Color(0xFF222222) else Color(0x1A000000)
    }

    return this
        .shadow(if (isDark) 8.dp else 4.dp, shape, ambientColor = if (isDark) Color.Black else Color(0x1A000000))
        .clip(shape)
        .background(bgColor)
        .border(1.dp, borderColor, shape)
}

/**
 * Floating Pill Navigation Bar
 */
@Composable
fun FloatingBottomBar(
    currentScreen: Screen,
    onSelectScreen: (Screen) -> Unit,
    isDark: Boolean,
    accentColor: Color,
    isGlass: Boolean,
    modifier: Modifier = Modifier
) {
    val barBg = if (isGlass) {
        if (isDark) Color(0xD9181818) else Color(0xF2FFFFFF)
    } else {
        if (isDark) Color(0xFF151515) else Color(0xFFFFFFFF)
    }

    val barBorder = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(36.dp), ambientColor = Color.Black.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(36.dp))
            .background(barBg)
            .border(1.dp, barBorder, RoundedCornerShape(36.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingBarItem(
                title = stringResource(R.string.nav_visuals),
                icon = Icons.Default.Tune,
                isSelected = currentScreen == Screen.Main,
                accentColor = accentColor,
                isDark = isDark,
                onClick = { onSelectScreen(Screen.Main) },
                testTag = "nav_tab_visuals"
            )

            FloatingBarItem(
                title = stringResource(R.string.nav_patterns),
                icon = Icons.Default.NotificationsActive,
                isSelected = currentScreen == Screen.Call,
                accentColor = accentColor,
                isDark = isDark,
                onClick = { onSelectScreen(Screen.Call) },
                testTag = "nav_tab_patterns"
            )

            FloatingBarItem(
                title = stringResource(R.string.nav_timer),
                icon = Icons.Default.HourglassBottom,
                isSelected = currentScreen == Screen.Timer,
                accentColor = accentColor,
                isDark = isDark,
                onClick = { onSelectScreen(Screen.Timer) },
                testTag = "nav_tab_timer"
            )

            FloatingBarItem(
                title = stringResource(R.string.nav_settings),
                icon = Icons.Default.Settings,
                isSelected = currentScreen == Screen.Settings,
                accentColor = accentColor,
                isDark = isDark,
                onClick = { onSelectScreen(Screen.Settings) },
                testTag = "nav_tab_settings"
            )
        }
    }
}

@Composable
fun FloatingBarItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val unselectedColor = if (isDark) Color(0xFF7E7E82) else Color(0xFF8E8E93)
    val itemBg by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.Transparent,
        label = "itemBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) Color.White else unselectedColor,
        label = "iconTint"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(itemBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 9.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) (if (isDark) Color.White else Color.Black) else unselectedColor
        )
    }
}

/**
 * Interactive Glyph Phone Preview
 */
@Composable
fun GlyphPhonePreview(
    isPoweredOn: Boolean,
    brightness: Float,
    isTorchOn: Boolean,
    isVisualizerOn: Boolean,
    activeTimerSeconds: Int,
    isDark: Boolean,
    accentColor: Color,
    isGlass: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glyphCardStyle(isDark = isDark, isGlass = isGlass, cornerRadius = 32f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Phone Frame Representation
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(224.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(if (isDark) Color(0xFF111111) else Color(0xFF222226))
                .border(3.dp, if (isDark) Color(0xFF2E2E32) else Color(0xFF3E3E42), RoundedCornerShape(36.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Diagonal Strip LEDs (Top Right)
            val topLedAlpha = if (!isPoweredOn) 0.1f else if (isTorchOn) 1f else brightness.coerceIn(0.2f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 10.dp)
                    .width(42.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = topLedAlpha))
                    .then(
                        if (isPoweredOn && topLedAlpha > 0.4f) {
                            Modifier.shadow(8.dp, RoundedCornerShape(3.dp), ambientColor = Color.White, spotColor = Color.White)
                        } else Modifier
                    )
            )

            // Red Recording Status Dot
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 4.dp, start = 8.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isPoweredOn) accentColor else Color(0xFF441010))
            )

            // Center Ring Glyph LED
            val ringLedAlpha = if (!isPoweredOn) 0.1f else if (isVisualizerOn) pulseAlpha * brightness else brightness.coerceIn(0.2f, 1f)
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color(0xFF222222), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .border(
                            2.5.dp,
                            if (isPoweredOn) Color.White.copy(alpha = ringLedAlpha) else Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    if (isPoweredOn && ringLedAlpha > 0.4f) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = ringLedAlpha * 0.18f),
                                radius = size.minDimension / 2
                            )
                        }
                    }
                }
            }

            // Bottom Vertical Glyph LED Ladder
            val timerProgress = if (activeTimerSeconds >= 0) (activeTimerSeconds % 60) / 60f else 1f
            val bottomLedHeight = if (activeTimerSeconds >= 0) (34 * timerProgress).coerceIn(4f, 34f).dp else 34.dp
            val bottomLedAlpha = if (!isPoweredOn) 0.1f else brightness.coerceIn(0.2f, 1f)

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .width(5.dp)
                    .height(bottomLedHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = bottomLedAlpha))
            )
        }

        // Status Badge
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = stringResource(R.string.status_label),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = if (isDark) Color(0xFF7E7E82) else Color(0xFF8E8E93),
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            )
            Text(
                text = if (isPoweredOn) stringResource(R.string.status_active_link) else stringResource(R.string.status_offline),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPoweredOn) Color(0xFF00FF41) else Color(0xFF888888),
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Brightness Controller Card
 */
@Composable
fun HighDensityBrightnessCard(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    autoBrightness: Boolean,
    onAutoBrightnessChange: (Boolean) -> Unit,
    isDark: Boolean,
    accentColor: Color,
    isGlass: Boolean,
    modifier: Modifier = Modifier
) {
    var cardWidthPx by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glyphCardStyle(isDark = isDark, isGlass = isGlass, cornerRadius = 24f)
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.brightness_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF888888) else Color(0xFF6C6C70)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${(brightness * 100).toInt()}%",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Slider Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF222222) else Color(0xFFE5E5EA))
                    .onSizeChanged { cardWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val newB = (offset.x / cardWidthPx).coerceIn(0.05f, 1.0f)
                            onBrightnessChange(newB)
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val newB = (change.position.x / cardWidthPx).coerceIn(0.05f, 1.0f)
                            onBrightnessChange(newB)
                        }
                    }
                    .testTag("brightness_slider"),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = brightness.coerceIn(0.05f, 1f))
                        .background(if (isDark) Color.White.copy(alpha = 0.95f) else accentColor)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BrightnessMedium,
                        contentDescription = "Min Brightness",
                        tint = if (brightness > 0.15f) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.White else Color.Black),
                        modifier = Modifier.size(18.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Max Brightness",
                        tint = if (brightness > 0.88f) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.White else Color.Black),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auto_brightness_desc),
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF888888) else Color(0xFF6C6C70),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = autoBrightness,
                    onCheckedChange = onAutoBrightnessChange,
                    modifier = Modifier.testTag("toggle_auto_brightness"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = if (isDark) Color(0xFF2E2E32) else Color(0xFFD1D1D6)
                    )
                )
            }
        }
    }
}

/**
 * Feature Grid Tile (2x2 Box Style Card)
 */
@Composable
fun FeatureGridTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    isDark: Boolean,
    accentColor: Color,
    isGlass: Boolean,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glyphCardStyle(isDark = isDark, isGlass = isGlass, cornerRadius = 24f)
            .clickable { onToggle(!checked) }
            .padding(16.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (checked) accentColor.copy(alpha = 0.15f) else (if (isDark) Color(0xFF222222) else Color(0xFFE5E5EA))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (checked) accentColor else (if (isDark) Color.White else Color.Black),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = if (isDark) Color(0xFF2E2E32) else Color(0xFFD1D1D6)
                    )
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isDark) Color.White else Color(0xFF1C1C1E)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF888888) else Color(0xFF6C6C70),
                lineHeight = 14.sp
            )
        }
    }
}

/**
 * Feature Toggle Row
 */
@Composable
fun HighDensityToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    isDark: Boolean,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF222222) else Color(0xFFE5E5EA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (checked) accentColor else (if (isDark) Color.White else Color.Black),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isDark) Color.White else Color(0xFF1C1C1E)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = if (isDark) Color(0xFF888888) else Color(0xFF6C6C70)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = if (isDark) Color(0xFF2E2E32) else Color(0xFFD1D1D6)
            )
        )
    }
}

/**
 * Main Glyph Interface Screen
 */
@Composable
fun GlyphMainScreen(
    isDark: Boolean
) {
    val context = LocalContext.current
    val accentColor = AppSettings.currentAccentColor
    val isGlass = AppSettings.isGlassEffect

    var mainToggle by remember { mutableStateOf(RootUtils.isMainEnabled) }
    var brightness by remember { mutableFloatStateOf(RootUtils.globalBrightness) }
    var autoBrightness by remember { mutableStateOf(NotificationService.autoBrightnessEnabled) }
    var torchToggle by remember { mutableStateOf(NotificationService.isTorchOn) }
    var volumeToggle by remember { mutableStateOf(NotificationService.volumeIndicatorEnabled) }
    var visualizerToggle by remember { mutableStateOf(MusicVisualizerService.isRunning) }
    var musicProgressToggle by remember { mutableStateOf(false) }
    var flipToGlyphToggle by remember { mutableStateOf(NotificationService.flipToGlyphEnabled) }

    DisposableEffect(volumeToggle) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (volumeToggle && (intent?.action == "android.media.VOLUME_CHANGED_ACTION" || intent?.action == "android.media.EXTRA_VOLUME_STREAM_VALUE")) {
                    val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                    val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                    RootUtils.updateVolumeIndicator(currentVolume, maxVolume)
                }
            }
        }
        if (volumeToggle) {
            val filter = IntentFilter().apply {
                addAction("android.media.VOLUME_CHANGED_ACTION")
                addAction("android.media.EXTRA_VOLUME_STREAM_VALUE")
            }
            try {
                ContextCompat.registerReceiver(
                    context,
                    volumeReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } catch (e: Exception) {
                // Fallback for older systems or edge cases
                try {
                    context.registerReceiver(volumeReceiver, filter)
                } catch (_: Exception) {}
            }
        }
        onDispose {
            if (volumeToggle) {
                try { context.unregisterReceiver(volumeReceiver) } catch (e: Exception) {}
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.system_control),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = if (isDark) Color(0xFF888888) else Color(0xFF6C6C70)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.glyph_interface_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-0.5).sp,
                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                    )
                }

                // Master Power Toggle
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (mainToggle) accentColor else (if (isDark) Color(0xFF1A1A1A) else Color(0xFFE5E5EA)))
                        .border(1.dp, if (mainToggle) accentColor else (if (isDark) Color(0xFF333333) else Color(0xFFD1D1D6)), CircleShape)
                        .clickable {
                            val next = !mainToggle
                            mainToggle = next
                            RootUtils.isMainEnabled = next
                            RootUtils.setGlyphOperatingMode(next)
                            if (!next) {
                                RootUtils.clearAllLedsSmoothly()
                            }
                        }
                        .testTag("master_switch"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Toggle Glyph System",
                        tint = if (mainToggle) Color.White else (if (isDark) Color(0xFF777777) else Color(0xFF8E8E93)),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Preview Section
        item {
            GlyphPhonePreview(
                isPoweredOn = mainToggle,
                brightness = brightness,
                isTorchOn = torchToggle,
                isVisualizerOn = visualizerToggle,
                activeTimerSeconds = NotificationService.activeTimerSeconds,
                isDark = isDark,
                accentColor = accentColor,
                isGlass = isGlass
            )
        }

        // Brightness Controller
        item {
            HighDensityBrightnessCard(
                brightness = brightness,
                onBrightnessChange = {
                    brightness = it
                    RootUtils.globalBrightness = it
                    if (mainToggle && torchToggle) {
                        RootUtils.setGlyphBrightness((it * 255).toInt())
                    }
                },
                autoBrightness = autoBrightness,
                onAutoBrightnessChange = {
                    autoBrightness = it
                    NotificationService.autoBrightnessEnabled = it
                },
                isDark = isDark,
                accentColor = accentColor,
                isGlass = isGlass
            )
        }

        // 2x2 Style Quick Action Cards (Glyph Torch & Music Visualizer)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureGridTile(
                    icon = Icons.Default.FlashlightOn,
                    title = stringResource(R.string.feature_torch_title),
                    subtitle = stringResource(R.string.feature_torch_sub),
                    checked = torchToggle,
                    onToggle = {
                        torchToggle = it
                        NotificationService.isTorchOn = it
                        context.startService(Intent(context, NotificationService::class.java))
                        RootUtils.setGlyphBrightness(if (it) (RootUtils.globalBrightness * 255).toInt() else 0)
                    },
                    isDark = isDark,
                    accentColor = accentColor,
                    isGlass = isGlass,
                    testTag = "toggle_torch",
                    modifier = Modifier.weight(1f)
                )

                FeatureGridTile(
                    icon = Icons.Default.GraphicEq,
                    title = stringResource(R.string.feature_visualizer_title),
                    subtitle = stringResource(R.string.feature_visualizer_sub),
                    checked = visualizerToggle,
                    onToggle = {
                        visualizerToggle = it
                        val intent = Intent(context, MusicVisualizerService::class.java)
                        if (it) {
                            context.startService(intent)
                        } else {
                            context.stopService(intent)
                            RootUtils.clearAllLeds()
                        }
                    },
                    isDark = isDark,
                    accentColor = accentColor,
                    isGlass = isGlass,
                    testTag = "toggle_visualizer",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Feature System Toggles
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glyphCardStyle(isDark = isDark, isGlass = isGlass, cornerRadius = 24f)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.hardware_integration),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (isDark) Color(0xFF7E7E82) else Color(0xFF8E8E93),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    HighDensityToggleRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = stringResource(R.string.feature_volume_title),
                        subtitle = stringResource(R.string.feature_volume_sub),
                        checked = volumeToggle,
                        onToggle = {
                            volumeToggle = it
                            NotificationService.volumeIndicatorEnabled = it
                            context.startService(Intent(context, NotificationService::class.java))
                        },
                        isDark = isDark,
                        accentColor = accentColor,
                        testTag = "toggle_volume_level"
                    )

                    HighDensityToggleRow(
                        icon = Icons.Default.MusicNote,
                        title = stringResource(R.string.feature_music_progress_title),
                        subtitle = stringResource(R.string.feature_music_progress_sub),
                        checked = musicProgressToggle,
                        onToggle = { musicProgressToggle = it },
                        isDark = isDark,
                        accentColor = accentColor,
                        testTag = "toggle_music_progress"
                    )

                    HighDensityToggleRow(
                        icon = Icons.Default.ScreenRotation,
                        title = stringResource(R.string.feature_flip_title),
                        subtitle = stringResource(R.string.feature_flip_sub),
                        checked = flipToGlyphToggle,
                        onToggle = {
                            flipToGlyphToggle = it
                            NotificationService.flipToGlyphEnabled = it
                            context.startService(Intent(context, NotificationService::class.java))
                        },
                        isDark = isDark,
                        accentColor = accentColor,
                        testTag = "toggle_flip_to_glyph"
                    )
                }
            }
        }
    }
}

/**
 * Call / Patterns Screen
 */
@Composable
fun GlyphCallScreen(
    isDark: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = AppSettings.currentAccentColor
    val isGlass = AppSettings.isGlassEffect

    val patterns = listOf(
        "Abra", "Anna", "Beetle", "Clwb", "Coded", "Crossing",
        "Dolphin", "Hammer", "Latency", "Plot", "Pneumatic", "Pulse",
        "Radiate", "Ripple", "Squirrels", "Sticks", "Tennis", "Wings",
        "Wizard", "Woo Yeh"
    )

    var selectedPattern by remember { mutableStateOf(patterns[0]) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            if (isPreviewPlaying) {
                RootUtils.stopCallAnimation()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (isPreviewPlaying) {
                        RootUtils.stopCallAnimation()
                        isPreviewPlaying = false
                    }
                    onBack()
                },
                modifier = Modifier.testTag("call_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isDark) Color.White else Color(0xFF1C1C1E)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = stringResource(R.string.ringtone_patterns_header),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = if (isDark) Color(0xFF888888) else Color(0xFF6C6C70)
                )
                Text(
                    text = stringResource(R.string.ringtone_patterns_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Pattern Card & Play Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glyphCardStyle(isDark = isDark, isGlass = isGlass, cornerRadius = 24f)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.current_pattern_label),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF7E7E82) else Color(0xFF8E8E93),
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedPattern,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (isPreviewPlaying) {
                            RootUtils.stopCallAnimation()
                            isPreviewPlaying = false
                        } else {
                            isPreviewPlaying = true
                            RootUtils.playCallAnimation(context, "$selectedPattern.csv")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPreviewPlaying) accentColor else (if (isDark) Color.White else Color.Black)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("preview_pattern_button")
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPreviewPlaying) "Stop" else "Play",
                        tint = if (isPreviewPlaying) Color.White else (if (isDark) Color.Black else Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPreviewPlaying) stringResource(R.string.stop_preview_button) else stringResource(R.string.preview_glyph_button),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isPreviewPlaying) Color.White else (if (isDark) Color.Black else Color.White)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pattern Items List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(patterns) { pattern ->
                val isSelected = pattern == selectedPattern
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glyphCardStyle(
                            isDark = isDark,
                            isGlass = isGlass,
                            cornerRadius = 18f,
                            activeBorderColor = if (isSelected) accentColor else null
                        )
                        .clickable {
                            selectedPattern = pattern
                            if (isPreviewPlaying) {
                                RootUtils.playCallAnimation(context, "$pattern.csv")
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("pattern_item_$pattern")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) accentColor else Color.Transparent)
                                .border(2.dp, if (isSelected) accentColor else (if (isDark) Color(0xFF444444) else Color(0xFFC7C7CC)), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = pattern,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDark) Color.White else Color(0xFF1C1C1E),
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected && isPreviewPlaying) {
                            Text(
                                text = stringResource(R.string.playing_status),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Timer Screen
 */
@Composable
fun GlyphTimerScreen(
    isDark: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = AppSettings.currentAccentColor
    val isGlass = AppSettings.isGlassEffect
    val scope = rememberCoroutineScope()

    var totalDurationSeconds by remember { mutableIntStateOf(60) }
    var remainingSeconds by remember { mutableIntStateOf(60) }
    var isRunning by remember { mutableStateOf(false) }
    var isAlarmPlaying by remember { mutableStateOf(false) }
    var muteAlarm by remember { mutableStateOf(false) }
    var customMinutesInput by remember { mutableStateOf("1") }

    val defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    val ringtone: Ringtone? = remember {
        try { RingtoneManager.getRingtone(context, defaultRingtoneUri) } catch (e: Exception) { null }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isRunning || isAlarmPlaying) {
                isRunning = false
                isAlarmPlaying = false
                NotificationService.activeTimerSeconds = -1
                context.startService(Intent(context, NotificationService::class.java))
                RootUtils.clearAllLedsSmoothly()
                ringtone?.stop()
            }
        }
    }

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
            NotificationService.activeTimerSeconds = remainingSeconds
            context.startService(Intent(context, NotificationService::class.java))

            val progress = remainingSeconds.toFloat() / totalDurationSeconds.coerceAtLeast(1).toFloat()
            RootUtils.updateTimerProgress(progress, isTicking = true)

            if (remainingSeconds <= 0) {
                isRunning = false
                isAlarmPlaying = true
                NotificationService.activeTimerSeconds = -1
                context.startService(Intent(context, NotificationService::class.java))

                if (!muteAlarm) {
                    ringtone?.play()
                }

                RootUtils.blinkTimerFinished()

                scope.launch {
                    while (isActive && isAlarmPlaying) {
                        RootUtils.setGlyphBrightness(255)
                        delay(400)
                        RootUtils.setGlyphBrightness(0)
                        delay(400)
                    }
                    RootUtils.clearAllLedsSmoothly()
                }
            }
        }
    }

    val presets = listOf("0:30" to 30, "1:00" to 60, "5:00" to 300, "10:00" to 600)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (isRunning || isAlarmPlaying) {
                        isRunning = false
                        isAlarmPlaying = false
                        NotificationService.activeTimerSeconds = -1
                        context.startService(Intent(context, NotificationService::class.java))
                        RootUtils.clearAllLedsSmoothly()
                        ringtone?.stop()
                    }
                    onBack()
                },
                modifier = Modifier.testTag("timer_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isDark) Color.White else Color(0xFF1C1C1E)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = stringResource(R.string.glyph_timer_header),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = if (isDark) Color(0xFF888888) else Color(0xFF6C6C70)
                )
                Text(
                    text = stringResource(R.string.feature_timer_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Timer Readout Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glyphCardStyle(isDark = isDark, isGlass = isGlass, cornerRadius = 24f)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val mins = remainingSeconds / 60
                val secs = remainingSeconds % 60
                val timeFormatted = String.format("%02d:%02d", mins, secs)

                Text(
                    text = timeFormatted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 4.sp,
                    color = if (isAlarmPlaying) accentColor else (if (isDark) Color.White else Color(0xFF1C1C1E)),
                    textAlign = TextAlign.Center
                )

                val progress = if (totalDurationSeconds > 0) remainingSeconds.toFloat() / totalDurationSeconds else 0f
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isDark) Color(0xFF252525) else Color(0xFFE5E5EA))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .background(if (isAlarmPlaying) accentColor else (if (isDark) Color.White else accentColor))
                    )
                }

                if (isAlarmPlaying) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isAlarmPlaying = false
                            ringtone?.stop()
                            RootUtils.clearAllLedsSmoothly()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("stop_alarm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Alarm",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.stop_preview_button),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PRESETS",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = if (isDark) Color(0xFF7E7E82) else Color(0xFF8E8E93)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(presets) { (label, duration) ->
                val isSelected = totalDurationSeconds == duration && !isRunning
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) accentColor else (if (isDark) Color(0xFF1A1A1A) else Color(0xFFE5E5EA)))
                        .border(1.dp, if (isSelected) accentColor else (if (isDark) Color(0xFF2E2E32) else Color(0xFFD1D1D6)), RoundedCornerShape(14.dp))
                        .clickable(enabled = !isRunning) {
                            totalDurationSeconds = duration
                            remainingSeconds = duration
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .testTag("preset_$label")
                ) {
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (isSelected) Color.White else (if (isDark) Color.White else Color(0xFF1C1C1E))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Custom Minutes Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = customMinutesInput,
                onValueChange = {
                    if (it.length <= 3 && it.all { char -> char.isDigit() }) {
                        customMinutesInput = it
                        val mins = it.toIntOrNull() ?: 1
                        if (mins > 0 && !isRunning) {
                            totalDurationSeconds = (mins * 60).coerceIn(5, 7200)
                            remainingSeconds = totalDurationSeconds
                        }
                    }
                },
                label = {
                    Text(
                        stringResource(R.string.custom_timer_label),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                enabled = !isRunning,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = if (isDark) Color(0xFF333333) else Color(0xFFD1D1D6),
                    focusedLabelColor = accentColor,
                    unfocusedLabelColor = if (isDark) Color(0xFF888888) else Color(0xFF6C6C70),
                    focusedTextColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                    unfocusedTextColor = if (isDark) Color.White else Color(0xFF1C1C1E)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_timer_input")
            )

            Button(
                onClick = {
                    if (isRunning) {
                        isRunning = false
                        NotificationService.activeTimerSeconds = -1
                        context.startService(Intent(context, NotificationService::class.java))
                        RootUtils.clearAllLedsSmoothly()
                    } else {
                        isRunning = true
                        NotificationService.activeTimerSeconds = remainingSeconds
                        context.startService(Intent(context, NotificationService::class.java))
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) accentColor else (if (isDark) Color.White else Color.Black)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("toggle_timer_run_button")
            ) {
                Text(
                    text = if (isRunning) stringResource(R.string.pause_timer) else stringResource(R.string.start_timer),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) Color.White else (if (isDark) Color.Black else Color.White)
                )
            }
        }
    }
}

/**
 * Settings & Customization Screen
 */
@Composable
fun GlyphSettingsScreen(
    isDark: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = AppSettings.currentAccentColor
    val isGlass = AppSettings.isGlassEffect

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF1C1C1E)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = stringResource(R.string.settings_header),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (isDark) Color(0xFF888888) else Color(0xFF6C6C70)
                    )
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Light,
                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                    )
                }
            }
        }

        // Theme Mode Segmented Controller
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glyphCardStyle(isDark = isDark, isGlass = isGlass, cornerRadius = 24f)
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.theme_appearance_title),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (isDark) Color(0xFF7E7E82) else Color(0xFF8E8E93)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) Color(0xFF222222) else Color(0xFFE5E5EA))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ThemeModeOption(
                            label = stringResource(R.string.theme_mode_system),
                            icon = Icons.Default.SettingsBrightness,
                            isSelected = AppSettings.themeMode == ThemeMode.SYSTEM,
                            accentColor = accentColor,
                            isDark = isDark,
                            onClick = { AppSettings.setTheme(ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeModeOption(
                            label = stringResource(R.string.theme_mode_dark),
                            icon = Icons.Default.DarkMode,
                            isSelected = AppSettings.themeMode == ThemeMode.DARK,
                            accentColor = accentColor,
                            isDark = isDark,
                            onClick = { AppSettings.setTheme(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeModeOption(
                            label = stringResource(R.string.theme_mode_light),
                            icon = Icons.Default.LightMode,
                            isSelected = AppSettings.themeMode == ThemeMode.LIGHT,
                            accentColor = accentColor,
                            isDark = isDark,
                            onClick = { AppSettings.setTheme(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Glassmorphism Effect Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glyphCardStyle(isDark = isDark, isGlass = isGlass, cornerRadius = 24f)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.glass_effect_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color(0xFF1C1C1E)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.glass_effect_sub),
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF888888) else Color(0xFF6C6C70)
                        )
                    }
                    Switch(
                        checked = AppSettings.isGlassEffect,
                        onCheckedChange = { AppSettings.setGlass(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = if (isDark) Color(0xFF2E2E32) else Color(0xFFD1D1D6)
                        ),
                        modifier = Modifier.testTag("toggle_glass_effect")
                    )
                }
            }
        }

        // Monet & Accent Color Picker
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glyphCardStyle(isDark = isDark, isGlass = isGlass, cornerRadius = 24f)
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.monet_color_title),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (isDark) Color(0xFF7E7E82) else Color(0xFF8E8E93)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(AppSettings.accentColors) { index, option ->
                            val isSelected = AppSettings.accentIndex == index
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { AppSettings.setAccent(index) }
                                    .padding(4.dp)
                                    .testTag("accent_color_$index")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(option.color)
                                        .border(
                                            2.5.dp,
                                            if (isSelected) (if (isDark) Color.White else Color.Black) else Color.Transparent,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = if (option.color == Color.White) Color.Black else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = option.name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) (if (isDark) Color.White else Color.Black) else (if (isDark) Color(0xFF888888) else Color(0xFF6C6C70))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Background Wallpaper Selector
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glyphCardStyle(isDark = isDark, isGlass = isGlass, cornerRadius = 24f)
                .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.wallpaper_bg_title),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (isDark) Color(0xFF7E7E82) else Color(0xFF8E8E93)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(AppSettings.wallpapers) { index, wp ->
                            val isSelected = AppSettings.wallpaperIndex == index
                            Box(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else (if (isDark) Color(0xFF1E1E1E) else Color(0xFFF2F2F7)))
                                    .border(
                                        1.5.dp,
                                        if (isSelected) accentColor else (if (isDark) Color(0xFF333333) else Color(0xFFE5E5EA)),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { AppSettings.setWallpaper(index) }
                                    .padding(12.dp)
                                    .testTag("wallpaper_item_$index")
                            ) {
                                Column {
                                    Icon(
                                        imageVector = Icons.Default.Wallpaper,
                                        contentDescription = wp.name,
                                        tint = if (isSelected) accentColor else (if (isDark) Color.White else Color.Black),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = wp.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = wp.description,
                                        fontSize = 9.sp,
                                        color = if (isDark) Color(0xFF888888) else Color(0xFF6C6C70),
                                        lineHeight = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hardware Diagnostics & Root Verification
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glyphCardStyle(isDark = isDark, isGlass = isGlass, cornerRadius = 24f)
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.hardware_diagnostics_title),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (isDark) Color(0xFF7E7E82) else Color(0xFF8E8E93)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.root_status_label),
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF888888) else Color(0xFF6C6C70)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF00FF41).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.root_active_msg),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF41)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.sysfs_node_label),
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF888888) else Color(0xFF6C6C70)
                        )
                        Text(
                            text = stringResource(R.string.sysfs_node_path),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Test Hardware Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                RootUtils.blinkNotification(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Flash", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = {
                                RootUtils.clearAllLedsSmoothly()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF222222) else Color(0xFFE5E5EA)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear LEDs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeModeOption(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accentColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else (if (isDark) Color(0xFF888888) else Color(0xFF6C6C70)),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else (if (isDark) Color(0xFF888888) else Color(0xFF6C6C70))
            )
        }
    }
}
