package com.feofanova.mathup.testing

data class TestCase(
    val userLatex: String,
    val correctLatex: String,
    val expectedResult: Boolean
)


val testCases = listOf(
    TestCase(
        userLatex = "(\\frac{-1-\\sqrt[]{5}}{2}<x<\\frac{1}{3})\\cup (x>\\frac{\\sqrt[]{5}-1}{2})\\lceil",
        correctLatex = "((-1-sqrt(5))/2<x<1/3)or(x>((sqrt(5)-1)/2))",
        expectedResult = true
    ),
    // TestCase("1<log_{3}(5)<2\\lceil", "1<log(5,3)<2", true),
    // TestCase("2sin^{2}x+10sinx+2=sinx+7\\lceil", "2 * (sin(x))^2 + 10 * sin(x) + 2 = sin(x) + 7", true),
    // TestCase("2sinx^{2}+10sinx+2=sinx+7\\lceil", "2 * sin(x)^2 + 10 * sin(x) + 2 = sin(x) + 7", true),
    // TestCase("(x\\leq \\frac{1}{3})\\cup (x\\geq \\frac{1}{2})\\lceil", "(x<=1/3)or(x>=1/2)", true),
    // TestCase("720*0.5=30\\lceil", "720*0.5", true),
    // TestCase("\\frac{3 \\sqrt{7}}{2}", "(3 * sqrt(7)) / 2", true),
    // TestCase("0<x<\\frac{5}{6}\\lceil", "(0<x<5/6)", true),
    //  TestCase("a+b+c", "a+b+c", true),
    // TestCase("\\frac{\\pi}{6}+2a\\pi\\lceil", "pi/6+2*a*pi", true),
    // TestCase("(\\cos(\\frac{\\pi}{4})+2)*3b=2\\sin(\\frac{\\pi}{4})\\lceil", "(cos(pi/4)+2)*3*b=2*sin(pi/4)", true),
    // TestCase("log_{8}(\\frac{x+a}{x-a})=log_{8}(\\frac{2a}{x-a})\\lceil", "log((x+a)/(x-a),8)=log((2a)/(x-a),8)", true),

    // 🔁 Дополнительные вариации:
    TestCase(
        "(log_{5}(\\frac{\\sqrt[]{3}-1}{2})\\leq x<0)\\cup (0<x\\leq log_{5}(\\frac{\\sqrt[]{13}-1}{2}))\\lceil",
        "(log((sqrt(3)-1)/2, 5)<=x<0)or(0<x<=log((sqrt(13)-1)/2, 5))",
        true
    ),
)
