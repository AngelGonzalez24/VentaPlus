package com.example.ventaplus.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ventaplus.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val database = FirebaseDatabase.getInstance().reference

    var negocioNombre by remember { mutableStateOf("Cargando...") }
    var negocioDireccion by remember { mutableStateOf("Ubicación no disponible") }

    LaunchedEffect(currentUserId) {
        try {
            val negocioId = database.child("usuarios").child(currentUserId!!).child("negocioId")
                .get().await().getValue(String::class.java)

            negocioId?.let {
                val negocioSnapshot = database.child("negocios").child(it).get().await()
                negocioNombre = negocioSnapshot.child("nombre").getValue(String::class.java) ?: "Negocio"
                negocioDireccion = negocioSnapshot.child("direccion").getValue(String::class.java) ?: "Dirección desconocida"
            }
        } catch (e: Exception) {
            Log.e("DashboardScreen", "Error al cargar datos del negocio", e)
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
                        Text(text = negocioNombre, fontSize = 20.sp)
                        Text(text = negocioDireccion, fontSize = 14.sp, color = Color.Gray)
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
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Panel principal",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class DashboardItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)