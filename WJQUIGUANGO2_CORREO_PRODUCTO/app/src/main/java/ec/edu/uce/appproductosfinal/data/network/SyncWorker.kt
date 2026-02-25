package ec.edu.uce.appproductosfinal.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ec.edu.uce.appproductosfinal.R
import ec.edu.uce.appproductosfinal.data.AppDatabase
import ec.edu.uce.appproductosfinal.data.ProductRepository
import ec.edu.uce.appproductosfinal.model.Product
import java.io.ByteArrayOutputStream
import java.io.InputStream

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ProductRepository(database.productDao())
        
        return try {
            val localProducts = repository.getProducts()
            
            // Sincronización de salida (Subir locales a AWS)
            for (product in localProducts) {
                try {
                    val productDto = product.toDto(applicationContext)
                    val response = RetrofitClient.instance.syncProduct(productDto)
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.url != null && body.url.startsWith("http")) {
                            repository.updateProduct(product.copy(imageUri = body.url))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Error sincronizando producto ${product.id}", e)
                }
            }

            // Cumplimiento del requerimiento: Notificar con el número exacto de productos locales
            val totalLocalProducts = repository.getProducts().size
            showNotification(totalLocalProducts)

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Fallo en la tarea de actualización", e)
            Result.retry()
        }
    }

    private fun showNotification(totalLocalCount: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "inventory_sync_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Actualización de Inventario", 
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Sincronización Finalizada")
            .setContentText("Se han procesado $totalLocalCount productos en tu base de datos local.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun Product.toDto(context: Context): ProductDto {
        var base64: String? = null
        if (!imageUri.isNullOrEmpty() && !imageUri!!.startsWith("http")) {
            try {
                val uri = Uri.parse(imageUri)
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val bytes = outputStream.toByteArray()
                base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (e: Exception) {
                Log.e("SyncWorker", "Error Base64", e)
            }
        }

        return ProductDto(
            id = id,
            descripcion = descripcion,
            fechaFabricacion = fechaFabricacion,
            costo = costo,
            disponibilidad = disponibilidad,
            imageUri = imageUri,
            lastUpdated = lastUpdated,
            imageBase64 = base64
        )
    }
}
