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
import ec.edu.uce.appproductosfinal.data.network.OtpRequest
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

    // Estado principal
    var isChecking by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Tabs: 0 = Normal, 1 = OTP
    var selectedTab by remember { mutableIntStateOf(0) }

    // Estado Login Normal
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Estado Login OTP
    var otpEmail by remember { mutableStateOf("") }
    var otpCodeInput by remember { mutableStateOf("") }
    var isCodeSent by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf<String?>(null) } // El código que devolvió Lambda

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
                modifier = Modifier.size(150.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { 
                        selectedTab = 0 
                        showError = false
                    },
                    text = { Text("Normal") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1 
                        showError = false
                    },
                    text = { Text("Código Correo") }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == 0) {
                // =============== LOGIN NORMAL ===============
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
                                    val hashedPassword = SecurityUtils.hashPassword(password)
                                    val response = RetrofitClient.instance.authAction(AuthRequest("login", email, hashedPassword))
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        try {
                                            val userRes = RetrofitClient.instance.getUser(email)
                                            if (userRes.isSuccessful && userRes.body() != null) {
                                                userRepository.addUser(userRes.body()!!)
                                            }
                                        } catch(e: Exception) { }
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
            } else {
                // =============== LOGIN OTP ===============
                OutlinedTextField(
                    value = otpEmail,
                    onValueChange = { otpEmail = it },
                    label = { Text("Correo Electrónico del Grupo") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isChecking && !isCodeSent,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = if (isCodeSent) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isCodeSent) {
                    OutlinedTextField(
                        value = otpCodeInput,
                        onValueChange = { otpCodeInput = it },
                        label = { Text("Código de 6 dígitos") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isChecking,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isChecking) {
                    CircularProgressIndicator()
                } else {
                    if (!isCodeSent) {
                        Button(
                            onClick = {
                                if (otpEmail.isBlank()) {
                                    showError = true
                                    errorMessage = "Ingresa un correo válido"
                                    return@Button
                                }
                                coroutineScope.launch {
                                    isChecking = true
                                    showError = false
                                    
                                    try {
                                        val response = RetrofitClient.instance.authAction(
                                            AuthRequest(action = "request_code", email = otpEmail)
                                        )
                                        
                                        if (response.isSuccessful && response.body()?.success == true) {
                                            isCodeSent = true
                                            // Extraemos el código que generó AWS (nos lo manda en debug_code)
                                            val theCode = response.body()?.debug_code
                                            if (theCode != null) {
                                                // AWS lo guardó en DynamoDB, ahora NOSOTROS enviamos el correo directo
                                                // desde la App usando nuestra configuración funcional de JavaMail
                                                ec.edu.uce.appproductosfinal.data.network.EmailService.enviarCodigoCorreo(otpEmail, theCode)
                                            }
                                        } else {
                                            showError = true
                                            val errMsg = response.body()?.message ?: "Error al solicitar código"
                                            errorMessage = "Fallo del server: $errMsg"
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
                            Text("Enviar Código")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (otpCodeInput.isBlank()) {
                                    showError = true
                                    errorMessage = "Ingresa el código"
                                    return@Button
                                }
                                
                                coroutineScope.launch {
                                    isChecking = true
                                    showError = false
                                    
                                    try {
                                        val response = RetrofitClient.instance.authAction(
                                            AuthRequest(
                                                action = "verify_code", 
                                                email = otpEmail,
                                                code = otpCodeInput
                                            )
                                        )
                                        
                                        if (response.isSuccessful && response.body()?.success == true) {
                                            // Login exitoso según AWS DynamoDB
                                            onLoginSuccess(otpEmail)
                                        } else {
                                            showError = true
                                            errorMessage = response.body()?.message ?: "Código incorrecto o expirado"
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
                            Text("Verificar")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { 
                            isCodeSent = false 
                            otpCodeInput = ""
                            showError = false
                        }) {
                            Text("Reenviar código")
                        }
                    }
                }
            }

            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            if (selectedTab == 0) {
                TextButton(onClick = onNavigateToRegister, enabled = !isChecking) {
                    Text("¿No tienes cuenta? Regístrate aquí")
                }
            }
        }
    }
}
