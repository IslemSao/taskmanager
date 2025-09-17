package com.saokt.taskmanager.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.domain.model.ChatMessage
import com.saokt.taskmanager.domain.usecase.chat.CreateOrGetThreadUseCase
import com.saokt.taskmanager.domain.usecase.chat.ListenMessagesUseCase
import com.saokt.taskmanager.domain.usecase.chat.MarkThreadReadUseCase
import com.saokt.taskmanager.domain.usecase.chat.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val threadId: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val createOrGetThread: CreateOrGetThreadUseCase,
    private val listenMessages: ListenMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markThreadRead: MarkThreadReadUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun initThread(projectId: String, taskId: String?, participants: List<String>) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = createOrGetThread(projectId, taskId, participants)
            result.onSuccess { thread ->
                _state.update { it.copy(threadId = thread.id, loading = false) }
                viewModelScope.launch {
                    listenMessages(thread.id).collectLatest { msgs ->
                        _state.update { it.copy(messages = msgs) }
                    }
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun sendMessage(senderId: String, text: String) {
        val tid = state.value.threadId ?: return
        viewModelScope.launch {
            val msg = ChatMessage(threadId = tid, senderId = senderId, text = text)
            sendMessageUseCase(tid, msg)
        }
    }

    fun markRead(userId: String) {
        val tid = state.value.threadId ?: return
        viewModelScope.launch { markThreadRead(tid, userId) }
    }
}
