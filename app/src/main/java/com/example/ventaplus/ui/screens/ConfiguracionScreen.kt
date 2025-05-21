package com.example.ventaplus.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(database: DatabaseReference) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Datos negocio/admin
    var nombreNegocio by remember { mutableStateOf("") }
    var direccionNegocio by remember { mutableStateOf("") }
    var nombreAdministrador by remember { mutableStateOf("") }
    var correoAdministrador by remember { mutableStateOf("") }

    // Listas empleados y turnos
    var empleados by remember { mutableStateOf(listOf<Empleado>()) }
    var turnos by remember { mutableStateOf(listOf<Turno>()) }

    var cargando by remember { mutableStateOf(false) }

    // Estados para modales (agregar/editar)
    var empleadoEditando by remember { mutableStateOf<Empleado?>(null) }
    var mostrarEmpleadoDialog by remember { mutableStateOf(false) }

    var turnoEditando by remember { mutableStateOf<Turno?>(null) }
    var mostrarTurnoDialog by remember { mutableStateOf(false) }

    // Estado para dialogo de confirmación eliminar
    var empleadoAEliminar by remember { mutableStateOf<Empleado?>(null) }
    var turnoAEliminar by remember { mutableStateOf<Turno?>(null) }

    // Estado para cambiar contraseña
    var mostrarCambiarPassDialog by remember { mutableStateOf(false) }
    var correoParaPass by remember { mutableStateOf("") }
    var correoPassError by remember { mutableStateOf<String?>(null) }

    // Carga inicial datos
    suspend fun cargarDatos() {
        try {
            val userRef = database.child("usuarios").child(uid)
            nombreNegocio = userRef.child("negocio").get().await().getValue(String::class.java) ?: ""
            direccionNegocio = userRef.child("direccion").get().await().getValue(String::class.java) ?: ""
            nombreAdministrador = userRef.child("administrador").get().await().getValue(String::class.java) ?: ""
            correoAdministrador = userRef.child("correo").get().await().getValue(String::class.java) ?: ""

            val empSnap = userRef.child("empleados").get().await()
            empleados = empSnap.children.mapNotNull {
                val id = it.key ?: return@mapNotNull null
                val nombre = it.child("nombre").getValue(String::class.java) ?: ""
                val correo = it.child("correo").getValue(String::class.java) ?: ""
                Empleado(id, nombre, correo)
            }

            val turnoSnap = userRef.child("turnos").get().await()
            turnos = turnoSnap.children.mapNotNull {
                val id = it.key ?: return@mapNotNull null
                val fecha = it.child("fecha").getValue(String::class.java) ?: ""
                val caja = it.child("caja").getValue(String::class.java) ?: ""
                Turno(id, fecha, caja)
            }
        } catch (e: Exception) {
            Log.e("Configuracion", "Error cargando datos: ${e.message}")
        }
    }

    LaunchedEffect(Unit) {
        scope.launch { cargarDatos() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Configuración",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // Datos negocio
            item {
                SectionTitle(title = "Datos del Negocio", icon = Icons.Filled.Storefront)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = nombreNegocio,
                            onValueChange = { nombreNegocio = it },
                            label = { Text("Nombre del Negocio") },
                            leadingIcon = { Icon(Icons.Filled.Store, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = direccionNegocio,
                            onValueChange = { direccionNegocio = it },
                            label = { Text("Dirección del Negocio") },
                            leadingIcon = { Icon(Icons.Filled.LocationOn, null) },
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Datos administrador
            item {
                SectionTitle(title = "Datos del Administrador", icon = Icons.Filled.Person)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = nombreAdministrador,
                            onValueChange = { nombreAdministrador = it },
                            label = { Text("Nombre del Administrador") },
                            leadingIcon = { Icon(Icons.Filled.AccountCircle, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = correoAdministrador,
                            onValueChange = { correoAdministrador = it },
                            label = { Text("Correo del Administrador") },
                            leadingIcon = { Icon(Icons.Filled.Email, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                correoParaPass = correoAdministrador
                                correoPassError = null
                                mostrarCambiarPassDialog = true
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Icon(Icons.Filled.LockReset, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Cambiar contraseña")
                        }
                    }
                }
            }

            // Empleados
            item {
                SectionTitle(title = "Empleados", icon = Icons.Filled.Group)
                Spacer(Modifier.height(4.dp))
                if (empleados.isEmpty()) {
                    Text(
                        "No hay empleados registrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                }
                Button(
                    onClick = {
                        empleadoEditando = null
                        mostrarEmpleadoDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PersonAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar empleado")
                }
            }

            items(empleados, key = { it.id }) { empleado ->
                EmpleadoItem(
                    empleado = empleado,
                    onEditar = {
                        empleadoEditando = empleado
                        mostrarEmpleadoDialog = true
                    },
                    onEliminar = {
                        empleadoAEliminar = empleado
                    }
                )
            }

            // Turnos
            item {
                Spacer(Modifier.height(16.dp))
                SectionTitle(title = "Turnos", icon = Icons.Filled.Schedule)
                Spacer(Modifier.height(4.dp))
                if (turnos.isEmpty()) {
                    Text(
                        "No hay turnos registrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                }
                Button(
                    onClick = {
                        turnoEditando = null
                        mostrarTurnoDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar turno")
                }
            }

            items(turnos, key = { it.id }) { turno ->
                TurnoItem(
                    turno = turno,
                    onEditar = {
                        turnoEditando = turno
                        mostrarTurnoDialog = true
                    },
                    onEliminar = {
                        turnoAEliminar = turno
                    }
                )
            }

            // Botón guardar cambios negocio/admin
            item {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        scope.launch {
                            if (nombreNegocio.isBlank() || nombreAdministrador.isBlank()) {
                                snackbarHostState.showSnackbar("Nombre del negocio y administrador son obligatorios")
                                return@launch
                            }
                            cargando = true
                            try {
                                val userRef = database.child("usuarios").child(uid)
                                userRef.child("negocio").setValue(nombreNegocio)
                                userRef.child("direccion").setValue(direccionNegocio)
                                userRef.child("administrador").setValue(nombreAdministrador)
                                userRef.child("correo").setValue(correoAdministrador)
                                snackbarHostState.showSnackbar("Datos actualizados correctamente")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error al actualizar datos")
                            }
                            cargando = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !cargando
                ) {
                    if (cargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardar Cambios", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Dialogo para agregar/editar empleado
    if (mostrarEmpleadoDialog) {
        EmpleadoDialog(
            empleado = empleadoEditando,
            onDismiss = { mostrarEmpleadoDialog = false },
            onConfirm = { nombre, correo ->
                scope.launch {
                    if (nombre.isBlank() || correo.isBlank()) {
                        snackbarHostState.showSnackbar("Nombre y correo son obligatorios")
                        return@launch
                    }
                    try {
                        val userRef = database.child("usuarios").child(uid).child("empleados")
                        if (empleadoEditando == null) {
                            // Nuevo empleado - clave push
                            val newId = userRef.push().key ?: return@launch
                            userRef.child(newId).setValue(
                                mapOf("nombre" to nombre, "correo" to correo)
                            )
                            empleados = empleados + Empleado(newId, nombre, correo)
                            snackbarHostState.showSnackbar("Empleado agregado")
                        } else {
                            // Editar existente
                            userRef.child(empleadoEditando!!.id).setValue(
                                mapOf("nombre" to nombre, "correo" to correo)
                            )
                            empleados = empleados.map {
                                if (it.id == empleadoEditando!!.id) it.copy(nombre = nombre, correo = correo) else it
                            }
                            snackbarHostState.showSnackbar("Empleado actualizado")
                        }
                        mostrarEmpleadoDialog = false
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error al guardar empleado")
                    }
                }
            }
        )
    }

    // Dialogo para agregar/editar turno
    if (mostrarTurnoDialog) {
        TurnoDialog(
            turno = turnoEditando,
            onDismiss = { mostrarTurnoDialog = false },
            onConfirm = { fecha, caja ->
                scope.launch {
                    if (fecha.isBlank() || caja.isBlank()) {
                        snackbarHostState.showSnackbar("Fecha y caja son obligatorios")
                        return@launch
                    }
                    try {
                        val userRef = database.child("usuarios").child(uid).child("turnos")
                        if (turnoEditando == null) {
                            val newId = userRef.push().key ?: return@launch
                            userRef.child(newId).setValue(
                                mapOf("fecha" to fecha, "caja" to caja)
                            )
                            turnos = turnos + Turno(newId, fecha, caja)
                            snackbarHostState.showSnackbar("Turno agregado")
                        } else {
                            userRef.child(turnoEditando!!.id).setValue(
                                mapOf("fecha" to fecha, "caja" to caja)
                            )
                            turnos = turnos.map {
                                if (it.id == turnoEditando!!.id) it.copy(fecha = fecha, caja = caja) else it
                            }
                            snackbarHostState.showSnackbar("Turno actualizado")
                        }
                        mostrarTurnoDialog = false
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error al guardar turno")
                    }
                }
            }
        )
    }

    // Dialogo confirmación eliminar empleado
    if (empleadoAEliminar != null) {
        ConfirmDialog(
            title = "Eliminar empleado",
            text = "¿Seguro que deseas eliminar a ${empleadoAEliminar!!.nombre}?",
            onConfirm = {
                scope.launch {
                    try {
                        database.child("usuarios").child(uid).child("empleados").child(empleadoAEliminar!!.id).removeValue()
                        empleados = empleados.filter { it.id != empleadoAEliminar!!.id }
                        snackbarHostState.showSnackbar("Empleado eliminado")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error al eliminar empleado")
                    }
                    empleadoAEliminar = null
                }
            },
            onDismiss = { empleadoAEliminar = null }
        )
    }

    // Dialogo confirmación eliminar turno
    if (turnoAEliminar != null) {
        ConfirmDialog(
            title = "Eliminar turno",
            text = "¿Seguro que deseas eliminar el turno del ${turnoAEliminar!!.fecha} (Caja: ${turnoAEliminar!!.caja})?",
            onConfirm = {
                scope.launch {
                    try {
                        database.child("usuarios").child(uid).child("turnos").child(turnoAEliminar!!.id).removeValue()
                        turnos = turnos.filter { it.id != turnoAEliminar!!.id }
                        snackbarHostState.showSnackbar("Turno eliminado")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error al eliminar turno")
                    }
                    turnoAEliminar = null
                }
            },
            onDismiss = { turnoAEliminar = null }
        )
    }

    // Dialogo cambiar contraseña
    if (mostrarCambiarPassDialog) {
        ChangePasswordDialog(
            correo = correoParaPass,
            error = correoPassError,
            onCorreoChange = { correoParaPass = it },
            onDismiss = { mostrarCambiarPassDialog = false },
            onConfirm = {
                if (!correoParaPass.contains("@")) {
                    correoPassError = "Correo inválido"
                    return@ChangePasswordDialog
                }
                scope.launch {
                    try {
                        auth.sendPasswordResetEmail(correoParaPass).await()
                        snackbarHostState.showSnackbar("Correo de recuperación enviado")
                        mostrarCambiarPassDialog = false
                    } catch (e: Exception) {
                        correoPassError = "Error enviando correo: ${e.message}"
                    }
                }
            }
        )
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

data class Empleado(val id: String, val nombre: String, val correo: String)
data class Turno(val id: String, val fecha: String, val caja: String)

@Composable
fun EmpleadoItem(
    empleado: Empleado,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(empleado.nombre, fontWeight = FontWeight.SemiBold)
                Text(empleado.correo, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Filled.Edit, "Editar empleado")
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Filled.Delete, "Eliminar empleado", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TurnoItem(
    turno: Turno,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Fecha: ${turno.fecha}", fontWeight = FontWeight.SemiBold)
                Text("Caja: ${turno.caja}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Filled.Edit, "Editar turno")
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Filled.Delete, "Eliminar turno", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmpleadoDialog(
    empleado: Empleado?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nombre by remember { mutableStateOf(empleado?.nombre ?: "") }
    var correo by remember { mutableStateOf(empleado?.correo ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (empleado == null) "Agregar empleado" else "Editar empleado") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(nombre.trim(), correo.trim()) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnoDialog(
    turno: Turno?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var fecha by remember { mutableStateOf(turno?.fecha ?: "") }
    var caja by remember { mutableStateOf(turno?.caja ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (turno == null) "Agregar turno" else "Editar turno") },
        text = {
            Column {
                OutlinedTextField(
                    value = fecha,
                    onValueChange = { fecha = it },
                    label = { Text("Fecha (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caja,
                    onValueChange = { caja = it },
                    label = { Text("Número de caja") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(fecha.trim(), caja.trim()) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Sí, eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(
    correo: String,
    error: String?,
    onCorreoChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar contraseña") },
        text = {
            Column {
                OutlinedTextField(
                    value = correo,
                    onValueChange = onCorreoChange,
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Se enviará un correo para restablecer la contraseña.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Enviar correo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}