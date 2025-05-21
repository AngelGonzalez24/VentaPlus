package com.example.ventaplus.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ventaplus.models.Producto
import com.example.ventaplus.ui.theme.VentaPlusTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.*

data class ProductoEnCarrito(
    val producto: Producto,
    var cantidad: Int = 1
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SalesScreen() {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val database = FirebaseDatabase.getInstance()

    val productosEnVenta = remember { mutableStateListOf<ProductoEnCarrito>() }
    var codigoManual by remember { mutableStateOf(TextFieldValue("")) }
    var totalVenta by remember { mutableStateOf(0.0) }
    var productoDuplicado by remember { mutableStateOf<Producto?>(null) }
    var productoAEliminar by remember { mutableStateOf<ProductoEnCarrito?>(null) }

    // Función única para calcular el total, con logs para depuración
    fun calcularTotal() {
        Log.d("DEBUG", "Productos en carrito:")
        productosEnVenta.forEach {
            Log.d("DEBUG", "${it.producto.nombre} x${it.cantidad} = ${it.producto.precio * it.cantidad}")
        }
        totalVenta = productosEnVenta.sumOf { it.producto.precio * it.cantidad }
        Log.d("DEBUG", "Total venta: $totalVenta")
    }

    fun buscarProductoPorCodigo(codigo: String, onFound: (Producto?) -> Unit) {
        val ref = database.getReference("usuarios/$uid/productos")
        ref.get().addOnSuccessListener { snapshot ->
            val producto = snapshot.children.mapNotNull {
                it.getValue(Producto::class.java)
            }.find { it.codigoBarras == codigo }
            onFound(producto)
        }.addOnFailureListener {
            Toast.makeText(context, "Error al buscar producto", Toast.LENGTH_SHORT).show()
            onFound(null)
        }
    }

    fun agregarProductoAlCarrito(producto: Producto) {
        val existente = productosEnVenta.find { it.producto.codigoBarras == producto.codigoBarras }
        if (existente != null) {
            productoDuplicado = producto
        } else {
            productosEnVenta.add(ProductoEnCarrito(producto))
        }
        calcularTotal() // Se llama siempre para mantener el total actualizado
    }

    fun confirmarAgregarDuplicado(producto: Producto) {
        val existente = productosEnVenta.find { it.producto.codigoBarras == producto.codigoBarras }
        if (existente != null) {
            existente.cantidad++
        } else {
            productosEnVenta.add(ProductoEnCarrito(producto))
        }
        calcularTotal()
        productoDuplicado = null
    }

    fun registrarVenta() {
        if (productosEnVenta.isEmpty()) {
            Toast.makeText(context, "No hay productos en la venta", Toast.LENGTH_SHORT).show()
            return
        }

        val ventasRef = database.getReference("usuarios/$uid/ventas")
        val nuevaVentaRef = ventasRef.push()
        val fechaFormateada = SimpleDateFormat("EEEE dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        val productosMap = productosEnVenta.associate {
            it.producto.id to mapOf(
                "nombre" to it.producto.nombre,
                "precio" to it.producto.precio,
                "cantidad" to it.cantidad
            )
        }

        val ventaData = mapOf(
            "fecha" to fechaFormateada,
            "productos" to productosMap,
            "total" to totalVenta
        )

        nuevaVentaRef.setValue(ventaData).addOnSuccessListener {
            val productosRef = database.getReference("usuarios/$uid/productos")
            productosEnVenta.forEach { item ->
                val productoRef = productosRef.child(item.producto.id)
                productoRef.child("stock").get().addOnSuccessListener { snapshot ->
                    val stockActual = snapshot.getValue(Int::class.java) ?: 0
                    val nuevoStock = (stockActual - item.cantidad).coerceAtLeast(0)
                    productoRef.child("stock").setValue(nuevoStock)
                }
            }
            productosEnVenta.clear()
            totalVenta = 0.0
            Toast.makeText(context, "Venta registrada con éxito", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, "Error al registrar venta", Toast.LENGTH_SHORT).show()
        }
    }

    fun eliminarProductoDelCarrito(item: ProductoEnCarrito) {
        productosEnVenta.remove(item)
        calcularTotal()
        productoAEliminar = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Registrar Venta", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(8.dp))

        CameraBarcodeScanner { code ->
            buscarProductoPorCodigo(code) { producto ->
                if (producto != null) {
                    agregarProductoAlCarrito(producto)
                } else {
                    Toast.makeText(context, "Producto no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = codigoManual,
            onValueChange = { codigoManual = it },
            label = { Text("Ingresar código de barras") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                buscarProductoPorCodigo(codigoManual.text.trim()) { producto ->
                    if (producto != null) {
                        agregarProductoAlCarrito(producto)
                        codigoManual = TextFieldValue("")
                    } else {
                        Toast.makeText(context, "Producto no encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 4.dp)
        ) {
            Text("Agregar producto")
        }

        Spacer(Modifier.height(16.dp))

        Text("Carrito de venta", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            itemsIndexed(productosEnVenta, key = { _, item -> item.producto.id }) { _, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .animateItemPlacement(tween(300)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = item.producto.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Precio: $${"%.2f".format(item.producto.precio)} x${item.cantidad}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Subtotal: $${"%.2f".format(item.producto.precio * item.cantidad)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (item.cantidad > 1) {
                                    item.cantidad--
                                } else {
                                    productoAEliminar = item
                                }
                                calcularTotal()
                            }) {
                                Icon(Icons.Default.Remove, contentDescription = "Menos")
                            }
                            IconButton(onClick = {
                                item.cantidad++
                                calcularTotal()
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Más")
                            }
                            IconButton(onClick = {
                                productoAEliminar = item
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("Total: $${"%.2f".format(totalVenta)}", style = MaterialTheme.typography.headlineSmall)

        Button(
            onClick = { registrarVenta() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Icon(Icons.Default.Done, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Finalizar Venta")
        }

        // Confirmación producto duplicado
        if (productoDuplicado != null) {
            AlertDialog(
                onDismissRequest = { productoDuplicado = null },
                confirmButton = {
                    TextButton(onClick = {
                        confirmarAgregarDuplicado(productoDuplicado!!)
                    }) {
                        Text("Agregar otra vez")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { productoDuplicado = null }) {
                        Text("Cancelar")
                    }
                },
                title = { Text("Producto ya agregado") },
                text = { Text("Este producto ya está en el carrito. ¿Deseas agregar otra unidad?") }
            )
        }

        // Confirmación eliminar producto
        if (productoAEliminar != null) {
            AlertDialog(
                onDismissRequest = { productoAEliminar = null },
                title = { Text("Eliminar producto") },
                text = { Text("¿Estás seguro que deseas eliminar \"${productoAEliminar!!.producto.nombre}\" del carrito?") },
                confirmButton = {
                    TextButton(onClick = {
                        eliminarProductoDelCarrito(productoAEliminar!!)
                    }) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { productoAEliminar = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun CameraBarcodeScanner(onBarcodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val hasCameraPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission.value = granted
        if (!granted) {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission.value) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission.value) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            factory = { ctx ->
                val previewView = PreviewView(ctx)

                val cameraProvider = cameraProviderFuture.get()
                val preview = CameraPreview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
                val scanner = BarcodeScanning.getClient(options)

                val analysisUseCase = ImageAnalysis.Builder()
                    .build()

                analysisUseCase.setAnalyzer(
                    ContextCompat.getMainExecutor(ctx)
                ) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val code = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                                if (code != null) {
                                    onBarcodeScanned(code)
                                }
                            }
                            .addOnFailureListener {
                                Log.e("BarcodeScanner", "Error al procesar imagen", it)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysisUseCase
                )
                previewView
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SalesScreenPreview() {
    VentaPlusTheme {
        SalesScreen()
    }
}