package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AnuToolsRepository
import com.example.model.Retailer
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    repository: AnuToolsRepository,
    onLoginSuccess: (Retailer) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var isRegisterMode by remember { mutableStateOf(false) }

    // Form inputs
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Registration specific fields
    var shopNameInput by remember { mutableStateOf("") }
    var contactNameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IndustrialCharcoal)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Anu Tools Brand Logo Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .padding(bottom = 14.dp)
                .width(180.dp)
                .height(68.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_anu_tools_logo_1789031169278),
                    contentDescription = "Anu Tools Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Text(
            text = "Anu Tools & Service Center",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Retailer & Workshop Purchasing Portal",
            fontSize = 13.sp,
            color = SafetyOrangeLight
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Auth Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mode Toggle Tabs
                TabRow(
                    selectedTabIndex = if (isRegisterMode) 1 else 0,
                    containerColor = SlateLightBg,
                    contentColor = SafetyOrangePrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = !isRegisterMode,
                        onClick = {
                            isRegisterMode = false
                            errorMessage = null
                        },
                        text = {
                            Text(
                                "Login",
                                fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isRegisterMode) SafetyOrangePrimary else SlateGray
                            )
                        }
                    )
                    Tab(
                        selected = isRegisterMode,
                        onClick = {
                            isRegisterMode = true
                            errorMessage = null
                        },
                        text = {
                            Text(
                                "Register Shop",
                                fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (isRegisterMode) SafetyOrangePrimary else SlateGray
                            )
                        }
                    )
                }

                // Error message banner
                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFD32F2F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Registration Fields: Shop Name & Proprietor Name
                if (isRegisterMode) {
                    OutlinedTextField(
                        value = shopNameInput,
                        onValueChange = { shopNameInput = it },
                        label = { Text("Shop / Business Name *") },
                        leadingIcon = {
                            Icon(Icons.Default.Store, contentDescription = null, tint = SlateGray)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = contactNameInput,
                        onValueChange = { contactNameInput = it },
                        label = { Text("Proprietor / Contact Name *") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = SlateGray)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Mobile Number (10 digits) *") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = SlateGray)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Email Address (Common)
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email Address *") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = SlateGray)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.fillMaxWidth()
                )

                // Password (Common)
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text(if (isRegisterMode) "Password (min 6 chars) *" else "Password *") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SlateGray)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = SlateGray
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Registration Field: Shop Address
                if (isRegisterMode) {
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Shop Address (Street, Area, City) *") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = SlateGray)
                        },
                        minLines = 2,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Submit Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        errorMessage = null

                        val email = emailInput.trim()
                        val pass = passwordInput

                        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            errorMessage = "Please enter a valid email address."
                            return@Button
                        }
                        if (pass.length < 6) {
                            errorMessage = "Password must be at least 6 characters long."
                            return@Button
                        }

                        if (isRegisterMode) {
                            if (shopNameInput.isBlank()) {
                                errorMessage = "Please enter your Shop / Business Name."
                                return@Button
                            }
                            if (contactNameInput.isBlank()) {
                                errorMessage = "Please enter Proprietor / Contact Name."
                                return@Button
                            }
                            if (phoneInput.isBlank() || phoneInput.length < 10) {
                                errorMessage = "Please enter a valid 10-digit mobile number."
                                return@Button
                            }
                            if (addressInput.isBlank()) {
                                errorMessage = "Please enter your shop address."
                                return@Button
                            }

                            isSubmitting = true
                            coroutineScope.launch {
                                val result = repository.signUpWithEmail(
                                    email = email,
                                    pass = pass,
                                    name = contactNameInput.trim(),
                                    shopName = shopNameInput.trim(),
                                    phone = phoneInput.trim(),
                                    address = addressInput.trim()
                                )
                                isSubmitting = false
                                if (result.isSuccess) {
                                    onLoginSuccess(result.getOrThrow())
                                } else {
                                    val ex = result.exceptionOrNull()
                                    errorMessage = formatAuthError(ex?.message)
                                }
                            }
                        } else {
                            isSubmitting = true
                            coroutineScope.launch {
                                val result = repository.loginWithEmail(email = email, pass = pass)
                                isSubmitting = false
                                if (result.isSuccess) {
                                    onLoginSuccess(result.getOrThrow())
                                } else {
                                    val ex = result.exceptionOrNull()
                                    errorMessage = formatAuthError(ex?.message)
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrangePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("login_submit_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRegisterMode) "Register Shop Account" else "Login to Retailer Portal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Switch between Login and Register
                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        errorMessage = null
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (isRegisterMode) "Already have an account? Login here" else "New Retailer? Register your shop",
                        color = SafetyOrangeDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun formatAuthError(rawMessage: String?): String {
    val msg = rawMessage ?: return "Authentication failed. Please check your network and try again."
    return when {
        msg.contains("password", ignoreCase = true) && msg.contains("invalid", ignoreCase = true) ->
            "Incorrect password. Please try again."
        msg.contains("no user", ignoreCase = true) || msg.contains("user-not-found", ignoreCase = true) ->
            "No retailer account found with this email. Please register."
        msg.contains("already in use", ignoreCase = true) || msg.contains("email-already-in-use", ignoreCase = true) ->
            "This email is already registered. Please login instead."
        msg.contains("badly formatted", ignoreCase = true) || msg.contains("invalid-email", ignoreCase = true) ->
            "The email address format is invalid."
        msg.contains("network", ignoreCase = true) ->
            "Network connection error. Please check your internet connection."
        msg.contains("blocked", ignoreCase = true) || msg.contains("too-many-requests", ignoreCase = true) ->
            "Too many failed attempts. Please try again in a few minutes."
        else -> msg
    }
}
