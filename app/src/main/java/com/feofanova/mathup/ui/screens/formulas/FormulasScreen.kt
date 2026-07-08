package com.feofanova.mathup.ui.screens.formulas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.feofanova.mathup.data.local.db.MathUpDatabase
import com.feofanova.mathup.data.local.entities.FormulaEntity
import com.feofanova.mathup.data.stats.StatsDatabase
import com.feofanova.mathup.data.stats.entities.FormulaResultEntity
import android.webkit.WebView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.feofanova.mathup.ui.screens.main.SetStatusBarColor
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import android.content.Context
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.feofanova.mathup.data.local.db.MathUpOgeDatabase
import com.feofanova.mathup.sound.LocalSoundPlayer
import kotlinx.coroutines.delay


@Composable
fun FormulasScreen(profile: String, onBack: () -> Unit) {
    SetStatusBarColor(color = Color(0xFF1F2E59), darkIcons = false)
    val context = LocalContext.current
    val dao = remember {
        if (profile == "ОГЭ")
            MathUpOgeDatabase.getInstance(context).appDao()
        else
            MathUpDatabase.getInstance(context).appDao()
    }


    val statsDao = remember { StatsDatabase.getInstance(context).statsDao() }
    val scope = rememberCoroutineScope()

    var options by remember { mutableStateOf<List<FormulaEntity>>(emptyList()) }
    var correct by remember { mutableStateOf<FormulaEntity?>(null) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    var showResult by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    var showFeedback by remember { mutableStateOf(false) }
    var isCorrectAnswer by remember { mutableStateOf<Boolean?>(null) }
    var correctStreak by remember { mutableIntStateOf(0) }
    var rewardMessage by remember { mutableStateOf<String?>(null) }
    var rewardSubtext by remember { mutableStateOf<String?>(null) }

    // Подписываемся на процент из БД
    val percentFlow = statsDao.observeCorrectPercent()
    val percent by percentFlow.collectAsState(initial = 0f)

    fun loadNext() {
        scope.launch {
            val all = withContext(Dispatchers.IO) { dao.getAllFormulas() }
            if (all.size >= 4) {
                val correctFormula = all.random()
                val others = (all - correctFormula).shuffled().take(3)
                options = (others + correctFormula).shuffled()
                correct = correctFormula
            }
        }
    }

    LaunchedEffect(Unit) {
        loadNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFF2F9FF), Color.White)
                )
            )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                FormulasTopBar(percent = percent, onBack = onBack)
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(padding)
                    .padding(16.dp)
            ) {
                FormulaQuestionView(correct)

                Spacer(Modifier.height(16.dp))

                Text(
                    "Выберите правильную формулу:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                options.forEach { formula ->
                    key(formula.formulaID) {
                        FormulaOption(
                            formula = formula,
                            isSelected = selectedId == formula.formulaID,
                            isCorrect = formula.formulaID == correct?.formulaID,
                            showCheck = showResult,
                            onSelect = { if (!showResult) selectedId = it }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            showResult = true
                            val correctAnswer = selectedId == correct?.formulaID
                            if (selectedId != null) {
                                isCorrectAnswer = correctAnswer
                                showFeedback = true
                                if (correctAnswer) correctStreak++ else correctStreak = 0

                                // обновление streak-рекордов
                                rewardMessage = when (correctStreak) {
                                    5  -> "🎯 Отличное начало!"
                                    10 -> "🔥 Так держать!"
                                    20 -> "🌟 Ты супер!"
                                    35 -> "🚀 Невероятно!"
                                    50 -> "🧠 Ты гений!"
                                    else -> null
                                }
                                rewardSubtext = rewardMessage?.let {
                                    "$correctStreak правильных подряд"
                                }

                                // пишем результат в БД
                                scope.launch(Dispatchers.IO) {
                                    val dao = statsDao
                                    val id = selectedId!!
                                    val result = dao.getFormulaResultById(id)
                                    if (result == null) {
                                        dao.insertFormulaResult(
                                            FormulaResultEntity(
                                                formulaID   = id,
                                                correctCount= if (correctAnswer) 1 else 0,
                                                wrongCount  = if (!correctAnswer) 1 else 0
                                            )
                                        )
                                    } else {
                                        if (correctAnswer) dao.incrementCorrect(id)
                                        else               dao.incrementWrong(id)
                                    }
                                }

                                // Ждём 2 секунды, чтобы пользователь успел увидеть фидбек,
                                // а потом сбрасываем состояния и грузим следующий вопрос:
                                scope.launch {
                                    delay(1500)
                                    showFeedback = false
                                    showResult   = false
                                    selectedId   = null
                                    loadNext()
                                }
                            }
                        },
                        enabled = selectedId != null && !showResult,
                        colors = ButtonDefaults.buttonColors(
                            containerColor         = Color.Green,
                            disabledContainerColor = Color.LightGray,
                            contentColor           = Color.White
                        )
                    ) {
                        Text("Проверить")
                    }

                    Button(
                        onClick = {
                            selectedId = null
                            showResult = false
                            loadNext()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F2E59),
                            contentColor   = Color.White
                        )
                    ) {
                        Text("Следующий")
                    }
                }
            }

            if (showFeedback) {
                PlayFeedbackSound(isCorrectAnswer)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCorrectAnswer == true) Color(0xFF4CAF50) else Color(0xFFF44336),
                            shadowElevation = 8.dp
                        ) {
                            Text(
                                text      = if (isCorrectAnswer == true) "Правильно!" else "Неправильно",
                                color     = Color.White,
                                fontSize  = 22.sp,
                                fontWeight= FontWeight.Bold,
                                modifier  = Modifier.padding(32.dp, 20.dp)
                            )
                        }

                        rewardMessage?.let {
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1F2E59),
                                tonalElevation = 4.dp
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp, 16.dp)
                                ) {
                                    Text(it, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(rewardSubtext ?: "", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayFeedbackSound(isCorrect: Boolean?) {
    val player = LocalSoundPlayer.current

    LaunchedEffect(isCorrect) {
        when (isCorrect) {
            true -> player.playCorrect()
            false -> player.playWrong()
            null -> {} // ничего не делаем
        }
    }
}

@Composable
fun FormulaOption(
    formula: FormulaEntity,
    isSelected: Boolean,
    isCorrect: Boolean,
    showCheck: Boolean,
    onSelect: (Int) -> Unit
) {
    // Определяем цвет фона в зависимости от состояния
    val backgroundColor = when {
        showCheck && isCorrect  -> Color(0xFFA5D6A7) // мягкий зелёный
        showCheck && isSelected -> Color(0xFFEF9A9A)  // мягкий красный
        isSelected                   -> Color.LightGray   // жёлтый, если просто выделено
        else                         -> Color.White       // белый по умолчанию
    }

    val html = """
        <html>
          <head>
            <script src="https://polyfill.io/v3/polyfill.min.js?features=es6"></script>
            <script id="MathJax-script" async 
              src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js">
            </script>
            <style>body { margin: 0; font-size: 20px; text-align: center; }</style>
          </head>
          <body>\(${formula.formula}\)</body>
        </html>
    """.trimIndent()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clickable { onSelect(formula.formulaID) }
            .padding(12.dp)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    setBackgroundColor(0x00000000)
                    loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                }
            }
        )
    }
}


