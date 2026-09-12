package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.Product
import com.example.ui.components.StockBadge
import com.example.ui.theme.*

@Composable
fun ProductDetailScreen(
    product: Product,
    onAddToCart: (Product, Int) -> Unit,
    onBack: () -> Unit,
    onContactSupport: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    var quantity by remember { mutableIntStateOf(1) }
    var showAddedSnackbar by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLightBg)
    ) {
        val isSmall = maxWidth < 360.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp)
        ) {
            // Product Hero Image with Back Button
            val context = LocalContext.current
            
            // Build gallery image list: thumbnailUrl as primary, and all images[]
            val galleryImages = remember(product) {
                val list = mutableListOf<String>()
                if (product.thumbnailUrl.isNotBlank()) {
                    list.add(product.thumbnailUrl)
                }
                product.images.forEach { img ->
                    if (img.isNotBlank() && !list.contains(img)) {
                        list.add(img)
                    }
                }
                if (list.isEmpty() && product.images.isNotEmpty()) {
                    list.addAll(product.images.filter { it.isNotBlank() })
                }
                list
            }

            var selectedImageUrl by remember(product) {
                mutableStateOf(product.thumbnailUrl.ifBlank { galleryImages.firstOrNull() ?: "" })
            }
            val activeImageUrl = selectedImageUrl.ifBlank {
                product.thumbnailUrl.ifBlank { galleryImages.firstOrNull() ?: "" }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmall) 260.dp else 300.dp)
                    .background(Color(0xFFF1F5F9))
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(activeImageUrl.ifBlank { null })
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = SafetyOrangePrimary,
                                strokeWidth = 3.dp
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF8FAFC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = SlateGray,
                                    modifier = Modifier.size(52.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = product.name,
                                    fontSize = 12.sp,
                                    color = SlateGray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp)
                )

                // Top Bar with Back button and Brand Name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = IndustrialDark
                        )
                    }

                    if (product.brandName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IndustrialCharcoal.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = product.brandName.uppercase(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Image Gallery Selector showing all images[]
            if (galleryImages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    galleryImages.forEachIndexed { index, imgUrl ->
                        val isSelected = imgUrl == activeImageUrl
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) SafetyOrangePrimary else SlateBorder
                            ),
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedImageUrl = imgUrl }
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imgUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Gallery image ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Main Product Details Card
            Card(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-8).dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Availability status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StockBadge(status = product.stockStatus, quantity = product.stockQuantity)
                        Text(
                            text = "SKU: ${product.sku}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateGray
                        )
                    }

                    // Brand & Category Badges
                    if (product.brandName.isNotBlank() || product.categoryName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (product.brandName.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = IndustrialCharcoal
                                ) {
                                    Text(
                                        text = product.brandName,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (product.categoryName.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SlateLightBg,
                                    border = BorderStroke(1.dp, SlateBorder)
                                ) {
                                    Text(
                                        text = product.categoryName,
                                        color = IndustrialDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = IndustrialDark
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Price Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SafetyOrangeContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Retailer Price",
                                fontSize = 11.sp,
                                color = SlateGray,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "₹${"%,.0f".format(product.effectivePrice)}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SafetyOrangePrimary
                                )
                                Text(
                                    text = " / ${product.unit}",
                                    fontSize = 13.sp,
                                    color = SlateGray,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }

                        if (product.mrp > product.effectivePrice) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "MRP ₹${"%,.0f".format(product.mrp)}",
                                    fontSize = 13.sp,
                                    color = SlateGray,
                                    textDecoration = TextDecoration.LineThrough
                                )
                                Surface(
                                    color = SafetyOrangePrimary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${product.discountPercent}% MARGIN",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // GST Note
                    Text(
                        text = "• Inclusive of ${product.gstPercentage.toInt()}% GST • Official Tax Invoice provided",
                        fontSize = 12.sp,
                        color = StockGreen,
                        fontWeight = FontWeight.Medium
                    )

                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = SlateBorder)

                    // Short Overview
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IndustrialDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.shortDescription,
                        fontSize = 14.sp,
                        color = IndustrialDark,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.description,
                        fontSize = 13.sp,
                        color = SlateGray,
                        lineHeight = 18.sp
                    )

                    // Specifications dynamic key/value table
                    if (product.specifications.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 16.dp), color = SlateBorder)
                        Text(
                            text = "Technical Specifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IndustrialDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateLightBg),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                product.specifications.entries.forEachIndexed { index, (key, value) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 13.sp,
                                            color = SlateGray,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        Text(
                                            text = value,
                                            fontSize = 13.sp,
                                            color = IndustrialDark,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (index < product.specifications.size - 1) {
                                        Divider(color = SlateBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
                                    }
                                }
                            }
                        }
                    }

                    // Contact Anu Tools regarding this item
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { onContactSupport(product) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, IndustrialCharcoal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Contact Desk",
                            tint = IndustrialCharcoal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Inquire Machine Specs with Anu Tools Desk",
                            color = IndustrialCharcoal,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Sticky Bottom Bar with Quantity and Add to Cart
        Surface(
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = if (isSmall) 10.dp else 16.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity Selector
                if (product.isAvailable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(SlateLightBg, RoundedCornerShape(8.dp))
                            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = quantity.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = if (isSmall) 6.dp else 10.dp)
                        )
                        IconButton(
                            onClick = { if (quantity < product.stockQuantity) quantity++ },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(if (isSmall) 8.dp else 12.dp))
                }

                // Add to Cart Button
                Button(
                    onClick = {
                        if (product.isAvailable) {
                            onAddToCart(product, quantity)
                            showAddedSnackbar = true
                        } else {
                            onContactSupport(product)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (product.isAvailable) SafetyOrangePrimary else IndustrialCharcoal
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("detail_add_to_cart_button")
                ) {
                    Icon(
                        imageVector = if (product.isAvailable) Icons.Default.ShoppingCart else Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (product.isAvailable) {
                            if (isSmall) "Add ₹${"%,.0f".format(product.effectivePrice * quantity)}" else "Add to Cart (₹${"%,.0f".format(product.effectivePrice * quantity)})"
                        } else {
                            if (isSmall) "Out of Stock" else "Contact Us (Out of Stock)"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isSmall) 12.sp else 14.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
