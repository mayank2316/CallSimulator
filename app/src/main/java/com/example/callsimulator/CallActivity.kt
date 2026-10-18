package com.example.callsimulator

import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.ImageView
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay


class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val callId = intent.getStringExtra("call_id").orEmpty()
            val caller = intent.getStringExtra("name") ?: "Unknown caller"
            val number = intent.getStringExtra("number").orEmpty()
            val ringtoneUri = intent.getStringExtra("ringtone").orEmpty()
            val photoUri = intent.getStringExtra("photoUri").orEmpty()
            var state by remember { mutableStateOf("ringing") }
            var muted by remember { mutableStateOf(false) }
            var speaker by remember { mutableStateOf(true) }
            var held by remember { mutableStateOf(false) }
            var keypad by remember { mutableStateOf(false) }
            var addCall by remember { mutableStateOf(false) }
            var startedAt by remember { mutableLongStateOf(0L) }
            var elapsed by remember { mutableLongStateOf(0L) }
            var ringtone by remember { mutableStateOf<Ringtone?>(null) }

            DisposableEffect(state, ringtoneUri) {
                if (state == "ringing") {
                    val uri = ringtoneUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ringtone = runCatching { RingtoneManager.getRingtone(this@CallActivity, uri) }.getOrNull()
                    ringtone?.audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build()
                    runCatching { ringtone?.play() }
                } else ringtone?.stop()
                onDispose { ringtone?.stop(); ringtone = null }
            }
            LaunchedEffect(state) {
                while (state == "connected") {
                    if (!held) elapsed = (SystemClock.elapsedRealtime() - startedAt) / 1000
                    delay(1000)
                }
            }
            fun finish(status: String) {
                ringtone?.stop()
                if (callId.isNotBlank()) CallStore.updateStatus(this@CallActivity, callId, status)
                finishAndRemoveTask()
            }
            val darkMode = isSystemInDarkTheme()
            val colors = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) { if (darkMode) dynamicDarkColorScheme(this@CallActivity) else dynamicLightColorScheme(this@CallActivity) } else if (darkMode) darkColorScheme() else lightColorScheme()
            val bg = colors.background
            val fg = colors.onBackground
            val secondary = colors.onSurfaceVariant
            SideEffect { window.statusBarColor = bg.toArgb(); window.navigationBarColor = bg.toArgb(); androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !darkMode; androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !darkMode }
            MaterialTheme(colorScheme = colors) {
                Box(Modifier.fillMaxSize().background(bg).padding(horizontal = 24.dp, vertical = 18.dp)) {
                    when (state) {
                        "ringing" -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(Modifier.height(72.dp))
                            Text(caller.trim(), color = fg, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Normal, maxLines = 4, softWrap = true, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text(if (number.isBlank()) "Mobile" else number, color = secondary, fontSize = 19.sp, maxLines = 1)
                            Spacer(Modifier.weight(0.6f))
                            CallerAvatar(photoUri, 236)
                            Spacer(Modifier.weight(0.55f))
                            Surface(shape = RoundedCornerShape(40.dp), color = colors.surfaceVariant) { Row(Modifier.padding(horizontal = 22.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Rounded.Message, null, tint = colors.primary); Text("Message", color = fg, fontSize = 16.sp, fontWeight = FontWeight.Medium) } }
                            Spacer(Modifier.weight(0.55f))
                            var sliderOffset by remember { mutableFloatStateOf(0f) }
                            var sliderWidthPx by remember { mutableFloatStateOf(0f) }
                            val density = androidx.compose.ui.platform.LocalDensity.current
                            val sliderMaxPx = with(density) { (maxOf(0.dp,  (350.dp - 104.dp))).toPx() }
                            Surface(modifier = Modifier.fillMaxWidth().height(112.dp), shape = RoundedCornerShape(64.dp), color = colors.surfaceVariant) {
                                BoxWithConstraints(Modifier.fillMaxSize()) {
                                    val maxDragDp = (maxWidth - 104.dp).coerceAtLeast(0.dp)
                                    val maxDragPx = with(density) { maxDragDp.toPx() }
                                    Row(Modifier.fillMaxSize().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Decline", color = if (sliderOffset < -maxDragPx * 0.12f) Color(0xFFE76D6D) else fg, fontSize = 18.sp, maxLines = 1)
                                        Text("Answer", color = if (sliderOffset > maxDragPx * 0.12f) Color(0xFF55BA7A) else fg, fontSize = 18.sp, maxLines = 1)
                                    }
                                    FilledIconButton(
                                        onClick = { },
                                        modifier = Modifier.align(Alignment.Center).offset(x = with(density) { sliderOffset.toDp() }).size(92.dp).pointerInput(maxDragPx) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    when {
                                                        sliderOffset <= -maxDragPx * 0.55f -> finish("Rejected")
                                                        sliderOffset >= maxDragPx * 0.55f -> { ringtone?.stop(); startedAt = SystemClock.elapsedRealtime(); state = "connected"; if (callId.isNotBlank()) CallStore.updateStatus(this@CallActivity, callId, "Accepted") }
                                                    }
                                                    sliderOffset = 0f
                                                },
                                                onDragCancel = { sliderOffset = 0f },
                                                onDrag = { change, dragAmount -> change.consume(); sliderOffset = (sliderOffset + dragAmount.x).coerceIn(-maxDragPx, maxDragPx) }
                                            )
                                        },
                                        shape = RoundedCornerShape(42.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = if (sliderOffset < 0) Color(0xFFE76D6D) else Color(0xFF50B879))
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            if (photoUri.isNotBlank()) {
                                                AndroidView(factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setImageBitmap(loadCallerPhoto(context, photoUri)) } }, update = { view -> view.setImageBitmap(loadCallerPhoto(view.context, photoUri)) }, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                                val progress = (kotlin.math.abs(sliderOffset) / maxDragPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                                                Icon(Icons.Rounded.Call, "Swipe left to decline or right to answer", modifier = Modifier.size(26.dp).rotate(if (sliderOffset < 0f) -90f * progress else 90f * progress), tint = Color(0xFF50B879))
                                            } else {
                                                Icon(if (sliderOffset < -maxDragPx * 0.12f) Icons.Rounded.CallEnd else Icons.Rounded.Call, "Swipe left to decline or right to answer", modifier = Modifier.size(34.dp).rotate(if (sliderOffset < 0f) -90f else 0f), tint = if (sliderOffset < -maxDragPx * 0.12f) Color(0xFFE76D6D) else Color(0xFF50B879))
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(60.dp))
                        }
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Spacer(Modifier.height(18.dp))
                            CallerAvatar(photoUri, 108)
                            Text(caller, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (held) "On hold" else "%02d:%02d".format(elapsed / 60, elapsed % 60), color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
                            if (number.isNotBlank()) Text(number, color = Color(0xFFB8C4CC), fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            if (keypad) KeypadPanel()
                            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    CallControl("Mute", muted, Icons.Rounded.MicOff) { muted = !muted }
                                    CallControl("Speaker", speaker, Icons.Rounded.VolumeUp) { speaker = !speaker }
                                    CallControl("Keypad", keypad, Icons.Rounded.Dialpad) { keypad = !keypad }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    CallControl(if (held) "Resume" else "Hold", held, Icons.Rounded.Pause) { held = !held }
                                    CallControl("Add call", addCall, Icons.Rounded.PersonAdd) { addCall = !addCall }
                                }
                            }
                            if (addCall) Text("Add call is a visual simulation only", color = Color(0xFF80909B), fontSize = 11.sp)
                            Spacer(Modifier.height(10.dp))
                            FilledIconButton(onClick = { finish("Completed") }, modifier = Modifier.size(72.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE64F5D))) {
                                Icon(Icons.Rounded.CallEnd, "End simulated call", modifier = Modifier.size(32.dp))
                            }
                            Text("End call", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallerAvatar(photoUri: String, diameter: Int) {
    Box(Modifier.size(diameter.dp).clip(CircleShape).background(Color(0xFF263D4A)), contentAlignment = Alignment.Center) {
        if (photoUri.isNotBlank()) {
            AndroidView(factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageBitmap(loadCallerPhoto(context, photoUri))
                }
            }, update = { view -> view.setImageBitmap(loadCallerPhoto(view.context, photoUri)) }, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size((diameter * 0.52f).dp))
        }
    }
}

@Composable
private fun CallControl(label: String, active: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(84.dp)) {
        FilledIconButton(onClick = onClick, modifier = Modifier.size(60.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (active) MaterialTheme.colorScheme.primary else Color(0xFF30404B), contentColor = if (active) Color(0xFF102019) else Color.White)) {
            Icon(icon, label, modifier = Modifier.size(23.dp))
        }
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun KeypadPanel() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        listOf("1     2     3", "4     5     6", "7     8     9", "*     0     #").forEach { row ->
            Text(row, color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(5.dp))
        }
    }
}


private fun loadCallerPhoto(context: Context, value: String): Bitmap? {
    if (value.isBlank()) return null
    return runCatching {
        context.contentResolver.openInputStream(Uri.parse(value))?.use { stream -> BitmapFactory.decodeStream(stream) }
    }.getOrNull()
}
