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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.glyphinterface.R
import com.glyphinterface.ui.theme.GlyphCardBorderDark
import com.glyphinterface.ui.theme.GlyphCardBorderSubtle
import com.glyphinterface.ui.theme.GlyphCardDark
import com.glyphinterface.ui.theme.GlyphCardSubtle
import com.glyphinterface.ui.theme.GlyphDarkBg
import com.glyphinterface.ui.theme.GlyphInterfaceTheme
import com.glyphinterface.ui.theme.GlyphPhoneBorder
import com.glyphinterface.ui.theme.GlyphPhoneFrame
import com.glyphinterface.ui.theme.GlyphRed
import com.glyphinterface.ui.theme.GlyphSliderBg
import com.glyphinterface.ui.theme.GlyphStatusGreen
import com.glyphinterface.ui.theme.GlyphSurfaceDark
import com.glyphinterface.ui.theme.GlyphTextMuted
import com.glyphinterface.ui.theme.GlyphTextPrimaryDark
import com.glyphinterface.ui.theme.GlyphTextSecondaryDark
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
                var currentScreen by remember { mutableStateOf(Screen.Main) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GlyphDarkBg
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(GlyphDarkBg)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (currentScreen) {
                                Screen.Main -> {
                                    GlyphMainScreen(
                                        onNavigateToTimer = { currentScreen = Screen.Timer },
                                        onNavigateToCall = { currentScreen = Screen.Call }
                                    )
                                }
                                Screen.Timer -> {
                                    GlyphTimerScreen(
                                        onBack = { currentScreen = Screen.Main }
                                    )
                                }
                                Screen.Call -> {
                                    GlyphCallScreen(
                                        onBack = { currentScreen = Screen.Main }
                                    )
                                }
                            }
                        }

                        // Bottom Navigation Bar
                        HighDensityBottomBar(
                            currentScreen = currentScreen,
                            onSelectScreen = { currentScreen = it }
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
 * Dot Matrix Canvas overlay
 */
@Composable
fun DotMatrixGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val dotSpacing = 16.dp.toPx()
        val dotRadius = 1.dp.toPx()
        val numCols = (size.width / dotSpacing).toInt() + 1
        val numRows = (size.height / dotSpacing).toInt() + 1

        for (i in 0..numCols) {
            for (j in 0..numRows) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = dotRadius,
                    center = Offset(i * dotSpacing, j * dotSpacing)
                )
            }
        }
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
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(GlyphSurfaceDark)
            .border(1.dp, GlyphCardBorderSubtle, RoundedCornerShape(32.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        DotMatrixGrid(modifier = Modifier.matchParentSize())

        // Phone Frame Representation
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(230.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(GlyphPhoneFrame)
                .border(3.dp, GlyphPhoneBorder, RoundedCornerShape(36.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridAlpha = 0.04f
                val step = 12.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(Color.White.copy(alpha = gridAlpha), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(Color.White.copy(alpha = gridAlpha), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            }

            // Top-left Red Recording/Status Dot
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 4.dp, start = 8.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isPoweredOn) GlyphRed else Color(0xFF441010))
                    .border(1.dp, if (isPoweredOn) GlyphRed.copy(alpha = 0.6f) else Color.Transparent, CircleShape)
            )

            // Top Right Glyph LED
            val topLedAlpha = if (!isPoweredOn) 0.1f else if (isTorchOn) 1f else brightness.coerceIn(0.2f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 12.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = topLedAlpha))
                    .then(
                        if (isPoweredOn && topLedAlpha > 0.4f) {
                            Modifier.shadow(8.dp, RoundedCornerShape(3.dp), ambientColor = Color.White, spotColor = Color.White)
                        } else Modifier
                    )
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
                                color = Color.White.copy(alpha = ringLedAlpha * 0.15f),
                                radius = size.minDimension / 2
                            )
                        }
                    }
                }
            }

            // Bottom Vertical Glyph LED
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
                color = GlyphTextMuted,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            )
            Text(
                text = if (isPoweredOn) stringResource(R.string.status_active_link) else stringResource(R.string.status_offline),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPoweredOn) GlyphStatusGreen else GlyphTextMuted,
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
    modifier: Modifier = Modifier
) {
    var cardWidthPx by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlyphCardDark)
            .border(1.dp, GlyphCardBorderDark, RoundedCornerShape(24.dp))
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
                    color = GlyphTextSecondaryDark
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${(brightness * 100).toInt()}%",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlyphTextPrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Slider Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlyphSliderBg)
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
                        .background(Color.White.copy(alpha = 0.92f))
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
                        tint = if (brightness > 0.15f) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Max Brightness",
                        tint = if (brightness > 0.88f) Color.Black else Color.White,
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
                    color = GlyphTextSecondaryDark,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = autoBrightness,
                    onCheckedChange = onAutoBrightnessChange,
                    modifier = Modifier.testTag("toggle_auto_brightness"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GlyphRed,
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = GlyphSliderBg
                    )
                )
            }
        }
    }
}

