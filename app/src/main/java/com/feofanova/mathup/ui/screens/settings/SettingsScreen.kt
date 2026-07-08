package com.feofanova.mathup.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.feofanova.mathup.data.local.db.MathUpDatabase
import com.feofanova.mathup.data.repository.DataSyncManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.viewmodel.compose.viewModel
import com.feofanova.mathup.data.characters.GameDataSyncManager
import com.feofanova.mathup.data.characters.GameDatabase
import com.feofanova.mathup.data.local.db.MathUpOgeDatabase
import com.feofanova.mathup.data.stats.StatsDatabase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


@Composable
fun SettingsScreen(
    navController: NavHostController,
    isSoundEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    var expanded by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var isFeedbackExpanded by remember { mutableStateOf(false) }
    var isSoundNotificationsExpanded by remember { mutableStateOf(false) }
    var isNotificationsEnabled by remember { mutableStateOf(true) }
    var isFAQExpanded by remember { mutableStateOf(false) }
    var expandedQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var isTermsExpanded by remember { mutableStateOf(false) }
    var expandedTermIndex by remember { mutableStateOf<Int?>(null) }
    var isPrivacyExpanded by remember { mutableStateOf(false) }
    var expandedPrivacyIndex by remember { mutableStateOf<Int?>(null) }
    var isUpdatingContent by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val db = remember { StatsDatabase.getInstance(context) }
    val statsDao = remember { db.statsDao() }
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(statsDao))

    val currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    val currentEmail by remember { mutableStateOf(currentUser?.email ?: "Неизвестный пользователь") }

    var currentName by remember { mutableStateOf(user?.displayName ?: "Имя пользователя") }

    val coroutineScope = rememberCoroutineScope() // ← вот это обязательно

    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }




    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(Color(0xFFF2F9FF), Color.White))
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            // чтобы не появлялись лишние отступы под/над барами
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp), // поля от краёв
                snackbar = { data ->
                    Snackbar(
                        containerColor = Color.Green,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = data.visuals.message,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)              // теперь нулевой, но оставить можно
                .verticalScroll(rememberScrollState())
        ) {
            ProfileStatsCardUI(
                userName = currentName,
                userEmail = currentEmail,
                expanded = expanded,
                onExpandToggle = { expanded = !expanded },
                newName = newName,
                onNameChange = { newName = it },
                onApplyName = {
                    coroutineScope.launch {
                        try {
                            updateFirebaseName(newName) // 👈 просто вызвать suspend-функцию
                            FirebaseAuth.getInstance().currentUser?.reload()?.await()
                            currentName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Имя пользователя"
                            snackbarHostState.showSnackbar("Имя успешно обновлено")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Ошибка: ${e.localizedMessage}")
                        }
                    }
                },
                examsTaken = viewModel.examsTaken,
                taskTaken = viewModel.taskTaken,
                formulaAccuracy = viewModel.formulaAccuracy,
                bestScore = viewModel.bestScoreBase,
                bestScoreProfile = viewModel.bestScoreProfile
            )


            // ➕ Далее будет статистика и пункты меню — добавлю по запросу
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White // 👈 делает карту белой
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = Color(0xFF1F2E59),
                            modifier = Modifier.size(28.dp)
                        )

                        Text(
                            text = "Обратная связь",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2E59),
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .weight(1f)
                        )

                        IconButton(onClick = { isFeedbackExpanded = !isFeedbackExpanded }) {
                            Icon(
                                imageVector = if (isFeedbackExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF1F2E59)
                            )
                        }
                    }

                    if (isFeedbackExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FeedbackItem(label = "feofanovamathtutor.com", url = "https://feofanovamathtutor.com")
                        FeedbackItem(label = "Чат-бот поддержки", url = "https://t.me/your_support_bot")
                        FeedbackItem(label = "Оцените нас в Play Market", url = "https://play.google.com/store/apps/details?id=your.package.name")
                    }
                }
            }
            //  Card(
                //      modifier = Modifier
                //         .padding(horizontal = 16.dp, vertical = 8.dp)
            //          .fillMaxWidth(),
            //    shape = RoundedCornerShape(16.dp),
            //      elevation = CardDefaults.cardElevation(6.dp),
            //      colors = CardDefaults.cardColors(
            //           containerColor = Color.White // 👈 делает карту белой
            //      )
            //    ) {
            //       Row(
                //         modifier = Modifier
                //             .fillMaxWidth()
            //             .padding(16.dp),
            //          verticalAlignment = Alignment.CenterVertically
            //       ) {
            //       Icon(
            //          imageVector = Icons.Default.Star, // 👑 если хочешь корону — см. ниже
            //           contentDescription = null,
            //          tint = Color(0xFF1F2E59),
            //            modifier = Modifier.size(28.dp)
            //        )

            //      Text(
            //         text = "Управление подпиской",
            //         fontSize = 16.sp,
            //        fontWeight = FontWeight.Bold,
            //        color = Color(0xFF1F2E59),
                //         modifier = Modifier
                            //             .padding(start = 12.dp)
            //            .weight(1f)
            //        )
            //     }
            //    }
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White // 👈 делает карту белой
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications, // можно заменить на Icons.Default.Notifications
                            contentDescription = null,
                            tint = Color(0xFF1F2E59),
                            modifier = Modifier.size(28.dp)
                        )

                        Text(
                            text = "Звук и уведомления",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2E59),
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .weight(1f)
                        )

                        IconButton(onClick = { isSoundNotificationsExpanded = !isSoundNotificationsExpanded }) {
                            Icon(
                                imageVector = if (isSoundNotificationsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF1F2E59)
                            )
                        }
                    }

                    if (isSoundNotificationsExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp, // замените на Icons.Default.Notifications
                                contentDescription = null,
                                tint = Color.Blue,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Уведомления",
                                modifier = Modifier.padding(start = 12.dp).weight(1f),
                                fontSize = 14.sp,
                                color = Color.Blue
                            )
                            androidx.compose.material3.Switch(
                                checked = isNotificationsEnabled,
                                onCheckedChange = { isNotificationsEnabled = it }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive, // замените на Icons.Default.VolumeUp или аналог
                                contentDescription = null,
                                tint = Color.Blue,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Звук",
                                modifier = Modifier.padding(start = 12.dp).weight(1f),
                                fontSize = 14.sp,
                                color = Color.Blue
                            )
                            androidx.compose.material3.Switch(
                                checked = isSoundEnabled,
                                onCheckedChange = { onSoundToggle(it) }
                            )
                        }
                    }
                }
            }
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White // 👈 делает карту белой
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help, // Альтернатива questionmark.circle.fill
                            contentDescription = null,
                            tint = Color(0xFF1F2E59),
                            modifier = Modifier.size(28.dp)
                        )

                        Text(
                            text = "FAQs",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2E59),
                            modifier = Modifier.padding(start = 12.dp).weight(1f)
                        )

                        IconButton(onClick = { isFAQExpanded = !isFAQExpanded }) {
                            Icon(
                                imageVector = if (isFAQExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF1F2E59)
                            )
                        }
                    }

                    if (isFAQExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FAQBlock(expandedIndex = expandedQuestionIndex, onToggle = { index ->
                            expandedQuestionIndex = if (expandedQuestionIndex == index) null else index
                        })
                    }
                }
            }
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White // 👈 делает карту белой
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description, // аналог doc.text.fill
                            contentDescription = null,
                            tint = Color(0xFF1F2E59),
                            modifier = Modifier.size(28.dp)
                        )

                        Text(
                            text = "Условия использования",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2E59),
                            modifier = Modifier.padding(start = 12.dp).weight(1f)
                        )

                        IconButton(onClick = { isTermsExpanded = !isTermsExpanded }) {
                            Icon(
                                imageVector = if (isTermsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF1F2E59)
                            )
                        }
                    }

                    if (isTermsExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TermsOfUseBlock(
                            expandedIndex = expandedTermIndex,
                            onToggle = { index ->
                                expandedTermIndex = if (expandedTermIndex == index) null else index
                            }
                        )
                    }
                }
            }
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White // 👈 делает карту белой
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security, // Альтернатива hand.raised.fill
                            contentDescription = null,
                            tint = Color(0xFF1F2E59),
                            modifier = Modifier.size(28.dp)
                        )

                        Text(
                            text = "Политика конфиденциальности",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2E59),
                            modifier = Modifier.padding(start = 12.dp).weight(1f)
                        )

                        IconButton(onClick = { isPrivacyExpanded = !isPrivacyExpanded }) {
                            Icon(
                                imageVector = if (isPrivacyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF1F2E59)
                            )
                        }
                    }

                    if (isPrivacyExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        PrivacyPolicyBlock(
                            expandedIndex = expandedPrivacyIndex,
                            onToggle = { index ->
                                expandedPrivacyIndex = if (expandedPrivacyIndex == index) null else index
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clickable(
                        enabled = !isUpdatingContent,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // 🔹 отключает ripple
                    ) {
                        scope.launch {
                            isUpdatingContent = true
                            try {
                                val egeDb = MathUpDatabase.getInstance(context)
                                val ogeDb = MathUpOgeDatabase.getInstance(context) // 🔄 добавляем вторую БД
                                val gameDb = GameDatabase.getInstance(context)

                                DataSyncManager.syncFromRemote(context, egeDb)
                                DataSyncManager.syncOgeFromRemote(context, ogeDb)
                                GameDataSyncManager.syncGameData(context, gameDb)


                                snackbarHostState.showSnackbar("Контент обновлён")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Ошибка: ${e.localizedMessage}")
                            } finally {
                                isUpdatingContent = false
                            }

                        }
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        tint = Color(0xFF1F2E59),
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        text = if (isUpdatingContent) "Обновление..." else "Обновить контент",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2E59),
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    )
                    if (isUpdatingContent) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF1F2E59)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF1F2E59)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clickable {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White // 👈 делает карту белой
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = Color(0xFF1F2E59),
                        modifier = Modifier.size(28.dp)
                    )

                    Text(
                        text = "Выход",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2E59),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
        }
    }
}


