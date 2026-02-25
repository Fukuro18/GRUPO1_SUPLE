package ec.edu.uce.appproductosfinal.data

import ec.edu.uce.appproductosfinal.model.Product
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
    // Observar productos en tiempo real
    fun getProductsFlow(): Flow<List<Product>> = productDao.getAllProductsFlow()

    suspend fun getProducts(): List<Product> = productDao.getAllProducts()

    suspend fun addProduct(product: Product): Long {
        return productDao.insertProduct(product)
    }

    suspend fun updateProduct(updatedProduct: Product) {
        productDao.updateProduct(updatedProduct)
    }

    suspend fun deleteProduct(id: Int) {
        productDao.deleteProductById(id)
    }
}