/**
 * Grid Action Card
 */
@Composable
fun HighDensityActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean = false,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(GlyphCardDark)
            .border(
                1.dp,
                if (isActive) GlyphRed.copy(alpha = 0.5f) else GlyphCardBorderDark,
                RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isActive) GlyphRed else GlyphSliderBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(GlyphRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlyphTextPrimaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = GlyphTextSecondaryDark,
                    lineHeight = 13.sp
                )
            }
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
                .background(GlyphSliderBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (checked) GlyphRed else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = GlyphTextPrimaryDark
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = GlyphTextSecondaryDark
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GlyphRed,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = GlyphSliderBg
            )
        )
    }
}

/**
 * Bottom Navigation Bar
 */
@Composable
fun HighDensityBottomBar(
    currentScreen: Screen,
    onSelectScreen: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .border(width = 1.dp, color = Color(0xFF1A1A1A))
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visuals Tab
            Column(
                modifier = Modifier
                    .clickable { onSelectScreen(Screen.Main) }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isSelected = currentScreen == Screen.Main
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (isSelected) Color.White else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrightnessMedium,
                        contentDescription = stringResource(R.string.nav_visuals),
                        tint = if (isSelected) Color.Black else GlyphTextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.nav_visuals),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else GlyphTextSecondaryDark
                )
            }

            // Patterns Tab
            Column(
                modifier = Modifier
                    .clickable { onSelectScreen(Screen.Call) }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isSelected = currentScreen == Screen.Call
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (isSelected) Color.White else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = stringResource(R.string.nav_patterns),
                        tint = if (isSelected) Color.Black else GlyphTextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.nav_patterns),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else GlyphTextSecondaryDark
                )
            }

            // Timer Tab
            Column(
                modifier = Modifier
                    .clickable { onSelectScreen(Screen.Timer) }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isSelected = currentScreen == Screen.Timer
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (isSelected) Color.White else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = stringResource(R.string.nav_timer),
                        tint = if (isSelected) Color.Black else GlyphTextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.nav_timer),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else GlyphTextSecondaryDark
                )
            }
        }
    }
}

/**
 * Main Glyph Interface Screen
 */
