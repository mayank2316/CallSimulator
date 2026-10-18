package com.example.callsimulator

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CallSimulatorApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallSimulatorApp() {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var ringtoneUri by remember { mutableStateOf(CallStore.setting(context, "ringtone")) }
    var vibration by remember { mutableStateOf(CallStore.setting(context, "vibration", "true") == "true") }
    var volume by remember { mutableFloatStateOf(CallStore.setting(context, "volume", "0.8").toFloatOrNull() ?: 0.8f) }
    var scheduledAt by remember { mutableLongStateOf(System.currentTimeMillis() + 5 * 60_000L) }
    var calls by remember { mutableStateOf(CallStore.all(context).sortedBy { it.timeMillis }) }
    var showSettings by remember { mutableStateOf(false) }
    var previewRingtone by remember { mutableStateOf<Ringtone?>(null) }
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                    if (c.moveToFirst()) name = c.getString(0) ?: name
                }
            }
        }
    }
    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            ringtoneUri = uri?.toString().orEmpty()
            CallStore.setSetting(context, "ringtone", ringtoneUri)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF9CECCB), secondary = Color(0xFF82D9B4),
        background = Color(0xFF0D1412), surface = Color(0xFF17211E)
    )) {
        Scaffold(containerColor = Color(0xFF0D1412), topBar = {
            TopAppBar(
                title = { Column { Text("Call Simulator", fontWeight = FontWeight.Bold); Text("A little escape, on your schedule", fontSize = 12.sp, color = Color.LightGray) } },
                actions = { IconButton(onClick = { showSettings = !showSettings }) { Icon(Icons.Rounded.Settings, "Settings") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1412), titleContentColor = Color.White)
            )
        }) { padding ->
            if (showSettings) {
                Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Ringtone volume", color = Color.LightGray)
                    Slider(value = volume, onValueChange = { volume = it; CallStore.setSetting(context, "volume", it.toString()) })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vibration", color = Color.White, modifier = Modifier.weight(1f))
                        Switch(checked = vibration, onCheckedChange = { vibration = it; CallStore.setSetting(context, "vibration", it.toString()) })
                    }
                    Text("About", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("This app only simulates calls locally. It never calls anyone, accesses the microphone, or connects to a phone network.", color = Color.LightGray)
                    Button(onClick = { showSettings = false }) { Text("Back to home") }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(Modifier.fillMaxWidth().background(Color(0xFF17211E), RoundedCornerShape(26.dp)).padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(48.dp).background(Color(0xFF263D34), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Call, null, tint = Color(0xFF9CECCB), modifier = Modifier.size(25.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Your next exit, planned.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text("Create a convincing simulated call.", color = Color.LightGray, fontSize = 13.sp)
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Caller name") }, placeholder = { Text("e.g. Alex Morgan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Optional phone number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI))
                            }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.PersonSearch, null); Spacer(Modifier.width(8.dp)); Text("Choose from contacts")
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("WHEN SHOULD IT RING?", fontSize = 11.sp, color = Color(0xFF9CECCB), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = scheduledAt }
                                DatePickerDialog(context, { _, y, m, d ->
                                    val chosen = Calendar.getInstance().apply { timeInMillis = scheduledAt; set(y, m, d) }
                                    TimePickerDialog(context, { _, h, min -> chosen.set(Calendar.HOUR_OF_DAY, h); chosen.set(Calendar.MINUTE, min); chosen.set(Calendar.SECOND, 0); scheduledAt = chosen.timeInMillis }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.CalendarMonth, null); Spacer(Modifier.width(8.dp))
                                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(scheduledAt)))
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose simulated call ringtone")
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri.takeIf { it.isNotBlank() }?.let(Uri::parse))
                                ringtonePicker.launch(intent)
                            }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.MusicNote, null); Spacer(Modifier.width(8.dp)); Text(if (ringtoneUri.isBlank()) "Choose ringtone (default)" else "Ringtone selected")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Vibrate", color = Color.White, modifier = Modifier.weight(1f))
                                Switch(checked = vibration, onCheckedChange = { vibration = it; CallStore.setSetting(context, "vibration", it.toString()) })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = {
                                    if (name.isBlank()) return@Button
                                    val i = Intent(context, CallActivity::class.java).putExtra("name", name.trim()).putExtra("number", number.trim()).putExtra("ringtone", ringtoneUri).putExtra("vibration", vibration)
                                    context.startActivity(i)
                                }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9CECCB), contentColor = Color(0xFF102019))) {
                                    Icon(Icons.Rounded.Call, null); Spacer(Modifier.width(6.dp)); Text("Call now")
                                }
                                OutlinedButton(onClick = {
                                    if (name.isBlank() || scheduledAt <= System.currentTimeMillis()) return@OutlinedButton
                                    val call = SimCall(name = name.trim(), number = number.trim(), timeMillis = scheduledAt, ringtoneUri = ringtoneUri, vibration = vibration)
                                    CallScheduler.schedule(context, call)
                                    calls = CallStore.all(context).sortedBy { it.timeMillis }
                                }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.AlarmAdd, null); Spacer(Modifier.width(6.dp)); Text("Schedule")
                                }
                            }
                            Text("Calls are simulations only. Android may deliver scheduled notifications a little late.", fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Scheduled calls", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                            Text("${calls.count { it.status == "Scheduled" }} upcoming", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                    if (calls.isEmpty()) item {
                        Column(Modifier.fillMaxWidth().background(Color(0xFF17211E), RoundedCornerShape(20.dp)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.EventAvailable, null, tint = Color.Gray, modifier = Modifier.size(34.dp))
                            Text("Nothing scheduled yet", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Your planned calls will appear here.", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                    items(calls, key = { it.id }) { call ->
                        Row(Modifier.fillMaxWidth().background(Color(0xFF17211E), RoundedCornerShape(18.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).background(Color(0xFF263D34), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Person, null, tint = Color(0xFF9CECCB)) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(call.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(call.timeMillis)), color = Color.LightGray, fontSize = 12.sp)
                                Text(call.status, color = if (call.status == "Scheduled") Color(0xFF9CECCB) else Color.LightGray, fontSize = 11.sp)
                            }
                            IconButton(onClick = {
                                if (call.status == "Scheduled") {
                                    CallScheduler.cancel(context, call)
                                    calls = CallStore.all(context).sortedBy { it.timeMillis }
                                } else {
                                    CallStore.remove(context, call.id)
                                    calls = CallStore.all(context).sortedBy { it.timeMillis }
                                }
                            }) { Icon(Icons.Rounded.DeleteOutline, "Cancel or delete", tint = Color(0xFFFF9C91)) }
                        }
                    }
                }
            }
        }
    }
}
