package com.example.homeworkout.domain.running

import com.example.homeworkout.domain.models.running.RunStatus

class RunStateMachine {
    fun transition(from: RunStatus?, to: RunStatus): Boolean = when (from) {
        null -> to == RunStatus.RUNNING
        RunStatus.RUNNING -> to == RunStatus.PAUSED || to == RunStatus.FINISHED || to == RunStatus.ERROR
        RunStatus.PAUSED -> to == RunStatus.RUNNING || to == RunStatus.FINISHED || to == RunStatus.ERROR
        RunStatus.FINISHED, RunStatus.ERROR -> false
    }
}
