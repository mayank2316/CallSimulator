package com.example.callsimulator

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    ringtone?.play()
                } else ringtone?.stop()
                onDispose { ringtone?.stop(); ringtone = null }
            }
            LaunchedEffect(state) {
                while (state == "connected") {
                    elapsed = (SystemClock.elapsedRealtime() - startedAt) / 1000
                    delay(1000)
                }
            }
            fun finish(status: String) {
                ringtone?.stop()
                if (callId.isNotBlank()) CallStore.updateStatus(this@CallActivity, callId, status)
                state = "ended"
            }
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize().background(Color(0xFF101A22)).padding(24.dp), contentAlignment = Alignment.Center) {
                    when (state) {
                        "ringing" -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("INCOMING SIMULATED CALL", color = Color(0xFF9CECCB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(28.dp))
                            Box(Modifier.size(118.dp).background(Color(0xFF263D4A), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(64.dp))
                            }
                            Text(caller, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Medium)
                            if (number.isNotBlank()) Text(number, color = Color.LightGray, fontSize = 16.sp)
                            Text("mobile · simulated call", color = Color.LightGray, fontSize = 13.sp)
                            Spacer(Modifier.height(76.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    FilledIconButton(onClick = { finish("Rejected") }, modifier = Modifier.size(70.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE95757))) {
                                        Icon(Icons.Rounded.CallEnd, "Reject", modifier = Modifier.size(30.dp))
                                    }
                                    Text("Decline", color = Color.White, fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    FilledIconButton(onClick = { ringtone?.stop(); startedAt = SystemClock.elapsedRealtime(); state = "connected"; if (callId.isNotBlank()) CallStore.updateStatus(this@CallActivity, callId, "Accepted") }, modifier = Modifier.size(70.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2FC77A))) {
                                        Icon(Icons.Rounded.Call, "Accept", modifier = Modifier.size(30.dp))
                                    }
                                    Text("Accept", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                        "connected" -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Box(Modifier.size(100.dp).background(Color(0xFF263D4A), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(54.dp))
                            }
                            Text(caller, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                            Text(if (held) "On hold" else "%02d:%02d".format(elapsed / 60, elapsed % 60), color = Color(0xFF9CECCB), fontSize = 16.sp)
                            if (addCall) Text("Add call is a visual demo only", color = Color.LightGray, fontSize = 12.sp)
                            if (keypad) KeypadPanel()
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    CallControl("Mute", muted, Icons.Rounded.MicOff) { muted = !muted }
                                    CallControl("Speaker", speaker, Icons.Rounded.VolumeUp) { speaker = !speaker }
                                    CallControl("Keypad", keypad, Icons.Rounded.Dialpad) { keypad = !keypad }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    CallControl(if (held) "Resume" else "Hold", held, Icons.Rounded.Pause) { held = !held }
                                    CallControl("Add call", addCall, Icons.Rounded.PersonAdd) { addCall = !addCall }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            FilledIconButton(onClick = { finish("Accepted") }, modifier = Modifier.size(68.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE95757))) {
                                Icon(Icons.Rounded.CallEnd, "End simulated call", modifier = Modifier.size(30.dp))
                            }
                            Text("End call", color = Color.White, fontSize = 12.sp)
                        }
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF9CECCB), modifier = Modifier.size(62.dp))
                            Text(if (callId.isNotBlank() && CallStore.all(this@CallActivity).firstOrNull { it.id == callId }?.status == "Rejected") "Call declined" else "Simulation ended", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            Text("No real call was made.", color = Color.LightGray)
                            Button(onClick = { finishAndRemoveTask() }) { Text("Done") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControl(label: String, active: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(84.dp)) {
        FilledIconButton(onClick = onClick, modifier = Modifier.size(58.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (active) Color(0xFF9CECCB) else Color(0xFF30404B), contentColor = if (active) Color(0xFF102019) else Color.White)) {
            Icon(icon, label)
        }
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun KeypadPanel() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        listOf("1  2  3", "4  5  6", "7  8  9", "*  0  #").forEach { row ->
            Text(row, color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(5.dp))
        }
    }
}
