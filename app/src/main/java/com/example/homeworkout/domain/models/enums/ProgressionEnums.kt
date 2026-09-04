package com.example.homeworkout.domain.models.enums

/** One of the four calisthenics skill-tree branches on the Discovery screen. */
enum class ProgressionBranch {
    PUSH, PULL, LEGS, CORE
}

/** Mastery state of a single [com.example.homeworkout.domain.models.ProgressionNode]. */
enum class ProgressionNodeStatus {
    LOCKED, IN_PROGRESS, MASTERED
}
