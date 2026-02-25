package ec.edu.uce.appproductosfinal.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ec.edu.uce.appproductosfinal.R
import ec.edu.uce.appproductosfinal.data.UserRepository
import ec.edu.uce.appproductosfinal.data.network.AuthRequest
import ec.edu.uce.appproductosfinal.data.network.RetrofitClient
import ec.edu.uce.appproductosfinal.utils.SecurityUtils
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    userRepository: UserRepository,
    onLoginSuccess: (String) -> Unit, 
    onNavigateToRegister: () -> Unit,
    showSuccessMessage: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.grupo1img),
                contentDescription = "Logo",
                modifier = Modifier.size(200.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico / Usuario") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isChecking,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isChecking,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isChecking) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isChecking = true
                            showError = false
                            
                            try {
                                // Hasheamos la contraseña igual que en el registro
                                val hashedPassword = SecurityUtils.hashPassword(password)
                                val response = RetrofitClient.instance.authAction(AuthRequest("login", email, hashedPassword))
                                if (response.isSuccessful && response.body()?.success == true) {
                                    // Guardar datos de usuario localmente si queremos
                                    try {
                                        val userRes = RetrofitClient.instance.getUser(email)
                                        if (userRes.isSuccessful && userRes.body() != null) {
                                            userRepository.addUser(userRes.body()!!)
                                        }
                                    } catch(e: Exception) { /* ignore si no existe, igual pasamos */ }
                                    
                                    onLoginSuccess(email)
                                } else {
                                    showError = true
                                    errorMessage = response.body()?.message ?: "Contraseña incorrecta"
                                }
                            } catch (e: Exception) {
                                showError = true
                                errorMessage = "Error de conexión: ${e.localizedMessage}"
                            }
                            isChecking = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ingresar")
                }
            }

            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onNavigateToRegister, enabled = !isChecking) {
                Text("¿No tienes cuenta? Registrate aquí")
            }
        }
    }
}
