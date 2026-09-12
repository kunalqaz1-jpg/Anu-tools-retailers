package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.model.Retailer
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    retailer: Retailer,
    onEditProfile: () -> Unit,
    onContactSupport: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLightBg)
    ) {
        val isSmall = maxWidth < 360.dp
        val screenPad = if (isSmall) 12.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(screenPad),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Retailer Header Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = IndustrialCharcoal),
                modifier = Modifier.fillMaxWidth()
            ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(SafetyOrangePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (retailer.profilePhotoUrl.isNotBlank()) {
                            SubcomposeAsyncImage(
                                model = retailer.profilePhotoUrl,
                                contentDescription = "Retailer Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                loading = {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                },
                                error = {
                                    Text(
                                        text = retailer.shopName.take(2).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                }
                            )
                        } else {
                            Text(
                                text = retailer.shopName.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = retailer.shopName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Prop: ${retailer.name}",
                            fontSize = 13.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (retailer.status) {
                                "ACTIVE" -> StockGreen.copy(alpha = 0.25f)
                                "PENDING" -> StockAmber.copy(alpha = 0.25f)
                                else -> StockGreen.copy(alpha = 0.25f)
                            }
                        ) {
                            Text(
                                text = "VERIFIED RETAILER (${retailer.status})",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (retailer.status) {
                                    "ACTIVE" -> StockGreen
                                    "PENDING" -> StockAmber
                                    else -> StockGreen
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Business Details Section
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shop & Tax Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = IndustrialDark
                    )
                    TextButton(onClick = onEditProfile) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp), tint = SafetyOrangePrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", color = SafetyOrangePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ProfileItemRow(icon = Icons.Outlined.Store, label = "Business Type", value = retailer.businessType)
                ProfileItemRow(icon = Icons.Outlined.Receipt, label = "GSTIN / GST Number", value = retailer.gstNumber.ifEmpty { "Not Provided" })
                ProfileItemRow(icon = Icons.Outlined.Phone, label = "Contact Phone", value = retailer.phone)
                ProfileItemRow(icon = Icons.Outlined.Email, label = "Email Address", value = retailer.email.ifEmpty { "Not Provided" })
                val stateText = if (retailer.state.isNotBlank()) ", ${retailer.state}" else ""
                ProfileItemRow(icon = Icons.Outlined.LocationOn, label = "Shop Location", value = "${retailer.address}, ${retailer.area}, ${retailer.city}$stateText - ${retailer.pincode}")
            }
        }



        // Contact Anu Tools Desk
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Wholesale & Service Support",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = IndustrialDark
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onContactSupport,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, IndustrialCharcoal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = IndustrialCharcoal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact Anu Tools Desk & Service Center", color = IndustrialCharcoal, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Logout
        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, StockRed),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = StockRed, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Switch Retailer / Sign Out", color = StockRed, fontWeight = FontWeight.Bold)
        }
    }
}
}

@Composable
fun ProfileItemRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = SlateGray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, color = SlateGray)
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = IndustrialDark
            )
        }
    }
}
