package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.tvFocusable
import com.example.ui.viewmodel.MovieViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: MovieViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRegisterMode by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPendingApprovalDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    fun performSubmit() {
        if (email.trim().isEmpty() || password.trim().isEmpty()) {
            errorMessage = "Por favor introduce correo y contraseña"
            return
        }
        if (isRegisterMode && name.trim().isEmpty()) {
            errorMessage = "Por favor introduce tu nombre"
            return
        }

        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            val result = if (isRegisterMode) {
                viewModel.register(email.trim(), password.trim(), name.trim())
            } else {
                viewModel.login(email.trim(), password.trim())
            }

            isLoading = false
            result.onSuccess { user ->
                if (isRegisterMode && !user.isAdmin && !user.isApproved) {
                    // Non-admin user registered: requires administrator approval before accessing
                    showPendingApprovalDialog = true
                    isRegisterMode = false
                    password = ""
                } else {
                    Toast.makeText(
                        context,
                        if (user.isAdmin) "¡Bienvenido Administrador ${user.name}!" else "¡Bienvenido ${user.name}!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onAuthSuccess()
                }
            }.onFailure { err ->
                val rawMsg = err.message ?: "Error en la autenticación"
                errorMessage = if (rawMsg.contains("PENDING_APPROVAL", ignoreCase = true) || rawMsg.contains("pendiente de aprobación", ignoreCase = true)) {
                    "⏳ Tu cuenta está pendiente de aprobación por el administrador (juanjocarrillo7@gmail.com). El administrador debe activar tu cuenta antes de que puedas acceder."
                } else {
                    rawMsg
                }
            }
        }
    }

    if (showPendingApprovalDialog) {
        AlertDialog(
            onDismissRequest = { showPendingApprovalDialog = false },
            title = {
                Text(
                    text = "⏳ Registro Pendiente de Aprobación",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¡Tu cuenta ha sido creada con éxito!\n\nPor políticas de la plataforma, un administrador debe activar tu usuario antes de que puedas acceder al catálogo.\n\nEl administrador revisará tu solicitud para activar tu cuenta.",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showPendingApprovalDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8E1))
                ) {
                    Text("Entendido", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF0F1E36)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1E36), // Deep cinematic Navy
                        Color(0xFF09111E)  // Dark Slate
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .verticalScroll(scrollState)
                .padding(vertical = 24.dp)
        ) {
            // App Branding Logo
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF00A8E1).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00A8E1).copy(alpha = 0.4f)),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF00A8E1),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRIME",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "PLEX",
                        color = Color(0xFFFF9900),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Text(
                text = if (isRegisterMode) "Crear Cuenta de Usuario" else "Iniciar Sesión",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = if (isRegisterMode)
                    "Regístrate para ver todo el catálogo de películas y series."
                else
                    "Accede a tu cuenta o ingresa como administrador para gestionar la biblioteca.",
                color = Color.LightGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // TAB SELECTOR: Iniciar Sesión / Registrarse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2E4A), shape = RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (!isRegisterMode) Color(0xFF00A8E1) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            isRegisterMode = false
                            errorMessage = null
                        }
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        color = if (!isRegisterMode) Color.Black else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isRegisterMode) Color(0xFF00A8E1) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            isRegisterMode = true
                            errorMessage = null
                        }
                ) {
                    Text(
                        text = "Registrarse",
                        color = if (isRegisterMode) Color.Black else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // INPUT CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre Completo", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00A8E1),
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .testTag("auth_name_input")
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico / Usuario", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00A8E1),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .testTag("auth_email_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(
                                    text = if (passwordVisible) "Ocultar" else "Ver",
                                    color = Color(0xFF00A8E1),
                                    fontSize = 12.sp
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { performSubmit() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00A8E1),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .testTag("auth_password_input")
                    )

                    errorMessage?.let { error ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Red.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Text(
                                text = "⚠️ $error",
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { performSubmit() },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8E1)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tvFocusable(shape = RoundedCornerShape(8.dp), focusedScale = 1.04f)
                            .testTag("auth_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isRegisterMode) "Registrar y Entrar" else "Entrar",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notice about Admin rights vs Normal user rights per user request
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.05f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ℹ️ Acceso y Control de la Plataforma:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Administrador: Accede introduciendo su usuario (juanjocarrillo7@gmail.com) y contraseña para gestionar la biblioteca y activar o eliminar cuentas.\n" +
                                "• Nuevos Usuarios: Al registrarte, tu cuenta quedará pendiente de aprobación. No podrás acceder hasta que el administrador la active.\n" +
                                "• Perfiles: Cada usuario registrado puede crear y gestionar varios perfiles personalizados e independientes dentro de su cuenta.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
