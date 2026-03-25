package com.saokt.taskmanager.domain.model

enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
}

fun canonicalTaskStatus(status: TaskStatus?, completed: Boolean): TaskStatus {
    return when {
        status != null -> status
        completed -> TaskStatus.DONE
        else -> TaskStatus.TODO
    }
}

fun Task.canonicalizedStatus(): Task {
    val normalizedStatus = canonicalTaskStatus(status, completed)
    val normalizedCompleted = normalizedStatus == TaskStatus.DONE
    return if (status == normalizedStatus && completed == normalizedCompleted) {
        this
    } else {
        copy(status = normalizedStatus, completed = normalizedCompleted)
    }
}
