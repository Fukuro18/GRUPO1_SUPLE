package ec.edu.uce.appproductosfinal.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ec.edu.uce.appproductosfinal.R
import ec.edu.uce.appproductosfinal.data.UserRepository
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

    var nombre by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisibility by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
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
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Usuario") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isChecking,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
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
                visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                        Icon(if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                    }
                }
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
                            val hashedPassword = SecurityUtils.hashPassword(password)
                            val localUser = userRepository.findUser(nombre, hashedPassword)
                            
                            if (localUser != null) {
                                onLoginSuccess(localUser.nombre)
                            } else {
                                try {
                                    val response = RetrofitClient.instance.getUser(nombre)
                                    if (response.isSuccessful && response.body() != null) {
                                        val cloudUser = response.body()!!
                                        if (cloudUser.password == hashedPassword) {
                                            userRepository.addUser(cloudUser)
                                            onLoginSuccess(cloudUser.nombre)
                                        } else { showError = true }
                                    } else { showError = true }
                                } catch (e: Exception) { showError = true }
                            }
                            isChecking = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Iniciar Sesión")
                }
            }

            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Credenciales inválidas", color = MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onNavigateToRegister, enabled = !isChecking) {
                Text("¿No tienes cuenta? Regístrate aquí")
            }
        }
    }
}
