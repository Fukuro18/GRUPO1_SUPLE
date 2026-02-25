package ec.edu.uce.appproductosfinal

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ec.edu.uce.appproductosfinal.data.AppDatabase
import ec.edu.uce.appproductosfinal.data.ProductRepository
import ec.edu.uce.appproductosfinal.data.UserRepository
import ec.edu.uce.appproductosfinal.location.SharedPreferenceUtil
import ec.edu.uce.appproductosfinal.ui.home.HomeScreen
import ec.edu.uce.appproductosfinal.ui.login.LoginScreen
import ec.edu.uce.appproductosfinal.ui.product.ProductScreen
import ec.edu.uce.appproductosfinal.ui.register.RegisterScreen
import ec.edu.uce.appproductosfinal.ui.theme.AppProductosTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppProductosTheme {
                // Solicitar permiso de notificaciones para Android 13+
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { }
                )
                
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                AppNavigation()
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val userRepository = remember { UserRepository(database.userDao()) }
    val productRepository = remember { ProductRepository(database.productDao()) }
    val coroutineScope = rememberCoroutineScope()

    val savedUser = remember { SharedPreferenceUtil.getUserSession(context) }
    val startRoute = if (savedUser != null) "home/$savedUser" else "login"

    val navController = rememberNavController()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSessionExpiredDialog by remember { mutableStateOf(false) }

    val logout = {
        SharedPreferenceUtil.clearSession(context)
        navController.navigate("login") {
            popUpTo(0) { inclusive = true }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (SharedPreferenceUtil.getUserSession(context) != null) {
                if (!SharedPreferenceUtil.isSessionValid(context)) {
                    showSessionExpiredDialog = true
                }
            }
            delay(5000)
        }
    }

    if (showSessionExpiredDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Sesión Expirada") },
            text = { Text("Tu sesión ha expirado por inactividad o límite de tiempo.") },
            confirmButton = {
                TextButton(onClick = {
                    showSessionExpiredDialog = false
                    logout()
                }) { Text("OK") }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Deseas salir de la aplicación?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    logout()
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInteropFilter { motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    SharedPreferenceUtil.updateLastActivity(context)
                }
                false
            }
    ) {
        NavHost(navController = navController, startDestination = startRoute) {
            composable(
                route = "login?showSuccess={showSuccess}",
                arguments = listOf(navArgument("showSuccess") { 
                    type = NavType.BoolType
                    defaultValue = false 
                })
            ) { backStackEntry ->
                val showSuccess = backStackEntry.arguments?.getBoolean("showSuccess") ?: false
                LoginScreen(
                    userRepository = userRepository,
                    onLoginSuccess = { userName -> 
                        SharedPreferenceUtil.saveUserSession(context, userName)
                        navController.navigate("home/$userName") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate("register") },
                    showSuccessMessage = showSuccess
                )
            }
            composable("register") {
                RegisterScreen(
                    userRepository = userRepository,
                    onRegisterSuccess = {
                        navController.navigate("login?showSuccess=true") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = "home/{userName}",
                arguments = listOf(navArgument("userName") { type = NavType.StringType })
            ) { backStackEntry ->
                val userName = backStackEntry.arguments?.getString("userName") ?: ""
                HomeScreen(
                    userName = userName,
                    productRepository = productRepository,
                    onLogout = { showLogoutDialog = true },
                    onAddProduct = { navController.navigate("product") },
                    onEditProduct = { product -> navController.navigate("product?id=${product.id}") },
                    onDeleteProduct = { },
                    onNavigateToSensors = { },
                    onNavigateToLocation = { },
                    onBack = { }
                )
            }
            composable("product?id={id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.toIntOrNull()
                ProductScreen(
                    productId = id,
                    productRepository = productRepository,
                    onSave = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
