package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.model.Retailer
import com.example.ui.theme.*
import com.example.util.CloudinaryUploader
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    retailer: Retailer,
    onSave: (Retailer) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var shopName by remember { mutableStateOf(retailer.shopName) }
    var proprietorName by remember { mutableStateOf(retailer.name) }
    var phone by remember { mutableStateOf(retailer.phone) }
    var email by remember { mutableStateOf(retailer.email) }
    var gstNumber by remember { mutableStateOf(retailer.gstNumber) }
    var businessType by remember { mutableStateOf(retailer.businessType) }
    var address by remember { mutableStateOf(retailer.address) }
    var area by remember { mutableStateOf(retailer.area) }
    var city by remember { mutableStateOf(retailer.city) }
    var state by remember { mutableStateOf(retailer.state.ifBlank { "Gujarat" }) }
    var pincode by remember { mutableStateOf(retailer.pincode) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentPhotoUrl by remember { mutableStateOf(retailer.profilePhotoUrl) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLightBg)
    ) {
        Surface(color = IndustrialCharcoal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Edit Retailer Profile",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Photo Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Profile Photo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = IndustrialDark
                    )
                    Text(
                        text = "Shown on your profile and Admin Retailer Accounts",
                        fontSize = 12.sp,
                        color = SlateGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(SlateLightBg)
                            .border(2.dp, SafetyOrangePrimary, CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val imageModel: Any? = selectedImageUri ?: currentPhotoUrl.ifBlank { null }
                        if (imageModel != null) {
                            SubcomposeAsyncImage(
                                model = imageModel,
                                contentDescription = "Profile Photo Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                loading = {
                                    CircularProgressIndicator(
                                        color = SafetyOrangePrimary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.padding(20.dp)
                                    )
                                },
                                error = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = SlateGray,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = SlateGray,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SafetyOrangePrimary),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedImageUri != null || currentPhotoUrl.isNotBlank()) "Change Photo" else "Upload Photo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (selectedImageUri != null) {
                        Text(
                            text = "New photo selected. Click 'Save Profile Changes' below to upload.",
                            fontSize = 11.sp,
                            color = StockGreen,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Profile Information Form
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Business & Contact Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = IndustrialDark
                    )

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("Shop / Business Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = proprietorName,
                        onValueChange = { proprietorName = it },
                        label = { Text("Proprietor / Contact Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Primary Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gstNumber,
                        onValueChange = { gstNumber = it.uppercase() },
                        label = { Text("GST Number / GSTIN (15 Digits)") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Syncs directly with Admin Retailer Accounts & Invoices") }
                    )
                    OutlinedTextField(
                        value = businessType,
                        onValueChange = { businessType = it },
                        label = { Text("Business Type (e.g. Hardware Store, Workshop)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Full Shop Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = area,
                            onValueChange = { area = it },
                            label = { Text("Area / Market") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pincode,
                            onValueChange = { pincode = it },
                            label = { Text("Pincode") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = StockRedBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = StockRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    isSaving = true
                    errorMessage = null

                    coroutineScope.launch {
                        try {
                            var uploadedPhotoUrl = currentPhotoUrl
                            val uriToUpload = selectedImageUri

                            if (uriToUpload != null) {
                                val uploadResult = CloudinaryUploader.uploadImageFromUri(
                                    context = context,
                                    imageUri = uriToUpload,
                                    folder = CloudinaryUploader.FOLDER_RETAILER_PROFILES
                                )
                                if (uploadResult.isSuccess) {
                                    uploadedPhotoUrl = uploadResult.getOrThrow()
                                    currentPhotoUrl = uploadedPhotoUrl
                                } else {
                                    val err = uploadResult.exceptionOrNull()?.message ?: "Photo upload failed"
                                    errorMessage = "Warning: Could not upload photo ($err). Saving other details..."
                                }
                            }

                            val updated = retailer.copy(
                                shopName = shopName.trim(),
                                name = proprietorName.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                gstNumber = gstNumber.trim().uppercase(),
                                businessType = businessType.trim(),
                                address = address.trim(),
                                area = area.trim(),
                                city = city.trim(),
                                state = state.trim(),
                                pincode = pincode.trim(),
                                profilePhotoUrl = uploadedPhotoUrl.trim(),
                                updatedAt = System.currentTimeMillis()
                            )
                            isSaving = false
                            onSave(updated)
                        } catch (e: Exception) {
                            isSaving = false
                            errorMessage = "Error saving profile: ${e.message}"
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrangePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving Profile Changes...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                    Text("Save Profile Changes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
