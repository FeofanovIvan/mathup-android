package com.feofanova.mathup.testing


import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.feofanova.mathup.ui.components.MathAnswerChecker

@Composable
fun TestScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Отладочная надпись
        Log.d("AutoTest", "🔧 TestScreen запущен. Кол-во тестов: ${testCases.size}")

        Column(modifier = Modifier.height(1.dp)) {
            testCases.forEachIndexed { index, test ->
                MathAnswerChecker(
                    userLatex = test.userLatex,
                    correctLatex = test.correctLatex,
                    onResult = { actual ->
                        if (actual == test.expectedResult) {
                            Log.d("AutoTest", "✅ Test $index passed")
                        } else {
                            Log.e("AutoTest", """
                                ❌ Test $index failed
                                user: ${test.userLatex}
                                correct: ${test.correctLatex}
                                expected: ${test.expectedResult}
                                got: $actual
                            """.trimIndent())
                        }
                    }
                )
            }
        }
    }
}
