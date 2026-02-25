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
}

data class SyncResponse(val message: String, val url: String?)