@Composable
fun FormulasTopBar(
    percent: Float,
    onBack: () -> Unit
) {
    val animatedPercent by animateFloatAsState(targetValue = percent, label = "Progress")
    val progressColor = when {
        animatedPercent >= 0.8f -> Color(0xFF4CAF50)
        animatedPercent >= 0.5f -> Color(0xFFFFC107)
        else                     -> Color(0xFFF44336)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F2E59))                // 1) сначала фон — он покроет и будущие паддинги
            .windowInsetsPadding(WindowInsets.statusBars) // 2) отодвигаем контент ниже статус-бара
            .padding(16.dp),                              // 3) твой внутренний отступ
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Формулы",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            Modifier
                .background(progressColor, RoundedCornerShape(8.dp))
                .padding(12.dp, 6.dp)
        ) {
            Text(
                "${(animatedPercent * 100).toInt()}%",
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp
            )
        }
    }
}








@Composable
fun FormulaQuestionView(formula: FormulaEntity?) {
    if (formula == null) return

    // HTML с MathJax и кастомным CSS
    val html = """
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            <script src="https://polyfill.io/v3/polyfill.min.js?features=es6"></script>
            <script id="MathJax-script" async
              src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js">
            </script>
            <style>
              body {
                margin: 0;
                padding: 0;
                text-align: center;
              }
              /* Увеличим размер формулы и сделаем её жирной */
              .math-container {
                font-size: 18px;       /* можно регулировать в px или em */
                font-weight: bold;
                line-height: 1;      /* чтобы не «обрезало» */
              }
            </style>
          </head>
          <body>
            <div class="math-container">
              ${formula.name}
            </div>
          </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { ctx: Context ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(0) // transparent
            }
        },
        update = { webView: WebView ->
            webView.loadDataWithBaseURL(
                null, html, "text/html", "utf-8", null
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
