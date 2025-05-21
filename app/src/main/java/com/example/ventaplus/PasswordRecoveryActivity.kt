// PasswordRecoveryActivity.kt
package com.example.ventaplus

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ventaplus.ui.theme.VentaPlusTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PasswordRecoveryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()

        setContent {
            VentaPlusTheme {
                PasswordRecoveryScreen(
                    auth = auth,
                    onBackToLogin = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PasswordRecoveryScreen(
    auth: FirebaseAuth?,
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance().reference

    fun isConnectedToInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 16.dp)
        )

        Text("Recuperar contraseña", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                statusMessage = ""
                isSuccess = null
            },
            label = { Text("Correo electrónico") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (!isConnectedToInternet()) {
                    isSuccess = false
                    statusMessage = "¡Oops! No tienes conexión a Internet"
                    return@Button
                }

                if (email.isBlank()) {
                    Toast.makeText(context, "Por favor escribe tu correo", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true

                // ✅ Cambiado a "usuarios"
                database.child("usuarios").get().addOnSuccessListener { snapshot ->
                    var found = false
                    for (user in snapshot.children) {
                        val userEmail = user.child("correo").getValue(String::class.java)
                        if (userEmail.equals(email, ignoreCase = true)) {
                            found = true
                            break
                        }
                    }

                    if (found) {
                        auth?.sendPasswordResetEmail(email)
                            ?.addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    isSuccess = true
                                    statusMessage = "Correo de recuperación enviado exitosamente 🎉"
                                } else {
                                    isSuccess = false
                                    statusMessage = "Error al enviar el correo: ${task.exception?.message}"
                                }
                            }
                    } else {
                        isLoading = false
                        isSuccess = false
                        statusMessage = "Este correo no está registrado"
                    }

                }.addOnFailureListener { error ->
                    isLoading = false
                    isSuccess = false
                    statusMessage = "Error al verificar en la base de datos: ${error.message}"
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Procesando..." else "Enviar correo de recuperación")
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = statusMessage.isNotBlank(),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            val bgColor = if (isSuccess == true) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer
            val textColor = if (isSuccess == true) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onErrorContainer

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = bgColor)
            ) {
                Text(
                    text = statusMessage,
                    color = textColor,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { onBackToLogin() }) {
            Text("← Volver al login")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PasswordRecoveryScreenPreview() {
    VentaPlusTheme {
        PasswordRecoveryScreen(auth = null, onBackToLogin = {})
    }
}