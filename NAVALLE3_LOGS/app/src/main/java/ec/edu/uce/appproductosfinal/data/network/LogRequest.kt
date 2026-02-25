package ec.edu.uce.appproductosfinal.data.network

data class LogRequest(
    val accion: String,   // "Ingresar", "Creación", "Actualización", "Eliminación"
    val usuario: String
)
