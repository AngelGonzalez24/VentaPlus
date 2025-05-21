package com.example.ventaplus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ventaplus.ui.theme.VentaPlusTheme

@Composable
fun ClientsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Gestión de Clientes", style = MaterialTheme.typography.headlineSmall)
    }
}

@Preview(showBackground = true)
@Composable
fun ClientsScreenPreview() {
    VentaPlusTheme {
        ClientsScreen()
    }
}