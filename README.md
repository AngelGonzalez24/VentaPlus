**# 📱 VentaPlus

**VentaPlus** es una aplicación Android desarrollada con **Kotlin**, **Jetpack Compose** y **Firebase**, diseñada para gestionar ventas en pequeños negocios, tiendas familiares o establecimientos con múltiples empleados. La app es moderna, rápida y adaptable tanto para un solo usuario como para equipos de trabajo.

---

## 🚀 Funcionalidades principales

### 🛒 Ventas
- Escaneo de productos por código de barras con **CameraX + ML Kit**.
- Agrega productos al carrito y calcula el total automáticamente.
- Guarda ventas reales en **Firebase Realtime Database** con detalles y hora.

### 📈 Historial de ventas
- Consulta de ventas por día o por rango de fechas.
- Exportación de tickets a **PDF** incluyendo:
  - Logo del negocio
  - Nombre del administrador
  - Detalles del ticket (productos, total, fecha)
- Botones para **compartir por WhatsApp**, **descargar** o **imprimir**.

### 👥 Gestión de empleados
- Agregar empleados al negocio con su propio correo y contraseña.
- Se crean cuentas automáticamente en **Firebase Authentication**.
- Permite cambiar o restablecer contraseña desde la app.

### 👨‍💼 Gestión de turnos
- Registro de turnos: empleado en turno, hora de entrada y salida, número de caja.
- Visualización y eliminación de turnos.
- Filtrado por fecha y número de caja.

### ⚙️ Configuración avanzada
- Editar los datos del negocio (nombre, dirección, etc.).
- Cambiar contraseña del administrador.
- Ver, agregar, editar y eliminar empleados y turnos desde una sola pantalla.

### 🧾 Productos y categorías
- Registro y visualización de productos clasificados por categoría.
- Manejo correcto de cantidades vendidas por cada producto.

### 👤 Modo usuario único
- Soporte para negocios familiares donde no se requiere gestión de empleados.
- Simplificación de la app para un solo administrador.

---

## 🧱 Tecnologías utilizadas

- **Kotlin**
- **Jetpack Compose**
- **Firebase** (Realtime Database, Authentication)
- **CameraX** + **ML Kit** (para escaneo de productos)
- **Android Studio**
- **PDF export**, **WhatsApp Share**, **Responsive UI**

---

## 📁 Estructura del proyecto

```bash
VentaPlus/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/ventaplus/
│   │       │   └── ui/screens/...
│   │       ├── res/
│   │       └── AndroidManifest.xml
├── build.gradle
├── settings.gradle
└── README.md
**
