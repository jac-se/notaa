package cisneros.nota.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cisneros.nota.work.ReminderNotificationHelper
import cisneros.nota.work.ReminderScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestNotificationsScreen(
    textSize: TextSizeLevel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sizes = textSize.toAccessibleSizes()
    val cs = MaterialTheme.colorScheme
    val lifecycleOwner = LocalLifecycleOwner.current

    var notificationsEnabled by remember { mutableStateOf(ReminderNotificationHelper.areNotificationsEnabled(context)) }
    var exactAlarmGranted by remember { mutableStateOf(ReminderNotificationHelper.canScheduleExactAlarms(context)) }
    var channelSilenced by remember { mutableStateOf(ReminderNotificationHelper.isChannelSilenced(context)) }

    var postNotificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        postNotificationGranted = isGranted
        notificationsEnabled = ReminderNotificationHelper.areNotificationsEnabled(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = ReminderNotificationHelper.areNotificationsEnabled(context)
                exactAlarmGranted = ReminderNotificationHelper.canScheduleExactAlarms(context)
                channelSilenced = ReminderNotificationHelper.isChannelSilenced(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    postNotificationGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Probar avisos", fontSize = sizes.title, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Compruebe que el teléfono suena, vibra y muestra la notificación en pantalla.",
                fontSize = sizes.body,
                color = cs.onSurfaceVariant
            )

            // Estado de Permisos
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Estado de configuración", fontSize = sizes.title, fontWeight = FontWeight.Bold)

                    StatusRow(
                        title = "Notificaciones generales",
                        ok = notificationsEnabled,
                        okText = "Activadas",
                        failText = "Desactivadas",
                        textSize = textSize
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        StatusRow(
                            title = "Permiso de notificaciones",
                            ok = postNotificationGranted,
                            okText = "Concedido",
                            failText = "No concedido",
                            textSize = textSize
                        )
                    }

                    StatusRow(
                        title = "Alarmas exactas",
                        ok = exactAlarmGranted,
                        okText = "Permitido",
                        failText = "Sin permiso exacto",
                        textSize = textSize
                    )

                    StatusRow(
                        title = "Sonido y vibración del canal",
                        ok = !channelSilenced,
                        okText = "Activo",
                        failText = "Silenciado o sin sonido",
                        textSize = textSize
                    )
                }
            }

            // Botones de acción principal
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !postNotificationGranted) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        ReminderNotificationHelper.sendTestNotification(context)
                        Toast.makeText(context, "Aviso de prueba enviado", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Probar aviso ahora", fontSize = sizes.body, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !postNotificationGranted) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        ReminderScheduler.scheduleTestAlarm(context, 10_000L)
                        Toast.makeText(
                            context,
                            "Aviso programado para dentro de 10 segundos. Puede bloquear la pantalla.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Probar aviso en 10 segundos", fontSize = sizes.body, fontWeight = FontWeight.Bold)
            }

            // Accesos directos a ajustes
            OutlinedButton(
                onClick = { ReminderNotificationHelper.openAppNotificationSettings(context) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("Abrir configuración de notificaciones", fontSize = sizes.body)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmGranted) {
                OutlinedButton(
                    onClick = { ReminderNotificationHelper.openExactAlarmSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Permitir alarmas exactas en el sistema", fontSize = sizes.body)
                }
            }

            // Instrucciones claras para el usuario
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cs.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 Guía de ayuda", fontSize = sizes.title, fontWeight = FontWeight.Bold, color = cs.onPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Si el aviso no suena ni vibra:\n" +
                                "  - Verifique que el teléfono no esté en modo Silencioso o No Molestar.\n" +
                                "  - Suba el volumen de notificaciones.\n" +
                                "  - Asegúrese de haber concedido los permisos requeridos arriba.",
                        fontSize = sizes.body,
                        color = cs.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    ok: Boolean,
    okText: String,
    failText: String,
    textSize: TextSizeLevel
) {
    val sizes = textSize.toAccessibleSizes()
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = sizes.body, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (ok) cs.primary else cs.error,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                if (ok) okText else failText,
                fontSize = sizes.body,
                fontWeight = FontWeight.SemiBold,
                color = if (ok) cs.primary else cs.error
            )
        }
    }
}
