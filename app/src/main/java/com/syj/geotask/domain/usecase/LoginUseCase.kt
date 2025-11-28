package com.syj.geotask.domain.usecase

import com.syj.geotask.domain.repository.UserRepository

class LoginUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(username: String, password: String): Boolean {
        val user = userRepository.getUserByUsername(username)
        return user != null && user.password == password
    }
}
