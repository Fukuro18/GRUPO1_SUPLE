package ec.edu.uce.appproductosfinal.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val descripcion: String,
    val fechaFabricacion: Long, // Cambiado a Long (Timestamp) para máxima estabilidad
    val costo: Double,
    var disponibilidad: Boolean,
    val imageUri: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
