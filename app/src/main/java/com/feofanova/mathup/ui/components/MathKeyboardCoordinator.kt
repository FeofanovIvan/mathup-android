package com.feofanova.mathup.ui.components

import android.webkit.WebView
import android.webkit.WebSettings


/**
 * Аналог MathKeyboardCoordinator.swift, переведённый на Kotlin для Android.
 * Хранит "левую" и "правую" части LaTeX-строки, обновляет WebView по событию изменений.
 */
class MathKeyboardCoordinator {

    // WebView, куда мы будем вставлять отрисовку LaTeX (через MathJax)
    private var webView: WebView? = null

    // "Левая" часть ввода (то, что слева от курсора)
    private val left = mutableListOf<String>()
    // "Правая" часть (в обратном порядке) — куда перемещаются закрывающие скобки и т.д.
    private val right = mutableListOf<String>()

    // Коллбэк, чтобы SwiftUI (здесь Compose) узнал, что latex изменился
    // В Kotlin-версии можем хранить лямбду, устанавливаемую извне
    var onLatexUpdate: ((String) -> Unit)? = null

    // Maps symbols from button titles to LaTeX коды
    private val symbolMap: Map<String, String> = mapOf(
        "sin" to "\\sin", "cos" to "\\cos", "tg" to "\\tan", "ctg" to "\\cot",
        "asin" to "\\arcsin", "acos" to "\\arccos", "atg" to "\\arctan", "actg" to "\\arccot",
        "<" to "<", "U" to "\\cup ", ">" to ">",
        "+" to "+", "1" to "1", "2" to "2", "3" to "3", "x" to "x", "y" to "y", "°" to "^\\circ", "CE" to "CE",
        "-" to "-", "4" to "4", "5" to "5", "6" to "6", "a" to "a", "b" to "b", "c" to "c", "стереть" to "стереть",
        "×" to "*", "7" to "7", "8" to "8", "9" to "9", "√" to "\\sqrt[", "aⁿ" to "^{", "log" to "log_{", "e" to "e",
        "\\" to "\\frac{", "." to ".", "0" to "0", "=" to "=", "⏎" to "ENTER", "(" to "(", ")" to ")", "π" to "\\pi"
    )

    // Группы символов для проверок
    private val symRoundBracket = listOf("\\sqrt(", "(", "log_{")
    private val symCurlyBracket = listOf("\\sqrt{", "^{", "\\frac{")
    private val symClosing = listOf(")", "}", "}{", "}(", "]", "]{")
    private val symOpening = listOf(
        "sin", "cos", "\\sqrt(", "(", "log_{", "\\sqrt{", "}{", "}(", "^{", "\\frac{",
        "tg", "ctg", "\\arcsin", "\\arccos", "\\arctan", "\\arccot", ""
    )
    private val symNumbers = listOf("0","1","2","3","4","5","6","7","8","9","y","a","b","c","x","^\\circ","\\cup")
    private val symArithmeticWoMinus = listOf("+","*","^{")
    private val symSigns = listOf("+","-","*","^\\circ","\\cup")
    private val symRootBracket = listOf("\\sqrt[")
    private val symTrigFunctions = listOf("\\sin","\\cos","\\tan","\\cot","\\arcsin","\\arccos","\\arctan","\\arccot")

    /** Привязываем Kotlin-WebView и настраиваем JavaScript */
    fun setWebView(view: WebView) {
        webView = view.apply {
            settings.javaScriptEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
        }
    }
    /** Полная очистка ввода (аналог CE) */
    fun resetText() {
        left.clear()
        right.clear()
        updateMathView()
    }

    /** Удаление символа слева от курсора */
    fun delete() {
        if (left.isEmpty()) return

        val lastItem = left.removeAt(left.size - 1)
        val lastChar = lastItem.lastOrNull() ?: return

        // Если это закрывающая скобка — перемещаем в right
        when {
            lastItem == "}{" || lastItem == "}(" || lastChar == '}' || lastChar == ')' -> {
                right.add(lastItem)
            }
            // Если удаляем \\frac{ или log_{ — убираем из right два элемента
            lastItem == "\\frac{" || lastItem == "log_{" -> {
                if (right.size >= 2) {
                    right.removeAt(right.size - 1)
                    right.removeAt(right.size - 1)
                }
            }
            // Если удаляем открывающую скобку — убираем один элемент из right
            lastChar == '{' || lastChar == '(' -> {
                if (right.isNotEmpty()) {
                    right.removeAt(right.size - 1)
                }
            }
            else -> {
                // просто удалился одиночный символ, ничего дополнительного не делаем
            }
        }

        updateMathView()
    }

