package ec.edu.uce.appproductosfinal.data.network

data class ProductDto(
    val id: Int,
    val descripcion: String,
    val fechaFabricacion: Long,
    val costo: Double,
    val disponibilidad: Boolean,
    val imageUri: String?, // Link de S3 o URI Local
    val lastUpdated: Long,
    val imageBase64: String? = null // La foto en formato texto para AWS
)
