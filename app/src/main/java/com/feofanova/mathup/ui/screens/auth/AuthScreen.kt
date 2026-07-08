package com.feofanova.mathup.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavHostController

import com.feofanova.mathup.ui.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import androidx.credentials.GetCredentialRequest
import com.feofanova.mathup.ui.screens.main.SetStatusBarColor
import com.feofanova.mathup.R
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential


@Composable
fun AuthScreen(navController: NavHostController) {
    SetStatusBarColor(color = Color(0xFF1F2E59), darkIcons = false)
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1F2E59)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Row(Modifier.fillMaxWidth()) {
                TabButton(text = "ВХОД", selected = isLogin, modifier = Modifier.weight(1f)) { isLogin = true }
                TabButton(text = "РЕГИСТРАЦИЯ", selected = !isLogin, modifier = Modifier.weight(1f)) { isLogin = false }
            }

            Spacer(modifier = Modifier.height(64.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (!isLogin) {
                        CustomOutlinedField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Введите имя"
                        )
                    }
                    CustomOutlinedField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Введите почту"
                    )
                    CustomOutlinedField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Введите пароль",
                        isPassword = true,
                        showPassword = showPassword,
                        onVisibilityToggle = { showPassword = !showPassword }
                    )

                    if (isLogin) {
                        Text(
                            text = "Забыли пароль?",
                            color = Color(0xFF4CAF50),
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp)
                                .clickable {
                                    if (email.isBlank()) {
                                        alertMessage = "Введите почту для сброса пароля"
                                        showAlert = true
                                    } else {
                                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                            .addOnSuccessListener {
                                                alertMessage = "Ссылка для сброса отправлена"
                                                showAlert = true
                                            }
                                            .addOnFailureListener {
                                                alertMessage = it.localizedMessage ?: "Ошибка"
                                                showAlert = true
                                            }
                                    }
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                alertMessage = "Заполните почту и пароль"
                                showAlert = true
                                return@Button
                            }
                            val auth = FirebaseAuth.getInstance()
                            if (isLogin) {
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnSuccessListener {
                                        navController.navigate(Routes.MAIN) {
                                            popUpTo(Routes.AUTH) { inclusive = true }
                                        }
                                    }
                                    .addOnFailureListener {
                                        alertMessage = it.localizedMessage ?: "Ошибка входа"
                                        showAlert = true
                                    }
                            } else {
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnSuccessListener { result ->
                                        val uid = result.user?.uid ?: return@addOnSuccessListener
                                        FirebaseFirestore.getInstance().collection("users")
                                            .document(uid)
                                            .set(
                                                mapOf(
                                                    "name" to name,
                                                    "email" to email,
                                                    "createdAt" to com.google.firebase.Timestamp.now(),
                                                    "installSource" to "GooglePlay"
                                                )
                                            )
                                        alertMessage = "Регистрация прошла успешно"
                                        showAlert = true
                                        isLogin = true
                                    }
                                    .addOnFailureListener {
                                        alertMessage = it.localizedMessage ?: "Ошибка регистрации"
                                        showAlert = true
                                    }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text(
                            text = if (isLogin) "Войти" else "Зарегистрироваться",
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isLogin) "Войти с помощью:" else "Зарегистрироваться с помощью:",
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            GoogleSignInButton(
                navController = navController,
                onError = { msg ->
                    alertMessage = msg
                },
                onShowAlert = {
                    showAlert = true
                }
            )



            if (showAlert) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = alertMessage, color = Color.White)
            }
        }
    }
}
@Composable
fun GoogleSignInButton(
    navController: NavHostController,
    onError: (String) -> Unit,
    onShowAlert: () -> Unit
) {
    val context = LocalContext.current
    // помним CredentialManager, чтобы не каждый клик заново его создавать
    val credentialManager = remember { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    Image(
        painter = painterResource(id = R.drawable.android_dark_rd_su),
        contentDescription = "Войти с Google",
        modifier = Modifier
            .width(312.dp)
            .height(48.dp)
            .clickable (
                indication = null,
                interactionSource = interactionSource
            ){
                coroutineScope.launch {
                    try {
                        val signInOption = GetSignInWithGoogleOption.Builder(
                            context.getString(R.string.default_web_client_id)
                        )
                            .setNonce("")
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(signInOption)
                            .build()

                        val result = credentialManager.getCredential(context, request)

                        val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                        val idToken = googleCredential.idToken


                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        FirebaseAuth.getInstance()
                            .signInWithCredential(firebaseCredential)
                            .addOnSuccessListener { authResult ->
                                val user = authResult.user ?: return@addOnSuccessListener

                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(user.uid)
                                    .set(
                                        mapOf(
                                            "name" to user.displayName.orEmpty(),
                                            "email" to user.email.orEmpty(),
                                            "createdAt" to com.google.firebase.Timestamp.now(),
                                            "installSource" to "GoogleOAuth"
                                        )
                                    )

                                navController.navigate(Routes.MAIN) {
                                    popUpTo(Routes.AUTH) { inclusive = true }
                                }
                            }

                            .addOnFailureListener { e ->
                                onError("Ошибка Firebase входа: ${e.localizedMessage}")
                                onShowAlert()
                            }

                    } catch (e: GetCredentialCancellationException) {
                        onError("Авторизация отменена")
                        onShowAlert()
                    } catch (e: GetCredentialException) {
                        onError("Ошибка авторизации: ${e.message}")
                        onShowAlert()
                    } catch (e: Exception) {
                        onError("Неизвестная ошибка: ${e.localizedMessage}")
                        onShowAlert()
                    }
                }
            }
    )
}



@Composable
fun CustomOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onVisibilityToggle: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword && onVisibilityToggle != null) {
            {
                IconButton(onClick = onVisibilityToggle) {
                    Icon(
                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF4CAF50),
            unfocusedBorderColor = Color.Gray,
            focusedLabelColor = Color(0xFF4CAF50),
            cursorColor = Color(0xFF4CAF50)
        )
    )
}

@Composable
private fun TabButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .height(3.dp)
                .fillMaxWidth()
                .background(if (selected) Color.White else Color.Transparent)
        )
    }
}
