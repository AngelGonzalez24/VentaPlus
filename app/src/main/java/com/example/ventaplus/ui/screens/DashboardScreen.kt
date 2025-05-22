package com.example.ventaplus.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var negocioNombre by remember { mutableStateOf<String?>(null) }
    var negocioDireccion by remember { mutableStateOf<String?>(null) }
    var errorLoading by remember { mutableStateOf<String?>(null) }

    // Estados para indicadores
    var totalVentas by remember { mutableStateOf<Int?>(null) }
    var totalProductos by remember { mutableStateOf<Int?>(null) }
    var totalEmpleados by remember { mutableStateOf<Int?>(null) }
    var turnosActivos by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(currentUserId) {
        if (currentUserId == null) {
            errorLoading = "Usuario no autenticado"
            return@LaunchedEffect
        }
        try {
            val db = FirebaseDatabase.getInstance().reference

            // Obtener info negocio
            val userSnapshot = db.child("usuarios").child(currentUserId).get().await()
            negocioNombre = userSnapshot.child("negocio").getValue(String::class.java) ?: "Negocio sin nombre"
            negocioDireccion = userSnapshot.child("direccion").getValue(String::class.java) ?: "Dirección no especificada"
            val negocioId = userSnapshot.child("negocioId").getValue(String::class.java) // si tienes negocioId

            // Si tienes negocioId, usarlo para filtrar datos
            val filterPath = negocioId ?: currentUserId // fallback

            // Total ventas: contar nodos bajo /ventas/{negocioId} (suponiendo)
            totalVentas = try {
                val ventasSnapshot = db.child("ventas").child(filterPath).get().await()
                ventasSnapshot.childrenCount.toInt()
            } catch (e: Exception) {
                0
            }

            // Total productos: contar nodos bajo /productos/{negocioId}
            totalProductos = try {
                val productosSnapshot = db.child("productos").child(filterPath).get().await()
                productosSnapshot.childrenCount.toInt()
            } catch (e: Exception) {
                0
            }

            // Total empleados: contar nodos bajo /empleados/{negocioId}
            totalEmpleados = try {
                val empleadosSnapshot = db.child("empleados").child(filterPath).get().await()
                empleadosSnapshot.childrenCount.toInt()
            } catch (e: Exception) {
                0
            }

            // Turnos activos: contar nodos bajo /turnos/{negocioId} donde activo=true (asumiendo)
            turnosActivos = try {
                val turnosSnapshot = db.child("turnos").child(filterPath).get().await()
                turnosSnapshot.children.filter {
                    it.child("activo").getValue(Boolean::class.java) == true
                }.size
            } catch (e: Exception) {
                0
            }

        } catch (e: Exception) {
            Log.e("DashboardScreen", "Error al cargar datos del negocio o indicadores", e)
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
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = negocioDireccion ?: "",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Indicadores resumen
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    title = "Ventas",
                    count = totalVentas,
                    icon = Icons.Default.PointOfSale,
                    color = MaterialTheme.colorScheme.primary
                )
                SummaryCard(
                    title = "Productos",
                    count = totalProductos,
                    icon = Icons.Default.Inventory,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    title = "Empleados",
                    count = totalEmpleados,
                    icon = Icons.Default.Group,
                    color = MaterialTheme.colorScheme.tertiary
                )
                SummaryCard(
                    title = "Turnos Activos",
                    count = turnosActivos,
                    icon = Icons.Default.Schedule,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
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
fun SummaryCard(
    title: String,
    count: Int?,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier
            .height(110.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.medium
    )
    {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                )
                Text(
                    text = count?.toString() ?: "—",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
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
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)