@Composable
fun GlyphMainScreen(
    onNavigateToTimer: () -> Unit,
    onNavigateToCall: () -> Unit
) {
    val context = LocalContext.current

    var mainToggle by remember { mutableStateOf(RootUtils.isMainEnabled) }
    var brightness by remember { mutableFloatStateOf(RootUtils.globalBrightness) }
    var autoBrightness by remember { mutableStateOf(NotificationService.autoBrightnessEnabled) }
    var torchToggle by remember { mutableStateOf(NotificationService.isTorchOn) }
    var volumeToggle by remember { mutableStateOf(true) }
    var visualizerToggle by remember { mutableStateOf(MusicVisualizerService.isRunning) }
    var musicProgressToggle by remember { mutableStateOf(false) }
    var flipToGlyphToggle by remember { mutableStateOf(NotificationService.flipToGlyphEnabled) }

    DisposableEffect(volumeToggle) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (volumeToggle && intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                    RootUtils.updateVolumeIndicator(currentVolume)
                }
            }
        }
        if (volumeToggle) {
            context.registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
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
                        color = GlyphTextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.glyph_interface_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-0.5).sp,
                        color = GlyphTextPrimaryDark
                    )
                }

                // Master Power Toggle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (mainToggle) GlyphCardSubtle else GlyphCardDark)
                        .border(1.dp, if (mainToggle) GlyphRed.copy(alpha = 0.5f) else GlyphCardBorderDark, CircleShape)
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
                        tint = if (mainToggle) GlyphRed else GlyphTextMuted,
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
                activeTimerSeconds = NotificationService.activeTimerSeconds
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
                }
            )
        }

        // 2-Column Action Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HighDensityActionCard(
                    icon = Icons.Default.NotificationsActive,
                    title = stringResource(R.string.feature_essential_title),
                    subtitle = stringResource(R.string.feature_essential_sub),
                    onClick = onNavigateToCall,
                    testTag = "action_patterns",
                    modifier = Modifier.weight(1f)
                )

                HighDensityActionCard(
                    icon = Icons.Default.HourglassBottom,
                    title = stringResource(R.string.feature_timer_title),
                    subtitle = stringResource(R.string.feature_timer_sub),
                    isActive = NotificationService.activeTimerSeconds >= 0,
                    onClick = onNavigateToTimer,
                    testTag = "action_timer",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Feature System Toggles
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlyphCardDark)
                    .border(1.dp, GlyphCardBorderDark, RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.hardware_integration),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = GlyphTextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    HighDensityToggleRow(
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
                        testTag = "toggle_torch"
                    )

                    HighDensityToggleRow(
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
                        testTag = "toggle_visualizer"
                    )

                    HighDensityToggleRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = stringResource(R.string.feature_volume_title),
                        subtitle = stringResource(R.string.feature_volume_sub),
                        checked = volumeToggle,
                        onToggle = { volumeToggle = it },
                        testTag = "toggle_volume_level"
                    )

                    HighDensityToggleRow(
                        icon = Icons.Default.MusicNote,
                        title = stringResource(R.string.feature_music_progress_title),
                        subtitle = stringResource(R.string.feature_music_progress_sub),
                        checked = musicProgressToggle,
                        onToggle = { musicProgressToggle = it },
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
                        },
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
    onBack: () -> Unit
) {
    val context = LocalContext.current

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
                    tint = GlyphTextPrimaryDark
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
                    color = GlyphTextSecondaryDark
                )
                Text(
                    text = stringResource(R.string.ringtone_patterns_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = GlyphTextPrimaryDark
                )
            }
        }

        // Preview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlyphCardDark)
                .border(1.dp, GlyphCardBorderDark, RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.current_pattern_label),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = GlyphTextMuted,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedPattern,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlyphTextPrimaryDark
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
                        containerColor = if (isPreviewPlaying) GlyphRed else Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("preview_pattern_button")
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPreviewPlaying) "Stop" else "Play",
                        tint = if (isPreviewPlaying) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPreviewPlaying) stringResource(R.string.stop_preview_button) else stringResource(R.string.preview_glyph_button),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isPreviewPlaying) Color.White else Color.Black
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
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) GlyphCardSubtle else GlyphCardDark)
                        .border(
                            1.dp,
                            if (isSelected) GlyphRed.copy(alpha = 0.6f) else GlyphCardBorderDark,
                            RoundedCornerShape(18.dp)
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
                                .background(if (isSelected) GlyphRed else Color.Transparent)
                                .border(2.dp, if (isSelected) GlyphRed else Color(0xFF444444), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = pattern,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = GlyphTextPrimaryDark,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected && isPreviewPlaying) {
                            Text(
                                text = stringResource(R.string.playing_status),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlyphRed
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
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
            RootUtils.updateTimerIndicator(progress)

            if (remainingSeconds <= 0) {
                isRunning = false
                isAlarmPlaying = true
                NotificationService.activeTimerSeconds = -1
                context.startService(Intent(context, NotificationService::class.java))

                if (!muteAlarm) {
                    ringtone?.play()
                }

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
                    tint = GlyphTextPrimaryDark
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
                    color = GlyphTextSecondaryDark
                )
                Text(
                    text = stringResource(R.string.feature_timer_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = GlyphTextPrimaryDark
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Timer Readout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlyphCardDark)
                .border(1.dp, GlyphCardBorderDark, RoundedCornerShape(24.dp))
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
                    color = if (isAlarmPlaying) GlyphRed else GlyphTextPrimaryDark,
                    textAlign = TextAlign.Center
                )

                val progress = if (totalDurationSeconds > 0) remainingSeconds.toFloat() / totalDurationSeconds else 0f
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(GlyphSliderBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .background(if (isAlarmPlaying) GlyphRed else Color.White)
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
                        colors = ButtonDefaults.buttonColors(containerColor = GlyphRed),
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
            color = GlyphTextMuted
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
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color.White else GlyphCardSubtle)
                        .border(
                            1.dp,
                            if (isSelected) Color.White else GlyphCardBorderDark,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = !isRunning) {
                            totalDurationSeconds = duration
                            remainingSeconds = duration
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .testTag("preset_$label")
                ) {
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isRunning && !isAlarmPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlyphCardDark)
                    .border(1.dp, GlyphCardBorderDark, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Duration (min):",
                        fontSize = 14.sp,
                        color = GlyphTextSecondaryDark,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customMinutesInput,
                        onValueChange = { input ->
                            if (input.length <= 3 && input.all { it.isDigit() }) {
                                customMinutesInput = input
                                val mins = input.toIntOrNull() ?: 1
                                val clampedMins = mins.coerceIn(1, 180)
                                totalDurationSeconds = clampedMins * 60
                                remainingSeconds = totalDurationSeconds
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .width(80.dp)
                            .testTag("custom_minutes_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlyphRed,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedTextColor = GlyphTextPrimaryDark,
                            unfocusedTextColor = GlyphTextPrimaryDark
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Action Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isRunning) {
                Button(
                    onClick = {
                        if (remainingSeconds <= 0) {
                            remainingSeconds = totalDurationSeconds
                        }
                        isRunning = true
                        isAlarmPlaying = false
                        NotificationService.activeTimerSeconds = remainingSeconds
                        context.startService(Intent(context, NotificationService::class.java))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("start_timer_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.start_timer),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            } else {
                Button(
                    onClick = {
                        isRunning = false
                        NotificationService.activeTimerSeconds = -1
                        context.startService(Intent(context, NotificationService::class.java))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlyphSliderBg),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("pause_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.pause_timer),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Button(
                onClick = {
                    isRunning = false
                    isAlarmPlaying = false
                    remainingSeconds = totalDurationSeconds
                    NotificationService.activeTimerSeconds = -1
                    context.startService(Intent(context, NotificationService::class.java))
                    RootUtils.clearAllLedsSmoothly()
                    ringtone?.stop()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlyphCardSubtle),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .border(1.dp, GlyphCardBorderDark, RoundedCornerShape(20.dp))
                    .testTag("reset_timer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Reset",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    stringResource(R.string.reset_timer),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mute option
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(GlyphCardDark)
                .border(1.dp, GlyphCardBorderDark, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            HighDensityToggleRow(
                icon = Icons.Default.Timer,
                title = "Mute Audio Alarm",
                subtitle = "Only use Glyph flashing notification",
                checked = muteAlarm,
                onToggle = { muteAlarm = it },
                testTag = "toggle_mute_alarm"
            )
        }
    }
}
