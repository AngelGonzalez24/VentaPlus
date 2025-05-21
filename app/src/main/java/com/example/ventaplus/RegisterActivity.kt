package com.example.ventaplus

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().reference

        setContent {
            VentaPlusTheme {
                RegisterScreen(
                    onRegisterSuccess = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    onLoginClick = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    auth = auth,
                    dbRef = dbRef
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit,
    auth: FirebaseAuth? = null,
    dbRef: DatabaseReference? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var adminName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier
                .height(100.dp)
                .padding(bottom = 16.dp)
        )

        Text(text = "Crear cuenta", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            label = { Text("Nombre del negocio") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = adminName,
            onValueChange = { adminName = it },
            label = { Text("Nombre del administrador") },
            modifier = Modifier.fillMaxWidth()
        )

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
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle Password Visibility")
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it }
            )
            Text(text = "Acepto los ")
            Text(
                text = "Términos y Condiciones",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showTermsDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank() || businessName.isBlank() || adminName.isBlank()) {
                    Toast.makeText(context, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!termsAccepted) {
                    Toast.makeText(context, "Debes aceptar los Términos y Condiciones", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (auth == null || dbRef == null) return@Button

                isLoading = true
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                            val userMap = mapOf(
                                "negocio" to businessName,
                                "administrador" to adminName,
                                "correo" to email
                            )
                            dbRef.child("usuarios").child(uid).setValue(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        context,
                                        "Cuenta creada exitosamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isLoading = false
                                    onRegisterSuccess()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Error al guardar datos",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isLoading = false
                                }
                        } else {
                            Toast.makeText(
                                context,
                                "Error: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            isLoading = false
                        }
                    }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear cuenta")
        }

        TextButton(onClick = onLoginClick) {
            Text("¿Ya tienes cuenta? Iniciar sesión")
        }

        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                confirmButton = {
                    TextButton(onClick = { showTermsDialog = false }) {
                        Text("Cerrar")
                    }
                },
                title = { Text("Términos y Condiciones de VentaPlus") },
                text = {
                    Text(
                        """
                        1. Uso de la Aplicación
                        VentaPlus es una aplicación diseñada para facilitar la gestión de ventas, inventarios y clientes en negocios comerciales. Su uso está limitado exclusivamente a fines comerciales lícitos.

                        2. Creación de Cuenta
                        Para acceder a las funcionalidades de VentaPlus, es necesario registrarse con datos verídicos. El usuario es responsable de mantener la confidencialidad de sus credenciales de acceso.

                        3. Protección de Datos
                        VentaPlus almacena información como nombre del negocio, administrador, y correo electrónico en Firebase. Nos comprometemos a proteger esta información y no compartirla con terceros sin consentimiento, salvo requerimiento legal.

                        4. Responsabilidad del Usuario
                        El usuario se compromete a utilizar la aplicación de forma ética. Queda prohibido el uso con fines fraudulentos, la manipulación de datos, o cualquier acción que perjudique el funcionamiento del sistema.

                        5. Seguridad
                        Implementamos medidas de seguridad para resguardar la información. Sin embargo, el usuario también debe tomar precauciones como no compartir su contraseña y cerrar sesión en dispositivos públicos.

                        6. Cambios en los Términos
                        Nos reservamos el derecho de modificar estos Términos y Condiciones. Se notificará a los usuarios sobre cualquier cambio mediante la aplicación o correo electrónico registrado.

                        Al crear una cuenta, aceptas estos Términos y Condiciones.
                        """.trimIndent()
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    VentaPlusTheme {
        RegisterScreen(
            onRegisterSuccess = {},
            onLoginClick = {},
            auth = null,
            dbRef = null
        )
    }
}