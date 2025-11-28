package com.syj.geotask.data.repository

import com.syj.geotask.data.datasource.local.UserDao
import com.syj.geotask.domain.model.User
import com.syj.geotask.domain.repository.UserRepository

class UserRepositoryImpl(private val userDao: UserDao) : UserRepository {
    override suspend fun insert(user: User) {
        userDao.insert(user)
    }

    override suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }
}
