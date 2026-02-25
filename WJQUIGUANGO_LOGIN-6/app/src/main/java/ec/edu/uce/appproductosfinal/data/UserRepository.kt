package ec.edu.uce.appproductosfinal.data

import ec.edu.uce.appproductosfinal.model.User

class UserRepository(private val userDao: UserDao) {
    suspend fun getUsers(): List<User> = userDao.getAllUsers()

    suspend fun addUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun findUser(username: String, password: String): User? {
        return userDao.findUser(username, password)
    }
}
