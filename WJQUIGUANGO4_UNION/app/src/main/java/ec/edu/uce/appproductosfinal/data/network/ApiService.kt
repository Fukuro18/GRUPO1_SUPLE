package ec.edu.uce.appproductosfinal.data.network

import ec.edu.uce.appproductosfinal.model.Product
import ec.edu.uce.appproductosfinal.model.User
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // --- PRODUCTOS ---
    @POST("producto")
    suspend fun syncProduct(@Body product: ProductDto): Response<SyncResponse>

    @GET("producto")
    suspend fun getAllProducts(): Response<List<Product>>

    @DELETE("producto")
    suspend fun deleteProduct(@Query("id") id: Int): Response<Unit>

    // --- USUARIOS ---
    @POST("usuario")
    suspend fun syncUser(@Body user: User): Response<Unit>

    @GET("usuario")
    suspend fun getUser(@Query("nombre") nombre: String): Response<User?>

    // --- AUTHENTICATION (usuario + contraseña) ---
    @POST("auth")
    suspend fun authAction(@Body request: AuthRequest): Response<AuthResponse>

    // --- OTP POR CORREO: solicitar código (LOGINTOKENRECK) ---
    @POST("logintokenreck")
    suspend fun requestOtpCode(@Body request: OtpRequest): Response<OtpResponse>

    // --- CORREO NUEVO PRODUCTO (MAILINSERTREC) ---
    @POST("mailinsertrec")
    suspend fun mailInsertRec(@Body request: MailInsertRequest): Response<MailInsertResponse>

    // --- LOGREC (Registro de transacciones) ---
    @POST("log")
    suspend fun logAction(@Body request: LogRequest): Response<Unit>
}

data class SyncResponse(val message: String, val url: String?)
data class AuthRequest(
    val action: String, 
    val email: String, 
    val password: String? = null,
    val code: String? = null
)
data class AuthResponse(val message: String, val success: Boolean? = null, val debug_code: String? = null)

// OTP
data class OtpRequest(val email: String)
data class OtpResponse(val message: String, val success: Boolean? = null, val code: String? = null)

// Mail producto
data class MailInsertRequest(
    val email_grupo: String,
    val producto_descripcion: String,
    val producto_costo: Double
)
data class MailInsertResponse(val message: String)

// Log
data class LogRequest(
    val accion: String,
    val usuario: String,
    val fecha: String? = null
)
