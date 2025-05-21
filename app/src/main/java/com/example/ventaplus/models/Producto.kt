package com.example.ventaplus.models

data class Producto(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val precio: Double = 0.0,
    val stock: Int = 0,
    val categoria: String = "",
    val codigoBarras: String = "",
    val fechaRegistro: String = "",
    val porPeso: Boolean = false
    // ❌ Eliminar: val cantidad: Int = 0
)