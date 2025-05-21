package com.example.ventaplus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.ventaplus.ui.screens.*  // Aquí importa todas las pantallas, incluida ConfiguracionScreen completa
import com.example.ventaplus.ui.theme.VentaPlusTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Inicio", Icons.Default.SpaceDashboard)
    object Products : Screen("products", "Productos", Icons.Default.Inventory)
    object Sales : Screen("sales", "Ventas", Icons.Default.PointOfSale)
    object History : Screen("history", "Historial", Icons.Default.ReceiptLong)
    object Clients : Screen("clients", "Clientes", Icons.Default.Person)
    object LoyaltyPoints : Screen("loyalty", "Puntos de Lealtad", Icons.Default.Star)
    object Configuracion : Screen("configuracion", "Configuración", Icons.Default.Settings)
    object Logout : Screen("logout", "Salir", Icons.Default.ExitToApp)

    companion object {
        val all = listOf(
            Dashboard, Products, Sales, History,
            Clients, LoyaltyPoints, Configuracion, Logout
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VentaPlusTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    val currentRoute = currentBackStackRoute(navController)

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    var adminName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        if (userId != null) {
            val dbRef = FirebaseDatabase.getInstance().getReference("usuarios/$userId")
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    adminName = snapshot.child("administrador").getValue(String::class.java) ?: ""
                    businessName = snapshot.child("negocio").getValue(String::class.java) ?: ""
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (adminName.isNotBlank() && businessName.isNotBlank()) {
                        Text("Administrador: $adminName", style = MaterialTheme.typography.bodyLarge)
                        Text("Negocio: $businessName", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        "Menú",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                Divider()
                Screen.all.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        onClick = {
                            if (screen == Screen.Logout) {
                                showLogoutDialog = true
                            } else {
                                navController.navigate(screen.route) {
                                    launchSingleTop = true
                                    popUpTo(navController.graph.startDestinationId)
                                }
                                scope.launch { drawerState.close() }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(Screen.all.find { it.route == currentRoute }?.title ?: "VentaPlus")
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
                    composable(Screen.Dashboard.route) { DashboardScreen(navController) }
                    composable(Screen.Products.route) { ProductsScreen() }
                    composable(Screen.Sales.route) { SalesScreen() }
                    composable(Screen.History.route) { HistoryScreen() }
                    composable(Screen.Clients.route) { ClientsScreen() }
                    composable(Screen.LoyaltyPoints.route) { LoyaltyPointsScreen() }
                    composable(Screen.Configuracion.route) {
                        val database = FirebaseDatabase.getInstance().reference
                        // Aquí se llama a la pantalla completa de configuración, que está en otro archivo
                        ConfiguracionScreen(database = database)
                    }
                }
            }

            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text("Confirmar salida") },
                    text = { Text("¿Estás seguro que deseas cerrar sesión?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showLogoutDialog = false
                            FirebaseAuth.getInstance().signOut()
                            context.startActivity(Intent(context, LoginActivity::class.java))
                            if (context is ComponentActivity) context.finish()
                        }) {
                            Text("Sí")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("No")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun currentBackStackRoute(navController: NavHostController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    VentaPlusTheme {
        MainScreen()
    }
}