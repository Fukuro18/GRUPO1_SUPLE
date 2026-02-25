package ec.edu.uce.appproductosfinal.ui.register

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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
import ec.edu.uce.appproductosfinal.model.User
import ec.edu.uce.appproductosfinal.utils.SecurityUtils
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    userRepository: UserRepository,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisibility by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }

    val isFormValid by derivedStateOf {
        nombre.isNotBlank() && apellido.isNotBlank() && password.isNotBlank() && password == confirmPassword
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(id = R.drawable.grupo1img), contentDescription = null, modifier = Modifier.size(150.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Crear Nueva Cuenta", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nombre, 
            onValueChange = { nombre = it }, 
            label = { Text("Nombre/Usuario") }, 
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = apellido, 
            onValueChange = { apellido = it }, 
            label = { Text("Apellido") }, 
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            trailingIcon = {
                IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                    Icon(if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPassword, 
            onValueChange = { confirmPassword = it }, 
            label = { Text("Confirmar") }, 
            modifier = Modifier.fillMaxWidth(), 
            visualTransformation = PasswordVisualTransformation(), 
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isRegistering) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isRegistering = true
                        val hashedPassword = SecurityUtils.hashPassword(password)
                        val newUser = User(
                            nombre = nombre,
                            apellido = apellido,
                            password = hashedPassword,
                            lastUpdated = System.currentTimeMillis()
                        )
                        
                        try {
                            userRepository.addUser(newUser)
                            val response = RetrofitClient.instance.syncUser(newUser)
                            
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                onRegisterSuccess()
                            } else {
                                Toast.makeText(context, "Registrado localmente", Toast.LENGTH_LONG).show()
                                onRegisterSuccess()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Registro local completado", Toast.LENGTH_LONG).show()
                            onRegisterSuccess()
                        } finally {
                            isRegistering = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid
            ) {
                Text("Registrarse")
            }
        }
    }
}
