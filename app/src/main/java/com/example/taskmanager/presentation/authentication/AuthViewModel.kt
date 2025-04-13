package com.example.taskmanager.presentation.authentication

import androidx.lifecycle.ViewModel
import com.example.taskmanager.domain.usecase.auth.CheckAuthStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val checkAuthStatusUseCase: CheckAuthStatusUseCase
) : ViewModel() {
    
    fun isUserAuthenticated(): Boolean {
        return checkAuthStatusUseCase()
    }
} 