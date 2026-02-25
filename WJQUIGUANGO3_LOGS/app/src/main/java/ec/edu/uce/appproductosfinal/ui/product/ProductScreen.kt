package ec.edu.uce.appproductosfinal.ui.product

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import ec.edu.uce.appproductosfinal.data.ProductRepository
import ec.edu.uce.appproductosfinal.data.network.ProductDto
import ec.edu.uce.appproductosfinal.data.network.RetrofitClient
import ec.edu.uce.appproductosfinal.model.Product
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    productId: Int?,
    productRepository: ProductRepository,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var descripcion by remember { mutableStateOf("") }
    var costo by remember { mutableStateOf("") }
    var disponibilidad by remember { mutableStateOf(true) }
    var fechaFabricacionMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(productId != null) }
    
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var tempUri by remember { mutableStateOf<Uri?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) imageUri = tempUri?.toString() }

    val openCamera = {
        try {
            val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "images")
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, "IMG_${System.currentTimeMillis()}.jpg")
            val authority = "ec.edu.uce.appproductosfinal.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            tempUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Error cámara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) openCamera() }

    LaunchedEffect(productId) {
        if (productId != null) {
            val product = productRepository.getProducts().find { it.id == productId }
            product?.let {
                descripcion = it.descripcion
                costo = it.costo.toString()
                disponibilidad = it.disponibilidad
                fechaFabricacionMillis = it.fechaFabricacion
                imageUri = it.imageUri
            }
            isLoading = false
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = fechaFabricacionMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { fechaFabricacionMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    val isFormValid by derivedStateOf {
        descripcion.isNotBlank() && costo.isNotBlank() && costo.toDoubleOrNull() != null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == null) "Nuevo Producto" else "Editar Producto", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera()
                        else permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        Image(painter = rememberAsyncImagePainter(imageUri), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(8.dp)) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            Text("Toca para capturar foto", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                OutlinedTextField(
                    value = descripcion, 
                    onValueChange = { descripcion = it }, 
                    label = { Text("Descripción") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    leadingIcon = { Icon(Icons.Default.Description, null) }, 
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                
                OutlinedTextField(
                    value = costo, 
                    onValueChange = { costo = it }, 
                    label = { Text("Costo") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), 
                    modifier = Modifier.fillMaxWidth(), 
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null) }, 
                    shape = RoundedCornerShape(16.dp),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                Card(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Fecha de Fabricación", style = MaterialTheme.typography.labelSmall)
                            Text(text = dateFormat.format(Date(fechaFabricacionMillis)), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (disponibilidad) Icons.Default.CheckCircle else Icons.Default.Block, null, tint = if (disponibilidad) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(if (disponibilidad) "Disponible" else "Agotado")
                        }
                        Switch(checked = disponibilidad, onCheckedChange = { disponibilidad = it })
                    }
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val tempProduct = Product(
                                id = productId ?: 0,
                                descripcion = descripcion,
                                fechaFabricacion = fechaFabricacionMillis,
                                costo = costo.toDoubleOrNull() ?: 0.0,
                                disponibilidad = disponibilidad,
                                imageUri = imageUri,
                                lastUpdated = System.currentTimeMillis()
                            )
                            val finalId = if (productId == null) productRepository.addProduct(tempProduct).toInt() 
                                          else { productRepository.updateProduct(tempProduct); productId }
                            val finalProduct = tempProduct.copy(id = finalId)
                            try {
                                val productDto = finalProduct.toDto(context)
                                RetrofitClient.instance.syncProduct(productDto)
                            } catch (e: Exception) { }
                            onSave()
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (productId == null) "Registrar Producto" else "Guardar Cambios", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun Product.toDto(context: Context): ProductDto {
    var base64: String? = null
    if (!imageUri.isNullOrEmpty() && !imageUri!!.startsWith("http")) {
        try {
            val uri = Uri.parse(imageUri)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("ProductScreen", "Error Base64", e)
        }
    }
    return ProductDto(id, descripcion, fechaFabricacion, costo, disponibilidad, imageUri, lastUpdated, base64)
}
