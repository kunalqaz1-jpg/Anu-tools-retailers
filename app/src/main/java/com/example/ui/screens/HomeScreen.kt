package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.BusinessSettings
import com.example.model.Category
import com.example.model.Product
import com.example.model.Retailer
import com.example.ui.components.ProductCard
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    retailer: Retailer?,
    products: List<Product>,
    categories: List<Category>,
    businessSettings: BusinessSettings,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    onProductClick: (Product) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onAddToCart: (Product) -> Unit,
    onSearchClick: () -> Unit,
    onBrowseAllClick: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLightBg)
    ) {
        val isSmall = maxWidth < 360.dp
        val screenPad = if (isSmall) 12.dp else 16.dp
        val categoryCardWidth = if (isSmall) 120.dp else 138.dp
        val itemsPerRow = if (maxWidth < 320.dp) 1 else 2

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Retailer Welcome Banner & Fast Search Bar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(IndustrialCharcoal, IndustrialDark)
                            )
                        )
                        .padding(horizontal = screenPad, vertical = 18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Hello, ${retailer?.shopName ?: "Partner Retailer"}",
                                    fontSize = if (isSmall) 16.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Anu Tools & Service Center · Vesu, Surat",
                                    fontSize = 12.sp,
                                    color = SafetyOrangeLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Clickable Search Field
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSearchClick() }
                                .testTag("home_search_bar")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = SlateGray
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Search tools, machines, brands (Bosch, Makita)...",
                                    fontSize = if (isSmall) 12.sp else 13.sp,
                                    color = SlateGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 2. Hero Machinery Banner - FULLY CLICKABLE
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = IndustrialCharcoal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = screenPad, vertical = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onBrowseAllClick() }
                        .testTag("hero_browse_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isSmall) 140.dp else 155.dp)
                    ) {
                        // Generated Hero Banner image
                        Image(
                            painter = painterResource(id = R.drawable.banner_machinery_hero_1789028232924),
                            contentDescription = "Industrial Machinery",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            IndustrialDark.copy(alpha = 0.92f),
                                            IndustrialDark.copy(alpha = 0.65f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(if (isSmall) 12.dp else 16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = SafetyOrangePrimary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "OFFICIAL DISTRIBUTOR",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tools that keep your\nbusiness moving.",
                                fontSize = if (isSmall) 15.sp else 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = if (isSmall) 19.sp else 22.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Browse Catalogue",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Browse",
                                    tint = SafetyOrangeLight,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Category Horizontal Scroll
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = screenPad),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IndustrialDark
                        )
                        TextButton(onClick = onBrowseAllClick) {
                            Text(
                                text = "View All",
                                color = SafetyOrangePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (categories.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = screenPad, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            repeat(3) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.width(categoryCardWidth)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF1F5F9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = SafetyOrangePrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Syncing...",
                                            fontSize = 11.sp,
                                            color = SlateGray
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = screenPad),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(categories) { category ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .width(categoryCardWidth)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onCategoryClick(category) }
                                        .testTag("category_chip_${category.id}")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        AsyncImage(
                                            model = category.imageUrl,
                                            contentDescription = category.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF1F5F9))
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = category.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = IndustrialDark,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            minLines = 2,
                                            lineHeight = 15.sp,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        val countText = when (category.productCount) {
                                            0 -> "0 items"
                                            1 -> "1 item"
                                            else -> "${category.productCount} items"
                                        }
                                        Text(
                                            text = countText,
                                            fontSize = 10.sp,
                                            color = SlateGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Featured Machinery
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = screenPad),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Featured Products",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IndustrialDark
                        )
                        Text(
                            text = "High-demand workshop & contractor tools",
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                    }
                    Button(
                        onClick = onBrowseAllClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafetyOrangeContainer,
                            contentColor = SafetyOrangeDark
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("See All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Products Grid / List in items
            val featured = products.take(6)
            if (isLoading) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = screenPad, vertical = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = SafetyOrangePrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Loading high-demand machinery...",
                                fontSize = 13.sp,
                                color = SlateGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F0)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCCC7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = screenPad, vertical = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFCF1322),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Firestore Product Loading Error",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCF1322),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage,
                                fontSize = 12.sp,
                                color = Color(0xFF434343),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRetry,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrangePrimary),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("Retry", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            } else if (featured.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = screenPad, vertical = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = SlateGray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No active products currently in inventory",
                                fontSize = 13.sp,
                                color = SlateGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                items(featured.chunked(itemsPerRow)) { rowProducts ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = screenPad, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (product in rowProducts) {
                            Box(modifier = Modifier.weight(1f)) {
                                ProductCard(
                                    product = product,
                                    onProductClick = onProductClick,
                                    onAddToCart = onAddToCart
                                )
                            }
                        }
                        if (rowProducts.size < itemsPerRow) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 5. Why Buy From Anu Tools Trust Badges
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = screenPad)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Why Retailers Choose Anu Tools",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = IndustrialDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val points = listOf(
                            Triple(Icons.Default.Verified, "100% Genuine Machinery", "Authorized distributor for Bosch, Makita, Dewalt & Dongcheng"),
                            Triple(Icons.Default.PriceCheck, "Best Pricing", "Best tier margins for hardware shops, workshops & contractors"),
                            Triple(Icons.Default.Build, "Dedicated Service Center", "Original armatures, carbon brushes, and repairs under one roof"),
                            Triple(Icons.Default.LocalShipping, "Same-Day Dispatch", "Fast local delivery & express cargo across Surat & South Gujarat")
                        )

                        points.forEach { (icon, title, desc) ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(SafetyOrangeContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        tint = SafetyOrangeDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IndustrialDark
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = SlateGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Direct Contact Support Card
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = IndustrialCharcoal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = screenPad)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = "Support",
                                tint = SafetyOrangeLight,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Anu Tools Desk & Service Center",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = businessSettings.phone,
                                    color = SafetyOrangeLight,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = businessSettings.supportMessage,
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        val context = LocalContext.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onContactClick,
                                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrangePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSmall) "Desk" else "Contact Desk", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.app.goo.gl/ermyxbBhyHPNej2m6"))
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Map", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isSmall) "Store" else "Surat Store", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
