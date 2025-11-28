package com.syj.geotask.domain.usecase

import com.syj.geotask.domain.model.User
import com.syj.geotask.domain.repository.UserRepository

class RegisterUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(username: String, password: String): Boolean {
        if (userRepository.getUserByUsername(username) == null) {
            userRepository.insert(User(username = username, password = password))
            return true
        } 
        return false
    }
}