    /** Добавление символа (по синему словарю symbolMap) */
    fun addSymbol(symbolKey: String) {
        val symbol = symbolMap[symbolKey] ?: return

        // Проверка ограничений
        if (isInsertionViolations(symbol)) {
            return
        }

        // >= и <=
        if (left.lastOrNull() == ">" && symbol == "=") {
            left[left.size - 1] = "\\geq "
        } else if (left.lastOrNull() == "<" && symbol == "=") {
            left[left.size - 1] = "\\leq "
        }
        // Закрывающая скобка из right
        else if (symbol == "}") {
            if (right.isNotEmpty()) {
                left.add(right.removeAt(right.size - 1))
            }
        } else {
            left.add(symbol)
        }

        handleBrackets(symbol)
        handleBracketsS(symbol)
        updateMathView()
    }

    /** Перенести курсор вправо (как на Swift) */
    private fun moveCursorRight() {
        if (right.isNotEmpty()) {
            val next = right.removeAt(right.size - 1)
            left.add(next)
            updateMathView()
        }
    }

    /** Формирует конечный LaTeX-строку и обновляет WebView */
    private fun updateMathView() {
        val latex = getLatexText()
        onLatexUpdate?.invoke(latex)

        // Вставляем LaTeX в WebView (предполагаем, что там загружён MathJax)
        webView?.evaluateJavascript(
            "javascript:renderMath('$latex')",
            null
        )
    }

    /** Собирает строку LaTeX (левые + перевёрнутые правые) */
    private fun getLatexText(): String {
        return left.joinToString(separator = "") +
                "\\lceil" +
                right.asReversed().joinToString(separator = "")
    }

    /** Логика расстановки скобок и вложений (похожая на Swift) */
    private fun handleBrackets(symbol: String) {
        // Круглые скобки и логарифм
        if (symRoundBracket.contains(symbol)) {
            right.add(")")
            if (symbol == "log_{") {
                right.add("}(")
            }
        }
        // Фигурные скобки и дробь
        else if (symCurlyBracket.contains(symbol)) {
            right.add("}")
            if (symbol == "\\frac{") {
                right.add("}{")
            }
        }
        // Ручное закрытие скобок
        else if (symbol == ")" || symbol == "}") {
            if (right.isNotEmpty()) {
                right.removeAt(right.size - 1)
            }
        }
        // После тригонометрических функций сразу открываем "("
        if (symTrigFunctions.contains(symbol)) {
            left.add("(")
            right.add(")")
        }
    }

    /** Обработка []-скобок для корня с основанием (√[ ]) */
    private fun handleBracketsS(symbol: String) {
        if (symRootBracket.contains(symbol)) {
            right.add("}")
            if (symbol == "\\sqrt[") {
                right.add("]{")
            }
        }
    }

    /** Проверяет, можно ли вставить symbol, или это нарушение синтаксиса */
    private fun isInsertionViolations(symbol: String): Boolean {
        val lastSymbol = left.lastOrNull() ?: ""
        val nextSymbol = right.lastOrNull() ?: ""

        // Примеры из Swift-версии:
        if (symOpening.contains(lastSymbol)
            && symArithmeticWoMinus.contains(symbol)
            && !symTrigFunctions.contains(lastSymbol)) {
            return true
        }

        if (!symNumbers.contains(lastSymbol) && symbol == ".") {
            return true
        }

        if (lastSymbol == "." && !symNumbers.contains(symbol)) {
            return true
        }

        if (symSigns.contains(lastSymbol) && symSigns.contains(symbol)) {
            return true
        }

        if (symSigns.contains(lastSymbol) && symbol == "^{") {
            return true
        }

        if (lastSymbol == "}" && symbol == "^{") {
            return true
        }

        if (symOpening.contains(lastSymbol) && symClosing.contains(symbol) && !symTrigFunctions.contains(lastSymbol)) {
            return true
        }

        if (symbol == ")" && nextSymbol != ")") {
            return true
        }

        if (symbol == "}" && symClosing.contains(nextSymbol).not()) {
            return true
        }

        if (symTrigFunctions.contains(lastSymbol) && symbol == "^{") {
            // разрешаем возведение в степень после тригонометрической функции
            return false
        }

        if (lastSymbol == "\\pi" && symNumbers.contains(symbol)) {
            left.add("*")
            return false
        }

        return false
    }

    /** Обработка нажатий кнопок: CE, стереть, ENTER, остальные символы */
    fun handleButtonPress(id: String, value: String) {
        when (id) {
            "ster" -> delete()
            "bt_check", "CE" -> resetText()
            "bt_down", "⏎" -> moveCursorRight()
            else -> addSymbol(value)
        }
    }

    /** Полностью очистить ввод и WebView (без сбора скобок) */
    fun clearAllInput() {
        left.clear()
        right.clear()
        updateMathView()
    }
    fun getLatexPreview(): String {
        return getLatexText().replace("\\lceil", "\\lceil") // убираем маркер курсора
    }
}
