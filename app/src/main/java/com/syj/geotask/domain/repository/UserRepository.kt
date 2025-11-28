package com.syj.geotask.domain.repository

import com.syj.geotask.domain.model.User

interface UserRepository {
    suspend fun insert(user: User)
    suspend fun getUserByUsername(username: String): User?
}
