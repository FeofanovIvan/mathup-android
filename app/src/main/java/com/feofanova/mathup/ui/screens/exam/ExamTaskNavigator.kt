package com.feofanova.mathup.ui.screens.exam

object ExamTaskNavigator {
    fun findNextTaskId(
        taskIds: List<Long>,
        currentTaskId: Long?,
        completedTaskIds: Set<Long>
    ): Long? {
        if (taskIds.isEmpty()) return currentTaskId

        val currentIndex = taskIds.indexOf(currentTaskId)
        val searchStartIndex = if (currentIndex >= 0) currentIndex + 1 else 0

        val nextUnanswered = taskIds
            .drop(searchStartIndex)
            .firstOrNull { it !in completedTaskIds }

        return when {
            nextUnanswered != null -> nextUnanswered
            currentIndex >= 0 && currentIndex + 1 < taskIds.size -> taskIds[currentIndex + 1]
            else -> currentTaskId
        }
    }
}
