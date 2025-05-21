package com.example.ventaplus.ui.screens

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import android.Manifest
import android.media.Image
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.ventaplus.models.Producto


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen() {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val database = FirebaseDatabase.getInstance().getReference("usuarios").child(uid).child("productos")

    var productos by remember { mutableStateOf(listOf<Producto>()) }
    var isLoading by remember { mutableStateOf(true) }

    var dialogOpen by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Producto?>(null) }
    var deleteConfirmProduct by remember { mutableStateOf<Producto?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var categoriaFiltro by remember { mutableStateOf("Todas") }
    var expandedFiltro by remember { mutableStateOf(false) }

    var scanningBarcode by remember { mutableStateOf(false) }
    var scannedBarcode by remember { mutableStateOf<String?>(null) }

    val categoriasDisponibles = remember(productos) {
        val cats = productos.map { it.categoria }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("Todas") + cats
    }

    LaunchedEffect(Unit) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productos = snapshot.children.mapNotNull {
                    val id = it.key ?: return@mapNotNull null
                    Producto(
                        id = id,
                        nombre = it.child("nombre").getValue(String::class.java) ?: "",
                        precio = it.child("precio").getValue(Double::class.java) ?: 0.0,
                        stock = it.child("stock").getValue(Int::class.java) ?: 0,
                        codigoBarras = it.child("codigoBarras").getValue(String::class.java) ?: "",
                        porPeso = it.child("porPeso").getValue(Boolean::class.java) ?: false,
                        categoria = it.child("categoria").getValue(String::class.java) ?: "",
                        fechaRegistro = it.child("fechaRegistro").getValue(String::class.java) ?: ""
                    )
                }
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error al cargar productos", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
        })
    }

    val filteredProducts = productos.filter { producto ->
        (categoriaFiltro == "Todas" || producto.categoria == categoriaFiltro) &&
                (producto.nombre.contains(searchQuery, ignoreCase = true) ||
                        producto.codigoBarras.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val newCode = generateUniqueBarcode(productos)
                editingProduct = Producto(codigoBarras = newCode)
                dialogOpen = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar producto o código") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                singleLine = true,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedButton(onClick = { expandedFiltro = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Categoría: $categoriaFiltro")
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Filtrar categoría")
                }
                DropdownMenu(
                    expanded = expandedFiltro,
                    onDismissRequest = { expandedFiltro = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categoriasDisponibles.forEach { categoria ->
                        DropdownMenuItem(
                            text = { Text(categoria) },
                            onClick = {
                                categoriaFiltro = categoria
                                expandedFiltro = false
                            }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { scanningBarcode = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear código")
                Spacer(Modifier.width(8.dp))
                Text("Escanear código")
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    if (filteredProducts.isEmpty()) {
                        Text(
                            "No hay productos que coincidan",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.padding(16.dp)) {
                            items(filteredProducts) { producto ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    elevation = CardDefaults.cardElevation(6.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Categoría: ${producto.categoria.ifBlank { "Sin categoría" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Precio: \$${"%.2f".format(producto.precio)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (producto.porPeso) {
                                            Text("Producto por peso", style = MaterialTheme.typography.bodySmall)
                                        } else {
                                            Text("Stock: ${producto.stock}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(
                                            "Código: ${producto.codigoBarras.ifBlank { "No asignado" }}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Registrado: ${producto.fechaRegistro}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(onClick = {
                                                editingProduct = producto
                                                dialogOpen = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                                            }
                                            IconButton(onClick = {
                                                deleteConfirmProduct = producto
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (dialogOpen) {
        ProductoDialogWithScanner(
            producto = editingProduct,
            onDismiss = {
                dialogOpen = false
                editingProduct = null
            },
            onConfirm = { producto ->
                val id = producto.id.ifEmpty { UUID.randomUUID().toString() }
                val fechaRegistroFinal = producto.fechaRegistro.ifBlank {
                    val sdf = SimpleDateFormat("EEEE dd/MM/yyyy hh:mm a", Locale("es", "ES"))
                    sdf.format(Calendar.getInstance().time).replaceFirstChar { it.uppercase() }
                }
                val data = mapOf(
                    "id" to id,
                    "nombre" to producto.nombre,
                    "precio" to producto.precio,
                    "stock" to producto.stock,
                    "codigoBarras" to producto.codigoBarras,
                    "porPeso" to producto.porPeso,
                    "categoria" to producto.categoria,
                    "fechaRegistro" to fechaRegistroFinal
                )
                database.child(id).setValue(data).addOnCompleteListener {
                    Toast.makeText(context, "Producto guardado: ${producto.nombre}", Toast.LENGTH_SHORT).show()
                    dialogOpen = false
                    editingProduct = null
                }
            },
            scannedBarcode = scannedBarcode,
            onScanRequested = {
                scanningBarcode = true
            }
        )
    }

    if (deleteConfirmProduct != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmProduct = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Seguro que quieres eliminar \"${deleteConfirmProduct?.nombre}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirmProduct?.let {
                        database.child(it.id).removeValue().addOnCompleteListener {
                            Toast.makeText(context, "Producto eliminado", Toast.LENGTH_SHORT).show()
                        }
                    }
                    deleteConfirmProduct = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmProduct = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (scanningBarcode) {
        BarcodeScannerDialog(
            onBarcodeScanned = {
                scannedBarcode = it
                scanningBarcode = false
                Toast.makeText(context, "Código escaneado: $it", Toast.LENGTH_SHORT).show()
                // Auto fill code in dialog
                editingProduct = editingProduct?.copy(codigoBarras = it)
                dialogOpen = true
            },
            onDismiss = {
                scanningBarcode = false
            }
        )
    }
}

fun generateUniqueBarcode(existing: List<Producto>): String {
    // Genera un código aleatorio único para código de barras, ejemplo simple
    val existingCodes = existing.map { it.codigoBarras }.toSet()
    var newCode: String
    do {
        newCode = (100000000000..999999999999).random().toString()
    } while (existingCodes.contains(newCode))
    return newCode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoDialogWithScanner(
    producto: Producto?,
    onDismiss: () -> Unit,
    onConfirm: (Producto) -> Unit,
    scannedBarcode: String?,
    onScanRequested: () -> Unit,
) {
    var nombre by remember { mutableStateOf(producto?.nombre ?: "") }
    var precio by remember { mutableStateOf(if (producto?.precio != 0.0) producto?.precio.toString() else "") }
    var stock by remember { mutableStateOf(producto?.stock?.toString() ?: "0") }
    var codigoBarras by remember { mutableStateOf(scannedBarcode ?: producto?.codigoBarras ?: "") }
    var porPeso by remember { mutableStateOf(producto?.porPeso ?: false) }
    var categoria by remember { mutableStateOf(producto?.categoria ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (producto == null || producto.id.isEmpty()) "Agregar Producto" else "Editar Producto") },
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
                    value = precio,
                    onValueChange = { precio = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Precio") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!porPeso) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Checkbox(checked = porPeso, onCheckedChange = {
                        porPeso = it
                        if (it) stock = "0"
                    })
                    Spacer(Modifier.width(8.dp))
                    Text("Producto por peso (sin stock)")
                }
                OutlinedTextField(
                    value = codigoBarras,
                    onValueChange = { codigoBarras = it },
                    label = { Text("Código de barras") },
                    trailingIcon = {
                        IconButton(onClick = onScanRequested) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear código")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoría") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val precioDouble = precio.toDoubleOrNull() ?: 0.0
                val stockInt = stock.toIntOrNull() ?: 0
                if (nombre.isBlank()) return@TextButton
                onConfirm(
                    Producto(
                        id = producto?.id ?: "",
                        nombre = nombre.trim(),
                        precio = precioDouble,
                        stock = stockInt,
                        codigoBarras = codigoBarras.trim(),
                        porPeso = porPeso,
                        categoria = categoria.trim(),
                        fechaRegistro = producto?.fechaRegistro ?: ""
                    )
                )
            }) {
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

@Composable
fun BarcodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }

    var hasPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var camera: Camera? by remember { mutableStateOf(null) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    if (!hasPermission) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Permiso de cámara") },
            text = { Text("La aplicación necesita permiso para usar la cámara para escanear códigos.") },
            confirmButton = {
                TextButton(onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Permitir")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
        return
    }

    DisposableEffect(key1 = Unit) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        @OptIn(ExperimentalGetImage::class)
        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            barcode.rawValue?.let {
                                onBarcodeScanned(it)
                                imageProxy.close()
                                return@addOnSuccessListener
                            }
                        }
                    }
                    .addOnFailureListener {
                        // ignorar errores
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (exc: Exception) {
            Toast.makeText(context, "Error al iniciar cámara", Toast.LENGTH_SHORT).show()
            onDismiss()
        }

        onDispose {
            cameraProvider.unbindAll()
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        text = {
            AndroidView(factory = { previewView }, modifier = Modifier
                .fillMaxWidth()
                .height(300.dp))
        },
        title = { Text("Escanear código de barras") }
    )
}