@Composable
fun PrivacyPolicyBlock(
    expandedIndex: Int?,
    onToggle: (Int) -> Unit
) {
    val policies = listOf(
        "1. Общая информация" to "MathUp уважает вашу конфиденциальность и защищает личные данные, предоставленные вами при использовании приложения.",
        "2. Сбор данных" to "Мы собираем только необходимую информацию — имя, email, ответы на задания и статистику использования. Эти данные помогают улучшать наш сервис.",
        "3. Использование данных" to "Собранные данные используются для анализа прогресса, персонализации и предоставления актуального контента.",
        "4. Хранение и защита" to "Ваши данные хранятся в Firebase и локально на вашем устройстве. Мы применяем технические меры защиты информации.",
        "5. Раскрытие информации" to "Мы не передаём персональные данные третьим лицам, за исключением случаев, предусмотренных законом.",
        "6. Файлы cookie и аналитика" to "Мы можем использовать аналитику Firebase и технические cookie для улучшения производительности и UX, без отслеживания личности.",
        "7. Ваши права" to "Вы имеете право запросить удаление или изменение ваших данных, написав нам через обратную связь.",
        "8. Согласие" to "Используя приложение, вы соглашаетесь с настоящей Политикой конфиденциальности.",
        "9. Изменения политики" to "Мы можем обновлять политику. Актуальная версия всегда будет доступна в разделе настроек приложения."
    )

    Column {
        policies.forEachIndexed { index, (title, body) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(index) }
                    .padding(vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security, // можно заменить
                        contentDescription = null,
                        tint = Color(0xFF1F2E59),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1F2E59)
                    )
                }

                if (expandedIndex == index) {
                    Text(
                        text = body,
                        fontSize = 13.sp,
                        color = Color.Blue,
                        modifier = Modifier
                            .padding(start = 28.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TermsOfUseBlock(
    expandedIndex: Int?,
    onToggle: (Int) -> Unit
) {
    val terms = listOf(
        "1. Общие положения" to "MathApp — это обучающее приложение для изучения и практики математики. Используя его, вы соглашаетесь с настоящими Условиями.",
        "2. Регистрация и аккаунт" to "Для доступа к функциям необходимо создать аккаунт. Вы обязуетесь предоставлять достоверные данные и хранить конфиденциальность своего пароля.",
        "3. Использование контента" to "Контент предназначен только для личного некоммерческого использования. Копирование и распространение без согласия запрещено.",
        "4. Пользовательские данные" to "Ваши ответы и статистика сохраняются локально и в облаке, используются для улучшения опыта. Мы не передаём данные третьим лицам.",
        "5. Обновления и доступность" to "Мы можем обновлять контент и функции без уведомления, а также временно ограничивать доступ к отдельным возможностям.",
        "6. Ограничение ответственности" to "MathApp не гарантирует абсолютную точность решений и успешную сдачу экзаменов. Использование — на ваш страх и риск.",
        "7. Подписки и покупки" to "Некоторые функции доступны по подписке. Условия регулируются через App Store и могут включать автоматическое продление.",
        "8. Нарушения и блокировки" to "Мы можем ограничить доступ при нарушении условий, попытках взлома или злоупотребления возможностями приложения.",
        "9. Обратная связь" to "Вы можете отправлять идеи через раздел «Обратная связь». Мы благодарны за предложения, но не обязуемся их реализовывать.",
        "10. Изменения условий" to "Условия могут обновляться. Продолжение использования означает согласие с последней версией."
    )

    Column {
        terms.forEachIndexed { index, (title, body) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(index) }
                    .padding(vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF1F2E59),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1F2E59)
                    )
                }

                if (expandedIndex == index) {
                    Text(
                        text = body,
                        fontSize = 13.sp,
                        color = Color.Blue,
                        modifier = Modifier
                            .padding(start = 28.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FAQBlock(
    expandedIndex: Int?,
    onToggle: (Int) -> Unit
) {
    val faqs = listOf(
        "Как начать использовать приложение?" to "После регистрации выберите профиль обучения — \"База\" или \"Профиль\". Вы получите доступ к заданиям, формулами и экзаменам, соответствующим выбранному уровню.",
        "Что такое экзамен и как он работает?" to "Экзамен — это подборка случайных задач по вашему профилю. Вы проходите задания по очереди, и после каждого можете получить результат. В конце показывается статистика и ваш итоговый балл.",
        "Как пользоваться формульной клавиатурой?" to "Наша клавиатура поддерживает математический синтаксис. Вводите выражения, используя кнопки с формулами. Всё, что вы вводите, отображается в виде LaTeX.",
        "Что означают шаги решения?" to "Каждая задача может состоять из нескольких шагов. Это помогает лучше понять, как решаются сложные примеры поэтапно.",
        "Как рассчитывается результат экзамена?" to "Мы сравниваем введённое вами выражение с правильным ответом, используя проверку через Math.js. Учитываются точность и структура.",
        "Где посмотреть свою статистику?" to "В разделе 'Настройки' — верхняя карточка показывает задания, формулы, экзамены и активное время.",
        "Как включить или отключить звук и уведомления?" to "В разделе 'Звук и уведомления' можно выбрать, какие функции включить с помощью переключателей.",
        "Почему не отображаются формулы?" to "Убедитесь, что у вас есть интернет. Формулы отображаются через MathJax/KaTeX. Перезапустите приложение, если нужно.",
        "У меня остались вопросы, как связаться с поддержкой?" to "Откройте 'Обратную связь' в настройках. Напишите нам через чат-бота или на сайте feofanovamathtutor.com."
    )

    Column {
        faqs.forEachIndexed { index, (question, answer) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(index) }
                    .padding(vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = null,
                        tint = Color(0xFF1F2E59),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = question,
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1F2E59)
                    )
                }

                if (expandedIndex == index) {
                    Text(
                        text = answer,
                        fontSize = 13.sp,
                        color = Color.Blue,
                        modifier = Modifier
                            .padding(start = 28.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable

fun SettingsActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color(0xFF1F2E59),
    contentPaddingStart: Dp = 8.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                indication = null, // 🔹 убираем тёмную заливку при клике
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(start = contentPaddingStart, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}


@Composable
fun FeedbackItem(label: String, url: String) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color(0xFF1F2E59))
        Text(
            text = label,
            color = Color(0xFF1F2E59),
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp).weight(1f)
        )

    }
}
@Composable
fun ProfileStatsCardUI(
    userName: String,
    userEmail: String,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    newName: String,
    onNameChange: (String) -> Unit,
    examsTaken: Int,
    onApplyName: () -> Unit,
    taskTaken: Int,
    formulaAccuracy: Int,
    bestScore: Int,
    bestScoreProfile: Int
) {
    val scope = rememberCoroutineScope()
    var showNameInput by remember { mutableStateOf(false) }
    var showEmailInput by remember { mutableStateOf(false) }
    var showPasswordInput by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    var tempEmail by remember { mutableStateOf("") }
    var tempPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }


    fun showError(message: String) {
        alertMessage = message
        showAlert = true
    }

    fun showSuccess(message: String) {
        successMessage = message
        showAlert = true
    }

    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Color(0xFF1F2E59),
                    modifier = Modifier.size(48.dp).padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(userName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2E59))
                    Text(userEmail, fontSize = 14.sp, color = Color.Gray)
                }
                IconButton(onClick = onExpandToggle) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Раскрыть",
                        tint = Color(0xFF1F2E59)
                    )
                }
            }

            if (expanded) {
                Column {
                    SettingsActionItem(
                        icon = Icons.Default.Person,
                        label = "Сменить имя",
                        onClick = {
                            val user = FirebaseAuth.getInstance().currentUser
                            if (isGoogleLogin(user)) showError("Вы вошли через Google. Изменение имени недоступно.")
                            else showNameInput = !showNameInput
                        },
                        tint = Color(0xFF1F2E59), contentPaddingStart = 12.dp
                    )
                    if (showNameInput) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newName,
                            onValueChange = onNameChange,
                            placeholder = { Text("Новое имя") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        updateFirebaseName(newName)

                                        FirebaseAuth.getInstance().currentUser?.reload()?.await()
                                        val updatedUser = FirebaseAuth.getInstance().currentUser


                                        showSuccess("Имя успешно обновлено")
                                    } catch (e: Exception) {
                                        showError(e.localizedMessage ?: "Ошибка при обновлении имени")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) { Text("Сменить") }
                    }

                    SettingsActionItem(
                        icon = Icons.Default.Email,
                        label = "Сменить почту",
                        onClick = {
                            val user = FirebaseAuth.getInstance().currentUser
                            if (isGoogleLogin(user)) showError("Вы вошли через Google. Изменение почты недоступно.")
                            else showEmailInput = !showEmailInput
                        },
                        tint = Color(0xFF1F2E59), contentPaddingStart = 12.dp
                    )
                    if (showEmailInput) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempEmail,
                            onValueChange = { tempEmail = it },
                            placeholder = { Text("example@mail.com") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    updateFirebaseEmail(tempEmail,
                                        onSuccess = { showSuccess("Почта успешно обновлена") },
                                        onError = { showError(it) }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) { Text("Сменить") }
                    }

                    SettingsActionItem(
                        icon = Icons.Default.Lock,
                        label = "Сменить пароль",
                        onClick = {
                            val user = FirebaseAuth.getInstance().currentUser
                            if (isGoogleLogin(user)) showError("Вы вошли через Google. Изменение пароля недоступно.")
                            else showPasswordInput = !showPasswordInput
                        },
                        tint = Color(0xFF1F2E59), contentPaddingStart = 12.dp
                    )
                    if (showPasswordInput) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempPassword,
                            onValueChange = { tempPassword = it },
                            placeholder = { Text("Новый пароль") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = null)
                                }
                            }
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    updateFirebasePassword(tempPassword,
                                        onSuccess = { showSuccess("Пароль успешно обновлен") },
                                        onError = { showError(it) }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) { Text("Сменить") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            StatRow("Решено заданий", "$taskTaken")
            StatRow("Правильных формул", "$formulaAccuracy%")
            StatRow("Проведено экзаменов", "$examsTaken")

            Spacer(modifier = Modifier.height(12.dp))
            Text("Лучший результат База", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            LinearProgressIndicator(progress = { bestScore / 21f }, modifier = Modifier.fillMaxWidth())
            Text("Оценка: ${gradeText(bestScore)} 🎯 ${feedbackText(bestScore)}", fontSize = 12.sp)

            Spacer(modifier = Modifier.height(12.dp))
            Text("Лучший результат Профиль", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            LinearProgressIndicator(progress = { bestScoreProfile / 19f }, color = Color.Red, modifier = Modifier.fillMaxWidth())
            Text("Оценка: ${gradeText(bestScoreProfile)} 🎯 ${feedbackText(bestScoreProfile)}", fontSize = 12.sp)
        }
    }

    if (showAlert) {
        AlertDialog(
            onDismissRequest = { showAlert = false },
            title = { Text(if (successMessage.isNotBlank()) "Успешно" else "Ошибка") },
            text = { Text(alertMessage.ifBlank { successMessage }) },
            confirmButton = {
                Button(onClick = {
                    showAlert = false
                    successMessage = ""
                    alertMessage = ""
                }) {
                    Text("OK")
                }
            }
        )
    }
}

