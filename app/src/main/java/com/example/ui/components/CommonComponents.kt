package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.Product
import com.example.model.StatusUpdate
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StockBadge(status: String, quantity: Int = 0, modifier: Modifier = Modifier) {
    val (bgColor, textColor, text) = when (status) {
        "IN_STOCK" -> Triple(StockGreenBg, StockGreen, "IN STOCK")
        "LOW_STOCK" -> Triple(StockAmberBg, StockAmber, "LIMITED AVAILABILITY")
        else -> Triple(StockRedBg, StockRed, "OUT OF STOCK")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onProductClick(product) }
            .testTag("product_card_${product.id}")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isTight = maxWidth < 160.dp
            val horizontalPad = if (isTight) 8.dp else 10.dp
            val imageHeight = if (isTight) 115.dp else 135.dp

            Column(modifier = Modifier.fillMaxWidth()) {
                // Product image box with brand tag and discount
                val context = LocalContext.current
                val displayUrl = product.thumbnailUrl.ifBlank {
                    product.images.firstOrNull { it.isNotBlank() } ?: ""
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .background(Color(0xFFF1F5F9))
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(displayUrl.ifBlank { null })
                            .crossfade(true)
                            .build(),
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = SafetyOrangePrimary,
                                    strokeWidth = 2.dp
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
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = SlateGray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Brand Pill
                    if (product.brandName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = IndustrialCharcoal.copy(alpha = 0.88f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                        ) {
                            Text(
                                text = product.brandName.uppercase(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Discount Pill
                    if (product.discountPercent > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SafetyOrangePrimary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            Text(
                                text = "${product.discountPercent}% OFF",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontalPad)) {
                    // Stock indicator
                    StockBadge(status = product.stockStatus, quantity = product.stockQuantity)

                    Spacer(modifier = Modifier.height(4.dp))

                    // Category & Brand Label
                    if (product.categoryName.isNotBlank() || product.brandName.isNotBlank()) {
                        val brandCategoryLabel = listOf(product.brandName, product.categoryName)
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                        Text(
                            text = brandCategoryLabel,
                            fontSize = 10.sp,
                            color = SafetyOrangeDark,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Product Name - uniform 2 lines
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IndustrialDark,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp,
                        fontSize = if (isTight) 12.sp else 13.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // SKU
                    Text(
                        text = "SKU: ${product.sku}",
                        fontSize = 10.sp,
                        color = SlateGray,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pricing & Add to Cart
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "₹${"%,.0f".format(product.effectivePrice)}",
                                fontSize = if (isTight) 14.sp else 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafetyOrangePrimary,
                                maxLines = 1
                            )
                            if (product.mrp > product.effectivePrice) {
                                Text(
                                    text = "MRP ₹${"%,.0f".format(product.mrp)}",
                                    fontSize = 10.sp,
                                    color = SlateGray,
                                    textDecoration = TextDecoration.LineThrough,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Compact Add Button
                        Button(
                            onClick = { onAddToCart(product) },
                            enabled = product.isAvailable,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SafetyOrangePrimary,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFE2E8F0),
                                disabledContentColor = SlateGray
                            ),
                            contentPadding = PaddingValues(horizontal = if (isTight) 6.dp else 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .defaultMinSize(minWidth = 1.dp, minHeight = 30.dp)
                                .height(30.dp)
                                .testTag("add_to_cart_${product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                modifier = Modifier.size(14.dp)
                            )
                            if (!isTight) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (product.isAvailable) "Add" else "Sold",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnuTopBar(
    title: String,
    subtitle: String? = null,
    showBack: Boolean = false,
    showLogo: Boolean = false,
    onBack: () -> Unit = {},
    cartItemCount: Int = 0,
    onCartClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    onNotificationClick: () -> Unit = {},
    onSearchClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else if (showLogo) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White,
                    modifier = Modifier
                        .padding(start = 12.dp, end = 6.dp)
                        .size(width = 42.dp, height = 30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(2.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_anu_tools_logo_1789031169278),
                            contentDescription = "Anu Tools Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        },
        actions = {
            if (onSearchClick != null) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
            }
            IconButton(onClick = onNotificationClick) {
                BadgedBox(
                    badge = {
                        if (unreadNotificationCount > 0) {
                            Badge(
                                containerColor = SafetyOrangePrimary,
                                contentColor = Color.White
                            ) {
                                Text(unreadNotificationCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White
                    )
                }
            }
            IconButton(onClick = onCartClick) {
                BadgedBox(
                    badge = {
                        if (cartItemCount > 0) {
                            Badge(
                                containerColor = SafetyOrangePrimary,
                                contentColor = Color.White
                            ) {
                                Text(cartItemCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = "Cart",
                        tint = Color.White
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = IndustrialCharcoal
        )
    )
}

enum class NavigationTab(val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME("Home", Icons.Outlined.Home, Icons.Filled.Home),
    CATEGORIES("Categories", Icons.Outlined.Category, Icons.Filled.Category),
    CART("Cart", Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart),
    ORDERS("Orders", Icons.Outlined.ReceiptLong, Icons.Filled.ReceiptLong),
    PROFILE("Profile", Icons.Outlined.Person, Icons.Filled.Person)
}

@Composable
fun AnuBottomNavigationBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    cartItemCount: Int = 0,
    activeOrdersCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(0.5.dp, SlateBorder.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isNarrow = maxWidth < 360.dp
            val categoriesFontSize = if (isNarrow) 8.5.sp else 9.5.sp
            val normalFontSize = if (isNarrow) 9.5.sp else 10.5.sp
            val iconSize = if (isNarrow) 19.dp else 22.dp

            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
                windowInsets = NavigationBarDefaults.windowInsets,
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    val tabFontSize = if (tab == NavigationTab.CATEGORIES) categoriesFontSize else normalFontSize
                    val letterSpacing = if (tab == NavigationTab.CATEGORIES) (-0.5).sp else (-0.1).sp

                    NavigationBarItem(
                        selected = selected,
                        alwaysShowLabel = true,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            val iconModifier = Modifier.size(iconSize)
                            if (tab == NavigationTab.CART && cartItemCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = SafetyOrangePrimary,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = if (cartItemCount > 99) "99+" else cartItemCount.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                                        contentDescription = tab.title,
                                        modifier = iconModifier
                                    )
                                }
                            } else if (tab == NavigationTab.ORDERS && activeOrdersCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = SafetyOrangePrimary,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = activeOrdersCount.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                                        contentDescription = tab.title,
                                        modifier = iconModifier
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.title,
                                    modifier = iconModifier
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = tabFontSize,
                                maxLines = 1,
                                softWrap = false,
                                letterSpacing = letterSpacing,
                                overflow = TextOverflow.Clip
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SafetyOrangePrimary,
                            selectedTextColor = SafetyOrangePrimary,
                            indicatorColor = SafetyOrangeContainer,
                            unselectedIconColor = SlateGray,
                            unselectedTextColor = SlateGray
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun OrderStatusTimeline(
    currentStatus: String,
    history: List<StatusUpdate>,
    paymentStatus: String = "",
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        "PENDING" to "Order Placed",
        "CONFIRMED" to "Order Confirmed",
        "PACKING" to "Packing",
        "READY_FOR_DISPATCH" to "Ready for Dispatch",
        "OUT_FOR_DELIVERY" to "Out for Delivery",
        "DELIVERED" to "Delivered",
        "COMPLETED" to "Order Completed"
    )

    val currentStepIndex = when (currentStatus.uppercase()) {
        "PENDING" -> 0
        "CONFIRMED" -> 1
        "PACKING" -> 2
        "READY_FOR_DISPATCH" -> 3
        "OUT_FOR_DELIVERY" -> 4
        "DELIVERED" -> if (paymentStatus.uppercase() == "PAID") 6 else 5
        "COMPLETED", "ORDER_COMPLETED" -> 6
        else -> 0
    }

    val isCancelled = currentStatus.equals("CANCELLED", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Order Tracking Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = IndustrialDark
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isCancelled) {
                Surface(
                    color = StockRedBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancelled", tint = StockRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This order was Cancelled.",
                            color = StockRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                steps.forEachIndexed { index, step ->
                    val isPast = index <= currentStepIndex
                    val isCurrent = index == currentStepIndex
                    val statusRecord = history.find { it.status.equals(step.first, ignoreCase = true) }
                        ?: if (step.first == "COMPLETED" && (currentStatus.uppercase() == "COMPLETED" || (currentStatus.uppercase() == "DELIVERED" && paymentStatus.uppercase() == "PAID"))) {
                            history.find { it.status.equals("DELIVERED", ignoreCase = true) }
                        } else null

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Timeline spine & indicator
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCurrent && index == steps.size - 1 -> StockGreen
                                            isCurrent -> SafetyOrangePrimary
                                            isPast -> StockGreen
                                            else -> Color(0xFFE2E8F0)
                                        }
                                    )
                            ) {
                                if (isPast && (!isCurrent || index == steps.size - 1)) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }

                            if (index < steps.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(36.dp)
                                        .background(if (index < currentStepIndex) StockGreen else Color(0xFFE2E8F0))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Details
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = if (index < steps.size - 1) 16.dp else 0.dp)
                        ) {
                            Text(
                                text = step.second,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isPast) IndustrialDark else SlateGray,
                                fontSize = 14.sp
                            )
                            if (statusRecord != null) {
                                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                    .format(Date(statusRecord.timestamp))
                                Text(
                                    text = dateStr,
                                    fontSize = 11.sp,
                                    color = SlateGray
                                )
                                if (statusRecord.note.isNotBlank()) {
                                    Text(
                                        text = statusRecord.note,
                                        fontSize = 12.sp,
                                        color = IndustrialDark
                                    )
                                }
                            } else if (isCurrent) {
                                Text(
                                    text = "In Progress",
                                    fontSize = 11.sp,
                                    color = SafetyOrangePrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


