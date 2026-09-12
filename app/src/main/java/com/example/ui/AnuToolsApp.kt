package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AnuToolsRepository
import com.example.model.*
import com.example.ui.components.AnuBottomNavigationBar
import com.example.ui.components.AnuTopBar
import com.example.ui.components.NavigationTab
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object Categories : Screen()
    data class CategoryProducts(val category: Category) : Screen()
    object Search : Screen()
    data class ProductDetail(val product: Product) : Screen()
    object Cart : Screen()
    data class Checkout(val revalidation: AnuToolsRepository.RevalidationResult) : Screen()
    data class OrderConfirmation(val order: Order) : Screen()
    object Orders : Screen()
    data class OrderDetail(val orderId: String, val initialOrder: Order? = null) : Screen()
    object Notifications : Screen()
    object Profile : Screen()
    object EditProfile : Screen()
    object Contact : Screen()
    object Login : Screen()
}

@Composable
fun AnuToolsApp(
    repository: AnuToolsRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // States from Repository
    val authState by repository.authState.collectAsState()
    val products by repository.products.collectAsState()
    val categories by repository.categories.collectAsState()
    val isProductsLoading by repository.isProductsLoading.collectAsState()
    val productsError by repository.productsError.collectAsState()
    val businessSettings by repository.businessSettings.collectAsState()
    val orders by repository.orders.collectAsState()
    val cartItems by repository.cartItems.collectAsState()
    val notifications by repository.notifications.collectAsState()
    val currentRetailer by repository.currentRetailer.collectAsState()
    val isFirebaseConnected by repository.isFirebaseConnected.collectAsState()

    // Navigation Stack
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var selectedTab by remember { mutableStateOf(NavigationTab.HOME) }

    // Fast-dismiss "Added to Cart" popup (1 - 1.5s automatic duration)
    var cartPopupJob by remember { mutableStateOf<Job?>(null) }
    val showCartPopup: (String) -> Unit = { message ->
        cartPopupJob?.cancel()
        cartPopupJob = coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val job = launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Indefinite
                )
            }
            delay(1200L) // 1.2 seconds auto-dismiss
            snackbarHostState.currentSnackbarData?.dismiss()
            job.cancel()
        }
    }

    // Synchronize currentScreen with auth state
    LaunchedEffect(authState) {
        when (authState) {
            is AnuToolsRepository.AuthState.Authenticated -> {
                if (currentScreen is Screen.Login) {
                    currentScreen = Screen.Home
                    selectedTab = NavigationTab.HOME
                }
            }
            is AnuToolsRepository.AuthState.Unauthenticated -> {
                currentScreen = Screen.Login
            }
            else -> {}
        }
    }

    // Branded Splash/Loading while checking persisted session
    if (authState is AnuToolsRepository.AuthState.Initial || authState is AnuToolsRepository.AuthState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IndustrialCharcoal),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
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
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator(
                    color = SafetyOrangePrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        return
    }

    // Back handling
    BackHandler(enabled = currentScreen !is Screen.Home && currentScreen !is Screen.Login) {
        currentScreen = when (currentScreen) {
            is Screen.CategoryProducts -> Screen.Categories
            is Screen.ProductDetail -> Screen.Home
            is Screen.Search -> Screen.Home
            is Screen.Checkout -> Screen.Cart
            is Screen.OrderConfirmation -> Screen.Orders
            is Screen.OrderDetail -> Screen.Orders
            is Screen.EditProfile -> Screen.Profile
            is Screen.Contact -> Screen.Home
            is Screen.Notifications -> Screen.Home
            else -> {
                selectedTab = NavigationTab.HOME
                Screen.Home
            }
        }
    }

    val cartCount = cartItems.sumOf { it.quantity }
    val unreadNotifs = notifications.count { !it.read }
    val activeOrdersCount = orders.count {
        it.orderStatus in listOf("PENDING", "CONFIRMED", "PACKING", "READY_FOR_DISPATCH", "OUT_FOR_DELIVERY")
    }

    Scaffold(
        topBar = {
            // Show top bar for main tab screens
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    AnuTopBar(
                        title = "Anu Tools & Service",
                        cartItemCount = cartCount,
                        onCartClick = {
                            selectedTab = NavigationTab.CART
                            currentScreen = Screen.Cart
                        },
                        unreadNotificationCount = unreadNotifs,
                        onNotificationClick = { currentScreen = Screen.Notifications },
                        onSearchClick = { currentScreen = Screen.Search }
                    )
                }
                is Screen.Categories -> {
                    AnuTopBar(
                        title = "All Categories",
                        subtitle = "${categories.size} Machinery & Spares Divisions",
                        cartItemCount = cartCount,
                        onCartClick = {
                            selectedTab = NavigationTab.CART
                            currentScreen = Screen.Cart
                        },
                        unreadNotificationCount = unreadNotifs,
                        onNotificationClick = { currentScreen = Screen.Notifications },
                        onSearchClick = { currentScreen = Screen.Search }
                    )
                }
                is Screen.Cart -> {
                    AnuTopBar(
                        title = "Purchasing Cart",
                        subtitle = "$cartCount items added",
                        cartItemCount = cartCount,
                        onCartClick = {},
                        unreadNotificationCount = unreadNotifs,
                        onNotificationClick = { currentScreen = Screen.Notifications }
                    )
                }
                is Screen.Orders -> {
                    AnuTopBar(
                        title = "Order History",
                        subtitle = "${orders.size} Total Invoices & Orders",
                        cartItemCount = cartCount,
                        onCartClick = {
                            selectedTab = NavigationTab.CART
                            currentScreen = Screen.Cart
                        },
                        unreadNotificationCount = unreadNotifs,
                        onNotificationClick = { currentScreen = Screen.Notifications }
                    )
                }
                is Screen.Profile -> {
                    AnuTopBar(
                        title = "Retailer Account",
                        subtitle = currentRetailer?.shopName ?: "Shop Profile",
                        cartItemCount = cartCount,
                        onCartClick = {
                            selectedTab = NavigationTab.CART
                            currentScreen = Screen.Cart
                        },
                        unreadNotificationCount = unreadNotifs,
                        onNotificationClick = { currentScreen = Screen.Notifications }
                    )
                }
                is Screen.CategoryProducts -> {
                    AnuTopBar(
                        title = screen.category.name,
                        showBack = true,
                        onBack = { currentScreen = Screen.Categories },
                        cartItemCount = cartCount,
                        onCartClick = {
                            selectedTab = NavigationTab.CART
                            currentScreen = Screen.Cart
                        },
                        unreadNotificationCount = unreadNotifs,
                        onNotificationClick = { currentScreen = Screen.Notifications }
                    )
                }
                else -> {
                    // Custom sub-screen headers handled inside individual screens
                }
            }
        },
        bottomBar = {
            // Show bottom navigation bar on primary tabs
            if (currentScreen in listOf(Screen.Home, Screen.Categories, Screen.Cart, Screen.Orders, Screen.Profile)) {
                AnuBottomNavigationBar(
                    currentTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        currentScreen = when (tab) {
                            NavigationTab.HOME -> Screen.Home
                            NavigationTab.CATEGORIES -> Screen.Categories
                            NavigationTab.CART -> Screen.Cart
                            NavigationTab.ORDERS -> Screen.Orders
                            NavigationTab.PROFILE -> Screen.Profile
                        }
                    },
                    cartItemCount = cartCount,
                    activeOrdersCount = activeOrdersCount
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        retailer = currentRetailer,
                        products = products,
                        categories = categories,
                        businessSettings = businessSettings,
                        isLoading = isProductsLoading,
                        errorMessage = productsError,
                        onRetry = { repository.retryLoadProducts() },
                        onProductClick = { product -> currentScreen = Screen.ProductDetail(product) },
                        onCategoryClick = { cat -> currentScreen = Screen.CategoryProducts(cat) },
                        onAddToCart = { prod ->
                            repository.addToCart(prod, 1)
                            showCartPopup("Added ${prod.name} to Cart")
                        },
                        onSearchClick = { currentScreen = Screen.Search },
                        onBrowseAllClick = {
                            selectedTab = NavigationTab.CATEGORIES
                            currentScreen = Screen.Categories
                        },
                        onContactClick = { currentScreen = Screen.Contact }
                    )
                }

                is Screen.Categories -> {
                    CategoriesScreen(
                        categories = categories,
                        onCategoryClick = { cat -> currentScreen = Screen.CategoryProducts(cat) }
                    )
                }

                is Screen.CategoryProducts -> {
                    CategoryProductsScreen(
                        category = screen.category,
                        products = products,
                        onProductClick = { prod -> currentScreen = Screen.ProductDetail(prod) },
                        onAddToCart = { prod ->
                            repository.addToCart(prod, 1)
                            showCartPopup("Added ${prod.name} to Cart")
                        },
                        onBack = { currentScreen = Screen.Categories }
                    )
                }

                is Screen.Search -> {
                    SearchScreen(
                        products = products,
                        onProductClick = { prod -> currentScreen = Screen.ProductDetail(prod) },
                        onAddToCart = { prod ->
                            repository.addToCart(prod, 1)
                            showCartPopup("Added to Cart: ${prod.name}")
                        },
                        onBack = { currentScreen = Screen.Home }
                    )
                }

                is Screen.ProductDetail -> {
                    ProductDetailScreen(
                        product = screen.product,
                        onAddToCart = { prod, qty ->
                            repository.addToCart(prod, qty)
                            showCartPopup("Added $qty × ${prod.name} to Cart")
                        },
                        onBack = {
                            currentScreen = Screen.Home
                        },
                        onContactSupport = { currentScreen = Screen.Contact }
                    )
                }

                is Screen.Cart -> {
                    CartScreen(
                        cartItems = cartItems,
                        businessSettings = businessSettings,
                        onQuantityChange = { id, qty -> repository.updateCartQuantity(id, qty) },
                        onRemoveItem = { id -> repository.removeFromCart(id) },
                        onProceedToCheckout = {
                            val reval = repository.revalidateCart()
                            if (reval.outOfStockItems.isNotEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Notice: ${reval.outOfStockItems.joinToString(", ")} is currently unavailable and was adjusted.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            currentScreen = Screen.Checkout(reval)
                        },
                        onBrowseProducts = {
                            selectedTab = NavigationTab.HOME
                            currentScreen = Screen.Home
                        }
                    )
                }

                is Screen.Checkout -> {
                    val ret = currentRetailer
                    if (ret == null) {
                        Toast.makeText(context, "Please login to proceed to checkout", Toast.LENGTH_LONG).show()
                        currentScreen = Screen.Login
                    } else {
                        CheckoutScreen(
                            retailer = ret,
                            revalidation = screen.revalidation,
                            businessSettings = businessSettings,
                            repository = repository,
                            onOrderPlaced = { order ->
                                currentScreen = Screen.OrderConfirmation(order)
                            },
                            onBack = {
                                currentScreen = Screen.Cart
                            }
                        )
                    }
                }

                is Screen.OrderConfirmation -> {
                    OrderConfirmationScreen(
                        order = screen.order,
                        onTrackOrder = { order -> currentScreen = Screen.OrderDetail(order.orderId, order) },
                        onContinueShopping = {
                            selectedTab = NavigationTab.HOME
                            currentScreen = Screen.Home
                        }
                    )
                }

                is Screen.Orders -> {
                    OrdersScreen(
                        orders = orders,
                        onOrderClick = { order -> currentScreen = Screen.OrderDetail(order.orderId, order) },
                        onBrowseCatalogue = {
                            selectedTab = NavigationTab.HOME
                            currentScreen = Screen.Home
                        }
                    )
                }

                is Screen.OrderDetail -> {
                    OrderDetailScreen(
                        orderId = screen.orderId,
                        initialOrder = screen.initialOrder,
                        repository = repository,
                        onBack = {
                            selectedTab = NavigationTab.ORDERS
                            currentScreen = Screen.Orders
                        },
                        onReorder = { ord ->
                            val count = repository.reorder(ord)
                            if (count > 0) {
                                selectedTab = NavigationTab.CART
                                currentScreen = Screen.Cart
                                Toast.makeText(context, "Added $count available items to cart!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Items from this order are currently out of stock", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSubmitProof = { orderId, utr, amount, note, screenshotUrl ->
                            coroutineScope.launch {
                                val res = repository.submitPaymentProof(orderId, utr, amount, note, screenshotUrl)
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Payment verification proof submitted to Anu Tools!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to submit proof: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onContactSupport = { currentScreen = Screen.Contact }
                    )
                }

                is Screen.Notifications -> {
                    NotificationsScreen(
                        notifications = notifications,
                        onNotificationClick = { notif ->
                            repository.markNotificationAsRead(notif.id)
                            if (!notif.orderId.isNullOrEmpty()) {
                                val order = orders.find { it.orderId == notif.orderId }
                                currentScreen = Screen.OrderDetail(notif.orderId, order)
                            }
                        },
                        onMarkAllRead = { repository.markAllNotificationsAsRead() },
                        onBack = { currentScreen = Screen.Home }
                    )
                }

                is Screen.Profile -> {
                    val ret = currentRetailer
                    if (ret == null) {
                        currentScreen = Screen.Login
                    } else {
                        ProfileScreen(
                            retailer = ret,
                            onEditProfile = { currentScreen = Screen.EditProfile },
                            onContactSupport = { currentScreen = Screen.Contact },
                            onLogout = {
                                repository.logout()
                                currentScreen = Screen.Login
                            }
                        )
                    }
                }

                is Screen.EditProfile -> {
                    val ret = currentRetailer
                    if (ret == null) {
                        currentScreen = Screen.Login
                    } else {
                        EditProfileScreen(
                            retailer = ret,
                            onSave = { updated ->
                                coroutineScope.launch {
                                    val res = repository.updateRetailerProfile(updated)
                                    if (res.isSuccess) {
                                        currentScreen = Screen.Profile
                                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to update profile: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onBack = { currentScreen = Screen.Profile }
                        )
                    }
                }

                is Screen.Contact -> {
                    ContactScreen(
                        settings = businessSettings,
                        onBack = { currentScreen = Screen.Home }
                    )
                }

                is Screen.Login -> {
                    LoginScreen(
                        repository = repository,
                        onLoginSuccess = { ret ->
                            selectedTab = NavigationTab.HOME
                            currentScreen = Screen.Home
                        }
                    )
                }
            }
        }
    }
}
