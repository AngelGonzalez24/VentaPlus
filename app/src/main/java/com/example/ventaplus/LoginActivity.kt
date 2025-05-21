package com.example.ventaplus

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ventaplus.ui.theme.VentaPlusTheme
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            VentaPlusTheme {
                LoginScreen(
                    onLoginSuccess = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onRegisterClick = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                    },
                    onForgotPasswordClick = {
                        startActivity(Intent(this, PasswordRecoveryActivity::class.java))
                    },
                    auth = auth
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    auth: FirebaseAuth? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 16.dp)
        )

        Text(text = "Iniciar sesión", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = icon, contentDescription = description)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (auth == null) return@Button

                isLoading = true
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            Toast.makeText(context, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Iniciar sesión")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onForgotPasswordClick) {
            Text("¿Olvidaste tu contraseña?")
        }

        TextButton(onClick = onRegisterClick) {
            Text("¿No tienes cuenta? Crear cuenta")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    VentaPlusTheme {
        LoginScreen(
            onLoginSuccess = {},
            onRegisterClick = {},
            onForgotPasswordClick = {},
            auth = null
        )
    }
}