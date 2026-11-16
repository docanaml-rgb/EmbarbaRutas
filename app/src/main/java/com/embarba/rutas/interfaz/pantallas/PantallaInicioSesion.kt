package com.embarba.rutas.interfaz.pantallas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.embarba.rutas.R
import com.embarba.rutas.datos.RepositorioAutenticacion
import com.embarba.rutas.interfaz.tema.EmbarbaRutasTheme

private val LoginBackground = Color(0xFFF6F8FC)
private val InstitutionalBlue = Color(0xFF103F78)
private val BrightBlue = Color(0xFF2675D9)
private val LoginText = Color(0xFF17324D)
private val LoginSecondaryText = Color(0xFF60758A)
private val FieldBorder = Color(0xFFD4DFEB)

@Composable
fun PantallaInicioSesion(
    authRepository: RepositorioAutenticacion,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LoginBackground)
    ) {
        LoginDecorations(modifier = Modifier.fillMaxSize())

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
        ) {
            val compactLayout = maxHeight < 700.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 24.dp,
                        top = if (compactLayout) 16.dp else 0.dp,
                        end = 24.dp,
                        bottom = if (compactLayout) 16.dp else 72.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.logoembarba),
                        contentDescription = "Logo de Embarba Elevadores",
                        modifier = Modifier.size(if (compactLayout) 118.dp else 148.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(if (compactLayout) 8.dp else 14.dp))
                    Text(
                        text = "EMBARBA",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = InstitutionalBlue,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Gestión de rutas de supervisión",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LoginSecondaryText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(if (compactLayout) 22.dp else 32.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = loginTextFieldColors(),
                        isError = errorMessage != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Contraseña") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = loginTextFieldColors(),
                        isError = errorMessage != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (passwordVisible) {
                                        "Ocultar contraseña"
                                    } else {
                                        "Mostrar contraseña"
                                    }
                                )
                            }
                        }
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    Button(
                        onClick = {
                            when {
                                email.isBlank() -> errorMessage = "Ingresa tu correo electrónico."
                                password.isBlank() -> errorMessage = "Ingresa tu contraseña."
                                else -> {
                                    isLoading = true
                                    errorMessage = null
                                    authRepository.signIn(email.trim(), password) { result ->
                                        isLoading = false
                                        result.onSuccess { onLoginSuccess() }
                                        result.onFailure {
                                            errorMessage = "Correo o contraseña incorrectos. Inténtalo de nuevo."
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InstitutionalBlue,
                            contentColor = Color.White
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("INICIANDO SESIÓN", fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(if (compactLayout) 20.dp else 30.dp))
                    Text(
                        text = "Aplicación para personal de supervisión",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoginSecondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun loginTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    errorContainerColor = Color.White,
    focusedBorderColor = BrightBlue,
    unfocusedBorderColor = FieldBorder,
    focusedLabelColor = InstitutionalBlue,
    unfocusedLabelColor = LoginSecondaryText,
    focusedTextColor = LoginText,
    unfocusedTextColor = LoginText,
    cursorColor = BrightBlue
)

@Composable
private fun LoginDecorations(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        TopWaves(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .align(Alignment.TopCenter)
        )
        BottomCityWaves(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TopWaves(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val backWave = Path().apply {
            moveTo(0f, 0f)
            lineTo(width, 0f)
            lineTo(width, height * 0.44f)
            cubicTo(
                width * 0.76f, height * 0.64f,
                width * 0.48f, height * 0.27f,
                0f, height * 0.58f
            )
            close()
        }
        drawPath(backWave, color = BrightBlue.copy(alpha = 0.9f))

        val frontWave = Path().apply {
            moveTo(0f, 0f)
            lineTo(width, 0f)
            lineTo(width, height * 0.28f)
            cubicTo(
                width * 0.72f, height * 0.52f,
                width * 0.43f, height * 0.17f,
                0f, height * 0.43f
            )
            close()
        }
        drawPath(frontWave, color = InstitutionalBlue)
    }
}

@Composable
private fun BottomCityWaves(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val buildings = listOf(
            0.02f to 0.42f,
            0.12f to 0.30f,
            0.23f to 0.50f,
            0.35f to 0.36f,
            0.48f to 0.56f,
            0.60f to 0.40f,
            0.72f to 0.48f,
            0.84f to 0.32f,
            0.94f to 0.44f
        )
        val buildingWidth = width * 0.1f

        buildings.forEachIndexed { index, (xFraction, topFraction) ->
            val left = width * xFraction
            val top = height * topFraction
            drawRect(
                color = InstitutionalBlue.copy(alpha = if (index % 2 == 0) 0.08f else 0.055f),
                topLeft = Offset(left, top),
                size = Size(buildingWidth, height - top)
            )

            val windowSize = width * 0.009f
            val windowGap = width * 0.024f
            var windowY = top + windowGap
            while (windowY < height * 0.78f) {
                drawRect(
                    color = BrightBlue.copy(alpha = 0.09f),
                    topLeft = Offset(left + windowGap, windowY),
                    size = Size(windowSize, windowSize)
                )
                drawRect(
                    color = BrightBlue.copy(alpha = 0.09f),
                    topLeft = Offset(left + buildingWidth - windowGap - windowSize, windowY),
                    size = Size(windowSize, windowSize)
                )
                windowY += windowGap
            }
        }

        val backWave = Path().apply {
            moveTo(0f, height * 0.66f)
            cubicTo(
                width * 0.28f, height * 0.48f,
                width * 0.62f, height * 0.82f,
                width, height * 0.58f
            )
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(backWave, color = BrightBlue.copy(alpha = 0.18f))

        val frontWave = Path().apply {
            moveTo(0f, height * 0.79f)
            cubicTo(
                width * 0.30f, height * 0.62f,
                width * 0.68f, height * 0.94f,
                width, height * 0.72f
            )
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(frontWave, color = InstitutionalBlue.copy(alpha = 0.24f))
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    EmbarbaRutasTheme {
        PantallaInicioSesion(authRepository = RepositorioAutenticacion(), onLoginSuccess = {})
    }
}
