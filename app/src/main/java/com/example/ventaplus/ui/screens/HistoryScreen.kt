package com.example.ventaplus.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.ventaplus.utils.PDFUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

data class VentaProducto(
    val cantidad: Int = 0,
    val categoria: String = "",
    val codigoBarras: String = "",
    val descripcion: String = "",
    val fechaRegistro: String = "",
    val id: String = "",
    val nombre: String = "",
    val porPeso: Boolean = false,
    val precio: Double = 0.0,
    val stock: Int = 0
)

data class Venta(
    val fecha: String = "",
    val productos: Map<String, VentaProducto> = emptyMap(),
    val total: Double = 0.0
)

@Composable
fun HistoryScreen() {
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current

    val allVentas = remember { mutableStateOf<List<Venta>>(emptyList()) }
    val filteredVentas = remember { mutableStateOf<List<Venta>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }

    var searchStartDate by remember { mutableStateOf("") }
    var searchEndDate by remember { mutableStateOf("") }

    val negocioName = remember { mutableStateOf("") }
    val adminName = remember { mutableStateOf("") }

    LaunchedEffect(user?.uid) {
        if (user == null) {
            isLoading.value = false
            return@LaunchedEffect
        }

        val db = FirebaseDatabase.getInstance().reference

        // Obtener negocio y administrador
        db.child("usuarios").child(user.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                negocioName.value = snapshot.child("negocio").getValue(String::class.java) ?: "Negocio"
                adminName.value = snapshot.child("administrador").getValue(String::class.java) ?: "Administrador"

                // Obtener ventas
                db.child("usuarios").child(user.uid).child("ventas")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(ventasSnap: DataSnapshot) {
                            val list = mutableListOf<Venta>()
                            for (ventaSnap in ventasSnap.children) {
                                val venta = ventaSnap.getValue(Venta::class.java)
                                if (venta != null) list.add(venta)
                            }
                            list.sortByDescending { it.fecha }
                            allVentas.value = list
                            filteredVentas.value = list
                            isLoading.value = false
                        }

                        override fun onCancelled(error: DatabaseError) {
                            isLoading.value = false
                            Toast.makeText(context, "Error al cargar ventas: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading.value = false
                Toast.makeText(context, "Error al cargar negocio/admin: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = searchStartDate,
                onValueChange = {
                    searchStartDate = it
                    applyDateFilter(searchStartDate, searchEndDate, allVentas.value, filteredVentas)
                },
                label = { Text("Fecha inicio (yyyy-MM-dd)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = searchEndDate,
                onValueChange = {
                    searchEndDate = it
                    applyDateFilter(searchStartDate, searchEndDate, allVentas.value, filteredVentas)
                },
                label = { Text("Fecha fin (yyyy-MM-dd)") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (filteredVentas.value.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se encontraron ventas en el rango", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredVentas.value) { venta ->
                        VentaItem(venta, negocioName.value, adminName.value)
                    }
                }
            }
        }
    }
}

fun applyDateFilter(start: String, end: String, allVentas: List<Venta>, filteredVentas: MutableState<List<Venta>>) {
    try {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = formatter.parse(start)
        val endDate = formatter.parse(end)
        if (startDate != null && endDate != null) {
            filteredVentas.value = allVentas.filter {
                val ventaDate = formatter.parse(it.fecha.takeLast(10))
                ventaDate != null && !ventaDate.before(startDate) && !ventaDate.after(endDate)
            }
        }
    } catch (_: Exception) {
        filteredVentas.value = allVentas
    }
}

@Composable
fun VentaItem(venta: Venta, negocio: String, admin: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fecha: ${venta.fecha}", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            venta.productos.values.forEach { producto ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${producto.nombre} x${producto.cantidad}", style = MaterialTheme.typography.bodyMedium)
                    Text("$${producto.precio}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Total: $${venta.total}", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary))
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val pdfFile = PDFUtils.generatePDF(context, venta, negocio, admin, com.example.ventaplus.R.drawable.logo)
                    pdfFile?.let {
                        val uri = FileProvider.getUriForFile(context, "com.example.ventaplus.provider", it)
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, uri)
                            type = "application/pdf"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir ticket"))
                    }
                }) {
                    Text("Compartir")
                }
            }
        }
    }
}