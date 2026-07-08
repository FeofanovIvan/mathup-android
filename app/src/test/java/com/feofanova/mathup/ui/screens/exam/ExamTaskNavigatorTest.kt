package com.feofanova.mathup.ui.screens.exam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExamTaskNavigatorTest {

    @Test
    fun findsNextUnansweredAfterCurrentTask() {
        val result = ExamTaskNavigator.findNextTaskId(
            taskIds = listOf(1, 2, 3, 4),
            currentTaskId = 1,
            completedTaskIds = setOf(2)
        )

        assertEquals(3L, result)
    }

    @Test
    fun fallsBackToNextTaskWhenAllFollowingTasksAreCompleted() {
        val result = ExamTaskNavigator.findNextTaskId(
            taskIds = listOf(1, 2, 3),
            currentTaskId = 1,
            completedTaskIds = setOf(2, 3)
        )

        assertEquals(2L, result)
    }

    @Test
    fun staysOnCurrentTaskWhenCurrentTaskIsLast() {
        val result = ExamTaskNavigator.findNextTaskId(
            taskIds = listOf(1, 2, 3),
            currentTaskId = 3,
            completedTaskIds = emptySet()
        )

        assertEquals(3L, result)
    }

    @Test
    fun returnsFirstUnansweredWhenCurrentTaskIsMissing() {
        val result = ExamTaskNavigator.findNextTaskId(
            taskIds = listOf(1, 2, 3),
            currentTaskId = 99,
            completedTaskIds = setOf(1)
        )

        assertEquals(2L, result)
    }

    @Test
    fun keepsNullCurrentTaskWhenThereAreNoTasks() {
        val result = ExamTaskNavigator.findNextTaskId(
            taskIds = emptyList(),
            currentTaskId = null,
            completedTaskIds = emptySet()
        )

        assertNull(result)
    }
}
