package com.example.ventaplus.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var negocioNombre by remember { mutableStateOf<String?>(null) }
    var negocioDireccion by remember { mutableStateOf<String?>(null) }
    var errorLoading by remember { mutableStateOf<String?>(null) }

    // Carga datos negocio desde "usuarios/{uid}/negocio" y "usuarios/{uid}/direccion"
    LaunchedEffect(currentUserId) {
        if (currentUserId == null) {
            errorLoading = "Usuario no autenticado"
            return@LaunchedEffect
        }
        try {
            val userRef = FirebaseDatabase.getInstance().reference.child("usuarios").child(currentUserId)
            val snapshot = userRef.get().await()
            negocioNombre = snapshot.child("negocio").getValue(String::class.java) ?: "Negocio sin nombre"
            negocioDireccion = snapshot.child("direccion").getValue(String::class.java) ?: "Dirección no especificada"
        } catch (e: Exception) {
            Log.e("DashboardScreen", "Error al cargar datos del negocio", e)
            errorLoading = "Error cargando datos"
        }
    }

    val dashboardItems = listOf(
        DashboardItem("Ventas", Icons.Default.PointOfSale, "venta"),
        DashboardItem("Historial", Icons.Default.History, "historial"),
        DashboardItem("Productos", Icons.Default.Inventory, "productos"),
        DashboardItem("Categorías", Icons.Default.Category, "categorias"),
        DashboardItem("Empleados", Icons.Default.Group, "empleados"),
        DashboardItem("Turnos", Icons.Default.Schedule, "turnos"),
        DashboardItem("Reportes", Icons.Default.BarChart, "reportes"),
        DashboardItem("Configuración", Icons.Default.Settings, "configuracion")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = negocioNombre ?: "Cargando...",
                            fontSize = 22.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            text = negocioDireccion ?: "",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (errorLoading != null) {
                Text(
                    text = errorLoading!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            Text(
                text = "Panel Principal",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
            )

            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(dashboardItems) { item ->
                    DashboardCard(item = item, onClick = {
                        navController.navigate(item.route)
                    })
                }
            }
        }
    }
}

@Composable
fun DashboardCard(item: DashboardItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        }
    }
}

data class DashboardItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)