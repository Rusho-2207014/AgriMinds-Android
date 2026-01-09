package com.agriminds.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agriminds.data.repository.AIRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIAdvisorViewModel @Inject constructor(
    private val aiRepository: AIRepository
) : ViewModel() {

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun askQuestion(question: String, category: String = "General") {
        if (question.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Add user message
            _chatMessages.value = _chatMessages.value + ChatMessage(
                text = question,
                isUser = true,
                timestamp = System.currentTimeMillis()
            )

            // Get AI response
            aiRepository.getAgriculturalAdvice(question, category)
                .onSuccess { answer ->
                    _chatMessages.value = _chatMessages.value + ChatMessage(
                        text = answer,
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to get response"
                }

            _isLoading.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }

    fun clearError() {
        _error.value = null
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)
