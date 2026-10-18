package com.example.callsimulator

import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

private val CallBg = Color(0xFF101A22)
private val CallMint = Color(0xFF9CECCB)

class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = android.graphics.Color.rgb(16, 26, 34)
        window.navigationBarColor = android.graphics.Color.rgb(16, 26, 34)
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
            MaterialTheme(colorScheme = darkColorScheme(primary = CallMint, background = CallBg, surface = Color(0xFF1B2A34))) {
                Box(Modifier.fillMaxSize().background(CallBg).padding(horizontal = 24.dp, vertical = 28.dp), contentAlignment = Alignment.Center) {
                    when (state) {
                        "ringing" -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Spacer(Modifier.height(16.dp))
                            Text("INCOMING CALL", color = CallMint, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(28.dp))
                            CallerAvatar(photoUri, 132)
                            Text(caller, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.SemiBold)
                            Text(number.ifBlank { "Mobile · simulated call" }, color = Color(0xFFB8C4CC), fontSize = 15.sp)
                            Spacer(Modifier.weight(1f))
                            Text("Swipe into the moment", color = Color(0xFF80909B), fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FilledIconButton(onClick = { finish("Rejected") }, modifier = Modifier.size(76.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE64F5D))) {
                                        Icon(Icons.Rounded.CallEnd, "Decline", modifier = Modifier.size(32.dp))
                                    }
                                    Text("Decline", color = Color.White, fontSize = 13.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FilledIconButton(onClick = { ringtone?.stop(); startedAt = SystemClock.elapsedRealtime(); state = "connected"; if (callId.isNotBlank()) CallStore.updateStatus(this@CallActivity, callId, "Accepted") }, modifier = Modifier.size(76.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF25C477))) {
                                        Icon(Icons.Rounded.Call, "Accept", modifier = Modifier.size(32.dp))
                                    }
                                    Text("Accept", color = Color.White, fontSize = 13.sp)
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("SIMULATED CALL", color = CallMint, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            CallerAvatar(photoUri, 108)
                            Text(caller, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (held) "On hold" else "%02d:%02d".format(elapsed / 60, elapsed % 60), color = CallMint, fontSize = 17.sp)
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
                    setImageURI(runCatching { Uri.parse(photoUri) }.getOrNull())
                }
            }, update = { view -> runCatching { view.setImageURI(Uri.parse(photoUri)) } }, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size((diameter * 0.52f).dp))
        }
    }
}

@Composable
private fun CallControl(label: String, active: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(84.dp)) {
        FilledIconButton(onClick = onClick, modifier = Modifier.size(60.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (active) CallMint else Color(0xFF30404B), contentColor = if (active) Color(0xFF102019) else Color.White)) {
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