fun gradeText(score: Int): String = when (score) {
    in 1..<5 -> "2"
    in 5..<12 -> "3"
    in 12..<17 -> "4"
    in 17..21 -> "5"
    else -> "–"
}

fun feedbackText(score: Int): String = when (score) {
    0 -> "Давай начнём!"
    in 1..<5 -> "У тебя всё получится"
    in 5..<12 -> "Надо поднажать"
    in 12..<17 -> "Ты близок к цели"
    in 17..21 -> "Так держать!"
    else -> ""
}

@Composable
fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = Color.Gray)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}
fun isGoogleLogin(user: FirebaseUser?): Boolean {
    return user?.providerData?.any { it.providerId == "google.com" } == true
}

suspend fun updateFirebaseName(newName: String) {
    val user = FirebaseAuth.getInstance().currentUser
        ?: throw Exception("Пользователь не найден")

    val uid = user.uid

    val profileUpdates = UserProfileChangeRequest.Builder()
        .setDisplayName(newName)
        .build()

    user.updateProfile(profileUpdates).await()

    FirebaseFirestore.getInstance()
        .collection("users")
        .document(uid)
        .update("name", newName)
        .await()
}


suspend fun updateFirebaseEmail(newEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid ?: return onError("Пользователь не найден")

    try {
        user.updateEmail(newEmail).await()
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("email", newEmail)
            .await()

        onSuccess()
    } catch (e: Exception) {
        onError(e.localizedMessage ?: "Ошибка при обновлении почты")
    }
}

suspend fun updateFirebasePassword(newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser ?: return onError("Пользователь не найден")
    try {
        user.updatePassword(newPassword).await()
        onSuccess()
    } catch (e: Exception) {
        onError(e.localizedMessage ?: "Ошибка при обновлении пароля")
    }
}
