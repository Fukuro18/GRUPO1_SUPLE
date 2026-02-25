package ec.edu.uce.appproductosfinal.ui.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensorList = remember { sensorManager.getSensorList(Sensor.TYPE_ALL) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensores del Dispositivo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sensorList) { sensor ->
                SensorItem(sensor)
            }
        }
    }
}

@Composable
fun SensorItem(sensor: Sensor) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Nombre: ${sensor.name}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Fabricante: ${sensor.vendor}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Versión: ${sensor.version}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Tipo: ${sensor.stringType ?: sensor.type.toString()}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
