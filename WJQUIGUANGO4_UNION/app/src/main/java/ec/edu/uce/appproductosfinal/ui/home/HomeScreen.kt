package ec.edu.uce.appproductosfinal.ui.home

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.*
import coil.compose.rememberAsyncImagePainter
import ec.edu.uce.appproductosfinal.data.ProductRepository
import ec.edu.uce.appproductosfinal.data.network.RetrofitClient
import ec.edu.uce.appproductosfinal.data.network.SyncWorker
import ec.edu.uce.appproductosfinal.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    productRepository: ProductRepository,
    onLogout: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onNavigateToSensors: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val products by productRepository.getProductsFlow().collectAsState(initial = emptyList())
    var isRefreshing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "EC")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // SALUDO DINÁMICO (RESTAURADO)
    val greeting = remember {
        val calendar = Calendar.getInstance()
        when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "¡Buenos días!"
            in 12..18 -> "¡Buenas tardes!"
            else -> "¡Buenas noches!"
        }
    }

    val refreshData = {
        scope.launch(Dispatchers.IO) {
            isRefreshing = true
            try {
                val response = RetrofitClient.instance.getAllProducts()
                if (response.isSuccessful) {
                    val cloudProducts = response.body() ?: emptyList()
                    val localProducts = productRepository.getProducts()

                    val cloudIds = cloudProducts.map { it.id }.toSet()
                    localProducts.forEach { local ->
                        if (local.id !in cloudIds) productRepository.deleteProduct(local.id)
                    }

                    cloudProducts.forEach { cloud ->
                        val local = localProducts.find { it.id == cloud.id }
                        if (local == null || cloud.lastUpdated > local.lastUpdated) {
                            productRepository.addProduct(cloud)
                        }
                    }
                }
                
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                WorkManager.getInstance(context).enqueue(syncRequest)
                
            } catch (e: Exception) { }
            finally { isRefreshing = false }
        }
    }

    LaunchedEffect(Unit) { refreshData() }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)))
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(greeting, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f))
                        Text(userName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(onClick = onLogout, modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White)
                    }
                }
            }
        },
        floatingActionButton = {
            LargeFloatingActionButton(onClick = onAddProduct, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = RoundedCornerShape(24.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { refreshData() }, modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                SummaryHeader(products.size, products.sumOf { it.costo }, currencyFormat)
                
                if (products.isEmpty() && !isRefreshing) {
                    EmptyState()
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(1), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                        items(products, key = { it.id }) { product ->
                            ModernProductCard(product, currencyFormat, dateFormat, onEdit = { onEditProduct(product) }, onDelete = {
                                productToDelete = product
                                showDeleteDialog = true
                            })
                        }
                    }
                }
            }
            
            if (isDeleting) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("Eliminar Producto") },
            text = { Text("¿Deseas borrar este producto de forma permanente?") },
            confirmButton = {
                Button(
                    enabled = !isDeleting,
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            productToDelete?.let { prod ->
                                try {
                                    val response = withContext(Dispatchers.IO) {
                                        RetrofitClient.instance.deleteProduct(prod.id)
                                    }
                                    
                                    if (response.isSuccessful) {
                                        productRepository.deleteProduct(prod.id)
                                        Toast.makeText(context, "Producto eliminado", Toast.LENGTH_SHORT).show()
                                    } else {
                                        productRepository.deleteProduct(prod.id)
                                        Toast.makeText(context, "Eliminado localmente", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    productRepository.deleteProduct(prod.id)
                                    Toast.makeText(context, "Eliminado localmente", Toast.LENGTH_SHORT).show()
                                }
                            }
                            isDeleting = false
                            showDeleteDialog = false
                        }
                    }, 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = { 
                TextButton(enabled = !isDeleting, onClick = { showDeleteDialog = false }) { Text("Cancelar") } 
            }
        )
    }
}

@Composable
fun SummaryHeader(count: Int, total: Double, format: NumberFormat) {
    Card(modifier = Modifier.fillMaxWidth().padding(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(24.dp)) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Inversión Total", style = MaterialTheme.typography.labelMedium)
                Text(format.format(total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            }
            VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("Stock", style = MaterialTheme.typography.labelMedium)
                Text("$count", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ModernProductCard(product: Product, currencyFormat: NumberFormat, dateFormat: SimpleDateFormat, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                if (!product.imageUri.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(product.imageUri), 
                        contentDescription = null, 
                        modifier = Modifier.fillMaxSize(), 
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.descripcion, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("Código: ${product.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text("Fabricación: ${dateFormat.format(Date(product.fechaFabricacion))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currencyFormat.format(product.costo), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    AvailabilityBadge(product.disponibilidad)
                }
            }
            Column {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun AvailabilityBadge(isAvailable: Boolean) {
    val color = if (isAvailable) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
    Surface(color = color.copy(alpha = 0.1f), shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Text(if (isAvailable) "En Stock" else "Agotado", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyState() {
    Column(modifier = Modifier.fillMaxSize().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        Text("No hay productos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Desliza para actualizar", color = MaterialTheme.colorScheme.outline)
    }
}
