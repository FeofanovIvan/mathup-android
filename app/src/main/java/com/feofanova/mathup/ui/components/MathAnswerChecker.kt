package com.feofanova.mathup.ui.components

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@RequiresApi(Build.VERSION_CODES.ECLAIR_MR1)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MathAnswerChecker(
    userLatex: String,
    correctLatex: String,
    modifier: Modifier = Modifier,
    onResult: (Boolean) -> Unit
) {
    val isResultHandled = remember { mutableStateOf(false) }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    @RequiresApi(Build.VERSION_CODES.KITKAT)
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        view.evaluateJavascript("processComparison()", null)
                    }
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun postMessage(msg: String) {
                        if ((msg.contains("true") || msg.contains("false")) && !isResultHandled.value) {
                            onResult(msg.contains("true"))
                            isResultHandled.value = true
                        }
                    }
                }, "logger")

                val html = wrapCheckerHtml(userLatex, correctLatex)

                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        modifier = modifier
    )

}
fun wrapCheckerHtml(userLatex: String, correctLatex: String): String {
    val escapedUser = userLatex
        .replace("\\", "\\\\")
        .replace("\n", "")
    val escapedCorrect = correctLatex
        .replace("\\", "\\\\")
        .replace("\n", "")

    return """<!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <script src="https://cdnjs.cloudflare.com/ajax/libs/mathjs/9.4.4/math.min.js"></script>
            <script>
                
                const userLatex = "$escapedUser";
                const correctLatex = "$escapedCorrect";

                function latexToMathjs(latex) {
                 
                 let mathjsExpr = removeCursor(latex);
                 mathjsExpr = replaceFrac(mathjsExpr);
                 mathjsExpr = replaceSqrtAll(mathjsExpr);
                 
                 mathjsExpr = mathjsExpr.replace(/\left/g, '');
                 mathjsExpr = mathjsExpr.replace(/\\right/g, '');
                 mathjsExpr = mathjsExpr.replace(/\\geq/g, '>=');
                 mathjsExpr = mathjsExpr.replace(/\\leq/g, '<=');
                 mathjsExpr = mathjsExpr.replace(/\\le/g, '<=');
                 mathjsExpr = mathjsExpr.replace(/\\ge/g, '>=');
                 
                 mathjsExpr = mathjsExpr.replace(/\sqrt\[\]\{([^}]+)\}/g, 'sqrt(${'$'}1)');
                 
                 
                 mathjsExpr = mathjsExpr.replace(/\\sqrt\[\s*([^\]]+)\s*\]\{([^}]+)\}/g, 'nthRoot(${'$'}2, ${'$'}1)');
                 mathjsExpr = replaceLogBase(mathjsExpr);
                 mathjsExpr = mathjsExpr.replace(/\\log_\\\{([^}]+)\\\}\{([^}]+)\\\}/g, 'log(${'$'}2, ${'$'}1)');
                 mathjsExpr = mathjsExpr.replace(/\\log_\{(\d+)\}\s*\\left\(([^)]+)\s*\\right\)/g, '(log(${'$'}2) / log(${'$'}1))');
                 mathjsExpr = mathjsExpr.replace(/log_\{(\d+)\}\(([^)]+)\)/g, '(log(${'$'}2) / log(${'$'}1))');
                 

                 
                 mathjsExpr = mathjsExpr.replace(/(sin|cos|tan|cot|arcsin|arccos|arctan|arccot)\^\{([^}]+)\}(\d+)\^\\circ/g, '${'$'}1(${'$'}3 * pi / 180)^(${'$'}2)');
                 mathjsExpr = mathjsExpr.replace(/(sin|cos|tan|cot|arcsin|arccos|arctan|arccot)(\d+)\^\\circ/g, '${'$'}1(${'$'}2 * pi / 180)');
                 mathjsExpr = mathjsExpr.replace(/(sin|cos|tan|cot|arcsin|arccos|arctan|arccot)\^\{([^}]+)\}(\d+)/g, '${'$'}1(${'$'}3)^(${'$'}2)');
                 mathjsExpr = mathjsExpr.replace(/(sin|cos|tan|cot|arcsin|arccos|arctan|arccot)(\d+)/g, '${'$'}1(${'$'}2)');
                 
                 mathjsExpr = mathjsExpr.replace(/(sin|cos|tan|cot|arcsin|arccos|arctan|arccot)\^\{(\d+)\}\((\w)\)/g, '${'$'}1(${'$'}3)^(${'$'}2)');
                 mathjsExpr = mathjsExpr.replace(/(sin|cos|tan|cot|arcsin|arccos|arctan|arccot)\^\{(\d+)\}(\w)/g, '${'$'}1(${'$'}3)^(${'$'}2)');
                 
                 mathjsExpr = mathjsExpr.replace(/\\cdot/g, '*');
                 
                 
                 mathjsExpr = mathjsExpr.replace(/(\d+)\\circ/g, '(${'$'}1 * pi / 180)');
                 mathjsExpr = mathjsExpr.replace(/\\tan\(([^)]+)\)/g, 'tan(${'$'}1)');
                 mathjsExpr = mathjsExpr.replace(/\\cot\(([^)]+)\)/g, 'cot(${'$'}1)');
                 mathjsExpr = mathjsExpr.replace(/\\arcsin\(([^)]+)\)/g, 'asin(${'$'}1)');
                 mathjsExpr = mathjsExpr.replace(/\\arccos\(([^)]+)\)/g, 'acos(${'$'}1)');
                 mathjsExpr = mathjsExpr.replace(/\\arctan\(([^)]+)\)/g, 'atan(${'$'}1)');
                 mathjsExpr = mathjsExpr.replace(/\\arccot\(([^)]+)\)/g, 'acot(${'$'}1)');
                 
                 mathjsExpr = mathjsExpr.replace(/(\d+)\^\{([^}]+)\}/g, '${'$'}1^(${'$'}2)');

                 
                 mathjsExpr = mathjsExpr.replace(/\\ln\(([^)]+)\)/g, 'log(${'$'}1)');
                 
                 mathjsExpr = mathjsExpr.replace(/\\sin\(([^)]+)\)/g, 'sin(${'$'}1)');
                 mathjsExpr = mathjsExpr.replace(/\\cos\(([^)]+)\)/g, 'cos(${'$'}1)');
                 mathjsExpr = mathjsExpr.replace(/\\tan\(([^)]+)\)/g, 'tan(${'$'}1)');
                 mathjsExpr = mathjsExpr.replace(/\\pi/g, 'pi');
                 mathjsExpr = mathjsExpr.replace(/\\e/g, 'e');
                 mathjsExpr = mathjsExpr.replace(/\\left/g, '');
                 mathjsExpr = mathjsExpr.replace(/\\right/g, '');
                 mathjsExpr = mathjsExpr.replace(/\\\{/g, '(');
                 mathjsExpr = mathjsExpr.replace(/\\\}/g, ')');
                 mathjsExpr = mathjsExpr.replace(/\^\{([^}]+)\}/g, '^(${'$'}1)');
                 
                 mathjsExpr = mathjsExpr.replace(/times/g, '*');
                 mathjsExpr = mathjsExpr.replace(/(\d)([xyabc])/g, '${'$'}1*${'$'}2');
                 
                 mathjsExpr = mathjsExpr.replace(/\{/g, '(');
                 mathjsExpr = mathjsExpr.replace(/\}/g, ')');
                 mathjsExpr = mathjsExpr.replace(/\\/g, '');
                 
                 mathjsExpr = mathjsExpr.replace(/\bcup\b/g, 'or');
                 
                 mathjsExpr = mathjsExpr.replace(/(\d+)\^circ/g, '(${'$'}1 * pi / 180)');
                 return mathjsExpr;
                 }
                 function replaceLogBase(expr) {
                     expr = expr.replace(/log_\{(\d+)\}\{([^}]+)\}/g, 'log(${'$'}2, ${'$'}1)');
                     expr = replaceLogWithBaseInParens(expr);
                     expr = expr.replace(/log_{}\(([^)]+)\)/g, 'log(${'$'}1)');
                     expr = expr.replace(/log_\(\)\(([^)]+)\)/g, 'log(${'$'}1)');
                     return expr;
                 }
                 function replaceLogWithBaseInParens(expr) {
                     let result = '';
                     let i = 0;

                     while (i < expr.length) {
                         if (expr.slice(i).startsWith('log_')) {
                             const baseMatch = expr.slice(i).match(/^log_\{(\d+)\}\(/);
                             if (baseMatch) {
                                 const base = baseMatch[1];
                                 const logPrefixLength = baseMatch[0].length;

                                 const openParenIndex = i + logPrefixLength;

                                 const extract = extractBalanced(expr, openParenIndex - 1, '(', ')');
                                 const content = extract.content;
                                 const endIndex = extract.endIndex;

                                 result += "(log(" + content + ") / log(" + base + "))";
                                 i = endIndex + 1;
                                 continue;
                             }
                         }

                         result += expr[i];
                         i++;
                     }
                     return result;
                 }

                 
 
                 function extractBalanced(str, startIndex, openChar, closeChar) {
                     let depth = 0;
                     let content = '';
                     for (let i = startIndex; i < str.length; i++) {
                         const ch = str[i];
                         if (ch === openChar) depth++;
                         else if (ch === closeChar) depth--;

                         content += ch;

                         if (depth === 0) {
                             return {
                                 content: content.slice(1, -1),
                                 endIndex: i
                             };
                         }
                     }
                     throw new Error("Unbalanced " + openChar + closeChar + " in expression");
                 }
                 function replaceFrac(s) {
                 
                 const tag = '\\frac';
                 const i = s.indexOf(tag);
                 if (i === -1) return s;
                 const numStart = s.indexOf('{', i + tag.length);
                 const numEnd   = findMatchingBrace(s, numStart);
                 const num      = s.slice(numStart + 1, numEnd);
                 
                 const denStart = s.indexOf('{', numEnd + 1);
                 const denEnd   = findMatchingBrace(s, denStart);
                 const den      = s.slice(denStart + 1, denEnd);
                 
                 const replaced = '(' + replaceFrac(num) + ')/(' + replaceFrac(den) + ')';
                 const before   = s.slice(0, i);
                 const after    = s.slice(denEnd + 1);
                 
                 return replaceFrac(before + replaced + after);
                 }
                 function replaceSqrtIndexed(s) {
                 const tag = '\sqrt[';  
                 let i = s.indexOf(tag);
                 while (i !== -1) {
                 const idxStart = i + tag.length;
                 const idxEnd   = s.indexOf(']', idxStart);
                 if (idxEnd < 0) break;
                 
                 const index = s.slice(idxStart, idxEnd);
                 if (index === '') {
                 i = s.indexOf(tag, idxEnd + 1);
                 continue;
                 }
                 const braceStart = s.indexOf('{', idxEnd);
                 if (braceStart < 0) break;
                 const braceEnd = findMatchingBrace(s, braceStart);
                 if (braceEnd < 0) break;

                 const content = s.slice(braceStart + 1, braceEnd);
                 const newIndex   = replaceSqrtAll(index);
                 const newContent = replaceSqrtAll(content);

                 const replaced = 'nthRoot(' + newContent + ', ' + newIndex + ')';
                 s = s.slice(0, i) + replaced + s.slice(braceEnd + 1);
                 i = s.indexOf(tag, i + replaced.length);
                 }
                 return s;
                 }
                 function replaceSqrtEmpty(s) {
                     const tag = '\sqrt[]';
                     let i = s.indexOf(tag);
                     while (i !== -1) {
                         const braceStart = s.indexOf('{', i + tag.length);
                         if (braceStart < 0) break;
                         const braceEnd = findMatchingBrace(s, braceStart);
                         if (braceEnd < 0) break;

                         const content = s.slice(braceStart + 1, braceEnd);
                         const newContent = replaceSqrtAll(content);

                         const replaced = 'sqrt(' + newContent + ')';
                         s = s.slice(0, i) + replaced + s.slice(braceEnd + 1);
                         i = s.indexOf(tag, i + replaced.length);
                     }
                     return s;
                 }
                 function replaceSqrtAll(s) {
                 return replaceSqrtEmpty(replaceSqrtIndexed(s));
                 }
                 function findMatchingBrace(str, pos) {
                 let depth = 0;
                 for (let j = pos; j < str.length; j++) {
                 if (str[j] === '{') depth++;
                 else if (str[j] === '}') {
                 depth--;
                 if (depth === 0) return j;
                 }
                 }
                 return -1;
                 }
                 
                 function removeCursor(latex) {
                 return latex.replace(/\\lceil/g, '');
                 return latex.replace(/\lceil/g, '');
                 }
                 
                 function compareMathInputWithData(userLatex, correctLatex) {
                             const userInput = latexToMathjs(userLatex).replace(/\s+/g, '');
                             const actionData = latexToMathjs(correctLatex).replace(/\s+/g, '').replace(/times/g, '*');
                              
                             const inequalitySigns = ['<=','>=','<', '>', '='];
                 
                             if (hasVariables(userInput) || hasVariables(actionData)) {
                 const simplifiedUserInput = replaceVariablesWithOne(userInput);
                 const simplifiedActionData = replaceVariablesWithOne(actionData);
                 
                 try {
                 const userSegments = simplifiedUserInput.includes("or") ? simplifiedUserInput.split("or").map(s => stripOuterParens(s)) : [simplifiedUserInput];
                 const actionSegments = simplifiedActionData.includes("or") ? simplifiedActionData.split("or").map(s => stripOuterParens(s)) : [simplifiedActionData];
                 
                 if (userSegments.length !== actionSegments.length) {
                 return false;
                 }
                 for (let i = 0; i < userSegments.length; i++) {
                 const userSegment = userSegments[i];
                 const actionSegment = actionSegments[i];
                 const userSign = inequalitySigns.find(sign => userSegment.includes(sign));
                 const actionSign = inequalitySigns.find(sign => actionSegment.includes(sign));
                 
                 if (!userSign || !actionSign || userSign !== actionSign) {

                         try {
                             const valUser = roundToDecimalPlace(evaluateExpression(userSegment), 3);
                             const valAction = roundToDecimalPlace(evaluateExpression(actionSegment), 3);

                             const isEqual = valUser === valAction;


                             if (!isEqual) return false;
                             else continue; // переходим к следующему сегменту
                         } catch (innerErr) {
                             return false;
                         }
                     }
                 
                 const userParts = splitInequality(userSegment);
                 const actionParts = splitInequality(actionSegment);

                 const userSigns = extractInequalityOperators(userSegment);
                 const actionSigns = extractInequalityOperators(actionSegment);

                 if (userParts.length !== actionParts.length) {
                     return false;
                 }

                 if (userSigns.length !== actionSigns.length) {
                     return false;
                 }
                 const resultsUser = userParts.map(p => roundToDecimalPlace(evaluateExpression(p), 3));
                 const resultsAction = actionParts.map(p => roundToDecimalPlace(evaluateExpression(p), 3));
                 
                 for (let i = 0; i < resultsUser.length; i++) {
                     const userVal = resultsUser[i];
                     const correctVal = resultsAction[i];

                     const match =
                             (userVal === correctVal) ||
                             (Number.isNaN(userVal) && Number.isNaN(correctVal)) ||
                             (Math.abs(userVal) === Infinity && Math.abs(correctVal) === Infinity);



                     if (!match) {
                         return false;
                     }

                     if (userSigns[i] !== actionSigns[i]) {

                         return false;
                     }
                 }

                 }
                 return true;
                 
                 } catch (e) {
                 return false;
                 }
                 }
                            try {
                                const userInequality = inequalitySigns.find(sign => userInput.includes(sign));
                                const actionInequality = inequalitySigns.find(sign => actionData.includes(sign));
                                if (!userInput.includes('=') && actionData.includes('=')) {
                                    const [lhs, rhs] = actionData.split('=');
                                    const lhsVal = roundToDecimalPlace(evaluateExpression(lhs), 3);
                                    const rhsVal = roundToDecimalPlace(evaluateExpression(rhs), 3);
                                    const userVal = roundToDecimalPlace(evaluateExpression(userInput), 3);

                                    const isValid = lhsVal === rhsVal && userVal === lhsVal;


                                    return isValid;
                                }
                                if (!actionInequality && userInput.includes('=') && !actionData.includes('=')) {
                                    const [lhs, rhs] = userInput.split('=');

                                    const lhsVal = roundToDecimalPlace(evaluateExpression(lhs), 3);
                                    const rhsVal = roundToDecimalPlace(evaluateExpression(rhs), 3);
                                    const correctVal = roundToDecimalPlace(evaluateExpression(actionData), 3);

                                    const isUserCorrect = lhsVal === rhsVal && lhsVal === correctVal;


                                    return isUserCorrect;
                                }
                                if (userInequality || actionInequality) {
                                    const userParts = splitInequality(userInput);
                                    const actionParts = splitInequality(actionData);

                                    const userSigns = extractInequalityOperators(userInput);
                                    const actionSigns = extractInequalityOperators(actionData);

                                    const resultsUser = userParts.map(part => roundToDecimalPlace(evaluateExpression(part), 3));
                                    const resultsAction = actionParts.map(part => roundToDecimalPlace(evaluateExpression(part), 3));

                                    if (resultsUser.length !== resultsAction.length || userSigns.length !== actionSigns.length) {
                                        return false;
                                    }

                                    for (let i = 0; i < resultsUser.length; i++) {
                                        const valUser = resultsUser[i];
                                        const valCorrect = resultsAction[i];
                                        const match = valUser === valCorrect;


                                        if (!match) return false;
                                    }

                                    for (let j = 0; j < userSigns.length; j++) {
                                        if (userSigns[j] !== actionSigns[j]) {

                                            return false;
                                        }
                                    }

                                    return true;
                                }
                                const [userInputLeft, userInputRight] = userInput.split('=');
                                const [actionDataLeft, actionDataRight] = actionData.split('=');

                                const leftValUser = evaluateExpression(userInputLeft);
                                const rightValUser = userInputRight ? evaluateExpression(userInputRight) : null;

                                const leftValCorrect = evaluateExpression(actionDataLeft);
                                const rightValCorrect = actionDataRight ? evaluateExpression(actionDataRight) : null;

                                const leftMatch = roundToDecimalPlace(leftValUser, 3) === roundToDecimalPlace(leftValCorrect, 3);

                                let rightMatch = true;
                                if (userInputRight && actionDataRight) {
                                    rightMatch = roundToDecimalPlace(rightValUser, 3) === roundToDecimalPlace(rightValCorrect, 3);
                                }

                                return leftMatch && rightMatch;

                            } catch (error) {
                                return false;
                            }

                         }
                 function extractInequalityOperators(input) {
                     const regex = /(<=|>=|=|<|>)/g;
                     return input.match(regex) || [];
                 }

                 
                 function stripOuterParens(expr) {
                 if (expr.startsWith('(') && expr.endsWith(')')) {
                 return expr.substring(1, expr.length - 1);
                 }
                 return expr;
                 }
                 function replaceVariablesWithOne(expr) {
                 expr = expr.replace(/([xyabc])(?=pi\b)/g, '${'$'}1*');  // xpi → x*pi
                 expr = expr.replace(/([xyabc])(?=e\b)/g, '${'$'}1*');   // xe → x*e
                 expr = expr.replace(/(\d)([xyabc])/g, '${'$'}1*${'$'}2');
                 expr = expr.replace(/([xyabc])([xyabc])/g, '${'$'}1*${'$'}2');
                 expr = expr.replace(/([xyabc])(?=(sin|cos|tan|cot|arcsin|arccos|arctan|arccot)\b)/g, '${'$'}1*');
                 expr = expr.replace(/([xyabc])(?=\d)/g, '${'$'}1*');
                 let replacedExpr = expr
                         .replace(/(?<=\d)([xyabc])(?=[xyabc])/g, '(1)')              // цифра–буква–буква
                         .replace(/(?<=[xyabc])([xyabc])(?=[xyabc])/g, '(1)')         // буква–буква–буква
                         .replace( /(?<=[xyabc])([xyabc])(?=(?:${'$'}|[^A-Za-z0-9_]))/g,'(1)')  // буква–(конец строки или не-буква)
                         .replace(/\b([xyabc])\b/g, '(1)')                            // одиночный токен
                         .replace(/(?<=[nsit])([xyabc])(?![A-Za-z0-9_])/g,'(1)')
                 replacedExpr = replacedExpr.replace(/(\d)(sin|cos|tan|cot|arcsin|arccos|arctan|arccot)/g, '${'$'}1*${'$'}2');
                 replacedExpr = replacedExpr.replace(/(sin|cos|tan|cot|arcsin|arccos|arctan|arccot)\^\{(\d+)\}\((\w)\)/g, '${'$'}1(${'$'}3)^(${'$'}2)');
                 replacedExpr = replacedExpr.replace(/(sin|cos|tan|cot|arcsin|arccos|arctan|arccot)\^\{(\d+)\}(\w)/g, '${'$'}1(${'$'}3)^(${'$'}2)');
                 replacedExpr = replacedExpr.replace(/log_\{(\d+)\}\(([^)]+)\)/g, '(log(${'$'}2) / log(${'$'}1))');
                 replacedExpr = replacedExpr.replace(/(\d)\s*\((log\([^)]+\)\s*\/\s*log\([^)]+\))\)/g, '${'$'}1*${'$'}2');
                 
                 return replacedExpr;
                 }
                 function roundToDecimalPlace(value, decimalPlaces) {
                 const factor = Math.pow(10, decimalPlaces);
                 return Math.round(value * factor) / factor;
                 }
                 
                 function hasVariables(expr) {
                 return /[xyabc]/.test(expr);
                 }
                 
                 
                 
                 function evaluateExpression(expr) {
                 try {
                     const node = math.parse(expr);
                     const code = node.compile();
                     return code.evaluate();
                 } catch (error) {
                     throw error;
                 }
                 }
                 
                 function splitInequality(input) {
                     const operators = ['<=', '>=', '=', '<', '>'];
                     const allOperators = operators.join('|');
                     const regex = new RegExp('(' + allOperators + ')');


                     const tokens = input.split(regex).map(s => s.trim()).filter(Boolean);

                     const parts = [];

                     for (let i = 0; i < tokens.length; i += 2) {
                         let part = tokens[i];
                         const originalPart = part;
                         if (part.startsWith('(') && (part.match(/\(/g)?.length || 0) > (part.match(/\)/g)?.length || 0)) {
                             part = part.slice(1);
                         }
                         if (part.endsWith(')') && (part.match(/\)/g)?.length || 0) > (part.match(/\(/g)?.length || 0)) {
                             part = part.slice(0, -1);
                         }

                         parts.push(part);

                     }

                     return parts;
                 }
                 function processComparison() {
                     const result = compareMathInputWithData(userLatex, correctLatex);
                     window.logger.postMessage(result.toString());
                 }

                
            </script>
        </head>
        <body></body>
        </html>

    """.trimIndent()
}
