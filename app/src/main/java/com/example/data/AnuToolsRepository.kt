package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AnuToolsRepository(private val context: Context) {

    private val tag = "AnuToolsRepo"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    sealed class AuthState {
        object Initial : AuthState()
        object Loading : AuthState()
        data class Authenticated(val retailer: Retailer) : AuthState()
        object Unauthenticated : AuthState()
    }

    // Observables for UI
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(DefaultData.products)
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(DefaultData.categories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _businessSettings = MutableStateFlow(DefaultData.defaultSettings)
    val businessSettings: StateFlow<BusinessSettings> = _businessSettings.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _notifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    val notifications: StateFlow<List<InAppNotification>> = _notifications.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _currentRetailer = MutableStateFlow<Retailer?>(null)
    val currentRetailer: StateFlow<Retailer?> = _currentRetailer.asStateFlow()

    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private val _isProductsLoading = MutableStateFlow(true)
    val isProductsLoading: StateFlow<Boolean> = _isProductsLoading.asStateFlow()

    private val _productsError = MutableStateFlow<String?>(null)
    val productsError: StateFlow<String?> = _productsError.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private var ordersListener: ListenerRegistration? = null
    private var productsListener: ListenerRegistration? = null

    init {
        checkAndInitFirebase()
        // Add welcome demo notification
        _notifications.value = listOf(
            InAppNotification(
                id = "notif-welcome",
                title = "Welcome to Anu Tools",
                message = "Explore distributor rates on Angle Grinders, Drills, and Cutting Equipment directly from Anu Tools & Service Center.",
                read = false,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    fun checkAndInitFirebase() {
        try {
            val app: FirebaseApp? = if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseApp.getInstance()
            } else {
                FirebaseApp.initializeApp(context)
            }
            if (app != null) {
                val db = FirebaseFirestore.getInstance(app)
                firestore = db
                val projectId = app.options.projectId
                _isFirebaseConnected.value = true
                Log.d(tag, "Firebase initialized successfully with live Cloud Firestore. Project: $projectId, App: ${app.name}")

                // Immediately attach catalog and settings listeners so products load
                attachLiveFirestoreListeners()

                // Check persistent Firebase Auth session
                val auth = try {
                    FirebaseAuth.getInstance(app)
                } catch (e: Exception) {
                    Log.w(tag, "FirebaseAuth get instance: ${e.message}")
                    null
                }

                val currentUser = auth?.currentUser
                if (currentUser != null) {
                    val uid = currentUser.uid
                    Log.d(tag, "Active session detected for retailer UID: $uid (${currentUser.email})")
                    coroutineScope.launch {
                        loadRetailerProfile(uid, currentUser.email ?: "")
                    }
                } else {
                    Log.d(tag, "No active retailer session. Redirecting to Login.")
                    _currentRetailer.value = null
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                Log.w(tag, "FirebaseApp is not configured yet (no google-services.json).")
                _isFirebaseConnected.value = false
                _isProductsLoading.value = false
                _productsError.value = "FirebaseApp could not be initialized from google-services.json"
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            Log.e(tag, "Error initializing Firebase: ${e.message}")
            _isFirebaseConnected.value = false
            _isProductsLoading.value = false
            _productsError.value = "Firebase init error: ${e.message}"
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun parseRetailerDoc(doc: DocumentSnapshot, uid: String, fallbackEmail: String): Retailer {
        val now = System.currentTimeMillis()
        val gst = doc.getString("gstNumber")
            ?: doc.getString("gstin")
            ?: doc.getString("GSTIN")
            ?: ""
        val photo = doc.getString("profilePhotoUrl")
            ?: doc.getString("photoUrl")
            ?: doc.getString("avatarUrl")
            ?: ""
        return Retailer(
            retailerId = uid,
            userId = uid,
            name = doc.getString("name") ?: "",
            shopName = doc.getString("shopName") ?: "",
            phone = doc.getString("phone") ?: "",
            email = doc.getString("email") ?: fallbackEmail,
            gstNumber = gst,
            businessType = doc.getString("businessType") ?: "Hardware Store",
            address = doc.getString("address") ?: "",
            city = doc.getString("city") ?: "Surat",
            area = doc.getString("area") ?: "Vesu",
            state = doc.getString("state") ?: "Gujarat",
            pincode = doc.getString("pincode") ?: "395007",
            profilePhotoUrl = photo,
            status = if (doc.getBoolean("active") == false) "INACTIVE" else (doc.getString("status") ?: "ACTIVE"),
            createdAt = doc.getLong("createdAt") ?: now,
            updatedAt = doc.getLong("updatedAt") ?: now
        )
    }

    private suspend fun loadRetailerProfile(uid: String, fallbackEmail: String = "") {
        _authState.value = AuthState.Loading
        try {
            val doc = try {
                firestore?.collection("retailers")?.document(uid)?.get()?.await()
            } catch (fe: Exception) {
                Log.w(tag, "Firestore get retailer profile notice: ${fe.message}")
                null
            }
            val now = System.currentTimeMillis()
            val retailer = if (doc != null && doc.exists()) {
                parseRetailerDoc(doc, uid, fallbackEmail)
            } else {
                val newRet = Retailer(
                    retailerId = uid,
                    userId = uid,
                    name = "Partner Retailer",
                    shopName = "Retail Shop",
                    phone = "",
                    email = fallbackEmail,
                    gstNumber = "",
                    address = "",
                    city = "Surat",
                    area = "Vesu",
                    state = "Gujarat",
                    pincode = "395007",
                    profilePhotoUrl = "",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                )
                try {
                    firestore?.collection("retailers")?.document(uid)?.set(
                        mapOf(
                            "retailerId" to uid,
                            "userId" to uid,
                            "name" to newRet.name,
                            "shopName" to newRet.shopName,
                            "phone" to newRet.phone,
                            "email" to newRet.email,
                            "gstNumber" to "",
                            "gstin" to "",
                            "address" to newRet.address,
                            "city" to newRet.city,
                            "area" to newRet.area,
                            "state" to newRet.state,
                            "pincode" to newRet.pincode,
                            "profilePhotoUrl" to "",
                            "active" to true,
                            "status" to "ACTIVE",
                            "createdAt" to now,
                            "updatedAt" to now
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )?.await()
                } catch (fe: Exception) {
                    Log.w(tag, "Firestore create fallback retailer profile notice: ${fe.message}")
                }
                newRet
            }
            _currentRetailer.value = retailer
            _authState.value = AuthState.Authenticated(retailer)
            attachRetailerOrdersListener(uid)
            Log.d(tag, "Retailer session restored for: ${retailer.shopName} ($uid)")
        } catch (e: Exception) {
            Log.e(tag, "loadRetailerProfile failed: ${e.message}")
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        pass: String,
        name: String,
        shopName: String,
        phone: String,
        address: String
    ): Result<Retailer> = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseAuth.getInstance()
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("Sign up failed: User could not be created.")
            val uid = user.uid

            val now = System.currentTimeMillis()
            val docData = mapOf(
                "retailerId" to uid,
                "userId" to uid,
                "name" to name.trim(),
                "shopName" to shopName.trim(),
                "phone" to phone.trim(),
                "email" to email.trim(),
                "address" to address.trim(),
                "city" to "Surat",
                "area" to "Vesu",
                "state" to "Gujarat",
                "pincode" to "395007",
                "gstNumber" to "",
                "gstin" to "",
                "profilePhotoUrl" to "",
                "active" to true,
                "status" to "ACTIVE",
                "createdAt" to now,
                "updatedAt" to now
            )

            // Attempt to create: retailers/{FirebaseAuth.currentUser.uid} in Firestore
            try {
                firestore?.collection("retailers")?.document(uid)?.set(
                    docData,
                    com.google.firebase.firestore.SetOptions.merge()
                )?.await()
                Log.d(tag, "Retailer profile document created in Firestore: retailers/$uid")
            } catch (fe: Exception) {
                Log.w(tag, "Firestore retailers write notice: ${fe.message}. Proceeding with authenticated session.")
            }

            val retailer = Retailer(
                retailerId = uid,
                userId = uid,
                name = name.trim(),
                shopName = shopName.trim(),
                phone = phone.trim(),
                email = email.trim(),
                address = address.trim(),
                city = "Surat",
                area = "Vesu",
                state = "Gujarat",
                pincode = "395007",
                gstNumber = "",
                profilePhotoUrl = "",
                status = "ACTIVE",
                createdAt = now,
                updatedAt = now
            )

            _currentRetailer.value = retailer
            _authState.value = AuthState.Authenticated(retailer)
            attachRetailerOrdersListener(uid)
            Result.success(retailer)
        } catch (e: Exception) {
            Log.e(tag, "signUpWithEmail error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun loginWithEmail(
        email: String,
        pass: String
    ): Result<Retailer> = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseAuth.getInstance()
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("Login failed: User not found.")
            val uid = user.uid

            val now = System.currentTimeMillis()
            var loadedRetailer: Retailer? = null

            try {
                val doc = firestore?.collection("retailers")?.document(uid)?.get()?.await()
                if (doc != null && doc.exists()) {
                    loadedRetailer = parseRetailerDoc(doc, uid, email.trim())
                }
            } catch (fe: Exception) {
                Log.w(tag, "Firestore retailers read notice: ${fe.message}")
            }

            val retailer = loadedRetailer ?: Retailer(
                retailerId = uid,
                userId = uid,
                name = user.displayName ?: "Partner Retailer",
                shopName = "Retail Shop",
                phone = user.phoneNumber ?: "",
                email = email.trim(),
                address = "",
                city = "Surat",
                area = "Vesu",
                state = "Gujarat",
                pincode = "395007",
                gstNumber = "",
                profilePhotoUrl = "",
                status = "ACTIVE",
                createdAt = now,
                updatedAt = now
            )

            // Sync document if needed
            if (loadedRetailer == null) {
                try {
                    firestore?.collection("retailers")?.document(uid)?.set(
                        mapOf(
                            "retailerId" to uid,
                            "userId" to uid,
                            "name" to retailer.name,
                            "shopName" to retailer.shopName,
                            "phone" to retailer.phone,
                            "email" to retailer.email,
                            "address" to retailer.address,
                            "city" to retailer.city,
                            "area" to retailer.area,
                            "state" to retailer.state,
                            "pincode" to retailer.pincode,
                            "gstNumber" to "",
                            "gstin" to "",
                            "profilePhotoUrl" to "",
                            "active" to true,
                            "status" to "ACTIVE",
                            "createdAt" to now,
                            "updatedAt" to now
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )?.await()
                } catch (fe: Exception) {
                    Log.w(tag, "Firestore retailers init notice: ${fe.message}")
                }
            }

            _currentRetailer.value = retailer
            _authState.value = AuthState.Authenticated(retailer)
            attachRetailerOrdersListener(uid)
            Log.d(tag, "Retailer logged in: retailers/$uid")
            Result.success(retailer)
        } catch (e: Exception) {
            Log.e(tag, "loginWithEmail error: ${e.message}")
            Result.failure(e)
        }
    }

    fun retryLoadProducts() {
        checkAndInitFirebase()
    }

    private fun attachLiveFirestoreListeners() {
        val db = firestore
        if (db == null) {
            _isProductsLoading.value = false
            _productsError.value = "Firestore instance is null"
            Log.e(tag, "PRODUCT_QUERY_FAILURE: error=Firestore instance is null")
            return
        }

        // 1. Live Products listener: active == true
        try {
            Log.d(tag, "PRODUCT_QUERY_START")
            _isProductsLoading.value = true
            _productsError.value = null

            productsListener?.remove()
            productsListener = db.collection("products")
                .whereEqualTo("active", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "PRODUCT_QUERY_FAILURE: error=${error.message}")
                        _isProductsLoading.value = false
                        _productsError.value = error.message
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        Log.d(tag, "PRODUCT_QUERY_SUCCESS: count=${snapshot.size()}")
                        _isProductsLoading.value = false
                        _productsError.value = null

                        val liveList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val active = doc.safeBoolean("active", default = true)
                                if (!active) return@mapNotNull null

                                val id = doc.safeString("id").ifBlank { doc.id }
                                val name = doc.safeString("name", "title", "productName").ifBlank { doc.id }

                                val sku = doc.safeString("sku", "itemCode", "code").ifBlank { "AT-${doc.id.take(6).uppercase()}" }
                                val brandName = doc.safeString("brandName", "brand_name", "brand", "manufacturer")
                                val categoryName = doc.safeString("categoryName", "category_name", "catName", "category")
                                val categoryId = doc.safeString("categoryId", "category_id", "catId")

                                val matchedOfficial = DefaultData.OFFICIAL_CATEGORIES.firstOrNull { official ->
                                    official.name.equals(categoryName, ignoreCase = true) ||
                                    official.aliases.any { it.equals(categoryName, ignoreCase = true) }
                                }
                                val finalCategoryId = if (categoryId.isNotBlank()) categoryId else (matchedOfficial?.defaultId ?: "")

                                val sellingPrice = doc.safeDouble("sellingPrice", "selling_price")
                                val retailerPrice = doc.safeDouble("retailerPrice", "retailer_price")
                                val mrp = doc.safeDouble("mrp", "mrpPrice", default = if (sellingPrice > 0.0) sellingPrice else retailerPrice)
                                val price = if (retailerPrice > 0.0) retailerPrice else if (sellingPrice > 0.0) sellingPrice else doc.safeDouble("price", "rate", "cost", default = 0.0)

                                val stockQuantity = doc.safeInt("stockQuantity", "stock_quantity", "stock", "quantity", default = 10)
                                val stockStatus = if (stockQuantity > 0) "IN_STOCK" else "OUT_OF_STOCK"

                                val thumbnailUrl = doc.safeString("thumbnailUrl", "thumbnail_url", "imageUrl", "image_url", "image", "photoUrl")
                                val images = doc.safeImages("images", fallbackUrl = thumbnailUrl)
                                val finalThumbnail = if (thumbnailUrl.isNotBlank()) thumbnailUrl else images.firstOrNull() ?: ""
                                val finalImages = if (images.isNotEmpty()) images else if (finalThumbnail.isNotBlank()) listOf(finalThumbnail) else emptyList()

                                val shortDesc = doc.safeString("shortDescription", "short_description", "shortDesc", "subtitle")
                                val desc = doc.safeString("description", "desc", "details")
                                val unit = doc.safeString("unit", "uom").ifBlank { "Piece" }
                                val gst = doc.safeDouble("gstPercentage", "gst_percentage", "gst", default = 18.0)
                                val specs = doc.safeSpecifications("specifications")

                                Product(
                                    id = id,
                                    sku = sku,
                                    name = name,
                                    brandName = brandName,
                                    categoryId = finalCategoryId,
                                    categoryName = categoryName,
                                    shortDescription = shortDesc,
                                    description = desc,
                                    price = price,
                                    retailerPrice = retailerPrice,
                                    sellingPrice = sellingPrice,
                                    mrp = mrp,
                                    stockStatus = stockStatus,
                                    stockQuantity = stockQuantity,
                                    active = active,
                                    thumbnailUrl = finalThumbnail,
                                    images = finalImages,
                                    specifications = specs,
                                    gstPercentage = gst,
                                    unit = unit
                                )
                            } catch (e: Exception) {
                                Log.e(tag, "Error parsing product doc ${doc.id}: ${e.message}")
                                null
                            }
                        }

                        _products.value = liveList
                        _categories.value = DefaultData.buildCategoriesFromProducts(liveList)
                        Log.d(tag, "Loaded ${liveList.size} real products from Firestore")
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "PRODUCT_QUERY_FAILURE: error=${e.message}")
            _isProductsLoading.value = false
            _productsError.value = e.message
        }

        // 3. Business Settings
        try {
            db.collection("businessSettings").document("public")
                .addSnapshotListener { doc, error ->
                    if (error == null && doc != null && doc.exists()) {
                        _businessSettings.value = BusinessSettings(
                            businessName = doc.getString("businessName") ?: DefaultData.defaultSettings.businessName,
                            tagline = doc.getString("tagline") ?: DefaultData.defaultSettings.tagline,
                            phone = doc.getString("phone") ?: DefaultData.defaultSettings.phone,
                            whatsapp = doc.getString("whatsapp") ?: DefaultData.defaultSettings.whatsapp,
                            email = doc.getString("email") ?: DefaultData.defaultSettings.email,
                            address = doc.getString("address") ?: DefaultData.defaultSettings.address,
                            city = doc.getString("city") ?: DefaultData.defaultSettings.city,
                            area = doc.getString("area") ?: DefaultData.defaultSettings.area,
                            pincode = doc.getString("pincode") ?: DefaultData.defaultSettings.pincode,
                            mapsUrl = doc.getString("mapsUrl") ?: DefaultData.defaultSettings.mapsUrl,
                            upiId = doc.getString("upiId") ?: DefaultData.defaultSettings.upiId,
                            paymentQrUrl = doc.getString("paymentQrUrl") ?: DefaultData.defaultSettings.paymentQrUrl,
                            codEnabled = doc.getBoolean("codEnabled") ?: true,
                            qrPaymentEnabled = doc.getBoolean("qrPaymentEnabled") ?: true,
                            deliveryCharge = doc.getDouble("deliveryCharge") ?: DefaultData.defaultSettings.deliveryCharge,
                            freeDeliveryThreshold = doc.getDouble("freeDeliveryThreshold") ?: DefaultData.defaultSettings.freeDeliveryThreshold,
                            minimumOrderAmount = doc.getDouble("minimumOrderAmount") ?: DefaultData.defaultSettings.minimumOrderAmount,
                            supportMessage = doc.getString("supportMessage") ?: DefaultData.defaultSettings.supportMessage
                        )
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Settings listener: ${e.message}")
        }

        // 4. Attach orders for current retailer
        attachRetailerOrdersListener(_currentRetailer.value?.retailerId)
    }

    fun parseOrderDocument(doc: DocumentSnapshot): Order? {
        return try {
            val orderId = doc.id
            val orderNumber = doc.getString("orderNumber") ?: doc.safeString("orderNumber")
            val retId = doc.getString("retailerId") ?: doc.safeString("retailerId")
            @Suppress("UNCHECKED_CAST")
            val retSnap = doc.get("retailerSnapshot") as? Map<String, Any?> ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val rawItems = doc.get("items") as? List<Map<String, Any?>> ?: emptyList()
            val items = rawItems.map { itemMap ->
                OrderItemSnapshot(
                    productId = itemMap["productId"] as? String ?: "",
                    sku = itemMap["sku"] as? String ?: "",
                    name = itemMap["name"] as? String ?: "",
                    imageUrl = itemMap["imageUrl"] as? String ?: "",
                    quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1,
                    unit = itemMap["unit"] as? String ?: "Piece",
                    unitPrice = (itemMap["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                    mrp = (itemMap["mrp"] as? Number)?.toDouble() ?: 0.0,
                    gstPercentage = (itemMap["gstPercentage"] as? Number)?.toDouble() ?: 18.0,
                    gstAmount = (itemMap["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                    subtotal = (itemMap["subtotal"] as? Number)?.toDouble() ?: 0.0
                )
            }
            val subtotal = doc.getDouble("subtotal") ?: doc.safeDouble("subtotal")
            val discount = doc.getDouble("discount") ?: doc.safeDouble("discount")
            val gst = doc.getDouble("gst") ?: doc.safeDouble("gst")
            val deliveryCharge = doc.getDouble("deliveryCharge") ?: doc.safeDouble("deliveryCharge")
            val total = doc.getDouble("total") ?: doc.safeDouble("total")
            val paymentMethod = doc.getString("paymentMethod") ?: "COD"
            val paymentStatus = doc.getString("paymentStatus") ?: "UNPAID"
            val orderStatus = doc.getString("orderStatus") ?: "PENDING"
            @Suppress("UNCHECKED_CAST")
            val rawHistory = doc.get("statusHistory") as? List<Map<String, Any?>> ?: emptyList()
            val history = rawHistory.map { hMap ->
                StatusUpdate(
                    status = hMap["status"] as? String ?: "PENDING",
                    timestamp = (hMap["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    note = hMap["note"] as? String ?: ""
                )
            }
            val note = doc.getString("customerNote") ?: ""
            val topPaymentRef = doc.getString("paymentReference") ?: doc.getString("utr") ?: ""
            val topScreenshotUrl = doc.getString("paymentScreenshotUrl") ?: ""
            val topSubmittedAt = doc.getLong("paymentProofSubmittedAt") ?: 0L
            val topSubmittedBy = doc.getString("paymentProofSubmittedBy") ?: ""

            @Suppress("UNCHECKED_CAST")
            val pMap = doc.get("paymentProof") as? Map<String, Any?>
            val proof = if (pMap != null) {
                PaymentProof(
                    utr = (pMap["utr"] as? String)?.ifBlank { topPaymentRef } ?: topPaymentRef,
                    screenshotUrl = (pMap["screenshotUrl"] as? String)?.ifBlank { topScreenshotUrl } ?: topScreenshotUrl,
                    amountPaid = (pMap["amountPaid"] as? Number)?.toDouble() ?: total,
                    paymentNote = pMap["paymentNote"] as? String ?: "",
                    submittedAt = (pMap["submittedAt"] as? Number)?.toLong() ?: topSubmittedAt
                )
            } else if (topPaymentRef.isNotBlank() || topScreenshotUrl.isNotBlank()) {
                PaymentProof(
                    utr = topPaymentRef,
                    screenshotUrl = topScreenshotUrl,
                    amountPaid = total,
                    paymentNote = "",
                    submittedAt = topSubmittedAt
                )
            } else null
            val createdAt = doc.getLong("createdAt") ?: doc.safeDouble("createdAt").toLong().takeIf { it > 0 } ?: System.currentTimeMillis()
            val updatedAt = doc.getLong("updatedAt") ?: doc.safeDouble("updatedAt").toLong().takeIf { it > 0 } ?: System.currentTimeMillis()

            Order(
                orderId = orderId,
                orderNumber = orderNumber,
                retailerId = retId,
                retailerSnapshot = retSnap,
                items = items,
                subtotal = subtotal,
                discount = discount,
                gst = gst,
                deliveryCharge = deliveryCharge,
                total = total,
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                orderStatus = orderStatus,
                statusHistory = history,
                customerNote = note,
                paymentProof = proof,
                paymentReference = topPaymentRef.ifBlank { proof?.utr ?: "" },
                paymentScreenshotUrl = topScreenshotUrl.ifBlank { proof?.screenshotUrl ?: "" },
                paymentProofSubmittedAt = if (topSubmittedAt > 0L) topSubmittedAt else (proof?.submittedAt ?: 0L),
                paymentProofSubmittedBy = topSubmittedBy,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            Log.w(tag, "parseOrderDocument error: ${e.message}")
            null
        }
    }

    /**
     * Real-time Firestore snapshot listener for orders/{orderId}.
     * Listens to live updates from Admin app (orderStatus, paymentStatus, etc.).
     * Enforces retailerId == FirebaseAuth.currentUser.uid security rule.
     */
    fun getOrderFlow(orderId: String): Flow<Order?> = callbackFlow {
        val db = firestore
        if (db == null) {
            val cached = _orders.value.find { it.orderId == orderId }
            trySend(cached)
            awaitClose { }
            return@callbackFlow
        }

        val registration = db.collection("orders").document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Real-time snapshot listener error on order $orderId: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val order = parseOrderDocument(snapshot)
                    if (order != null) {
                        val currentAuthUid = FirebaseAuth.getInstance().currentUser?.uid
                        val loggedInRetId = _currentRetailer.value?.retailerId
                        val isAllowed = (currentAuthUid != null && order.retailerId == currentAuthUid) ||
                                        (loggedInRetId != null && order.retailerId == loggedInRetId)
                        if (isAllowed) {
                            trySend(order)
                            // Synchronize order into local cached list
                            val currentList = _orders.value.toMutableList()
                            val idx = currentList.indexOfFirst { it.orderId == order.orderId }
                            if (idx >= 0) {
                                currentList[idx] = order
                            } else {
                                currentList.add(0, order)
                            }
                            _orders.value = currentList.sortedByDescending { it.createdAt }
                        } else {
                            Log.w(tag, "Security violation: order ${order.orderId} retailerId (${order.retailerId}) does not match authenticated retailer ($currentAuthUid)")
                            trySend(null)
                        }
                    } else {
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    fun attachRetailerOrdersListener(retailerId: String?) {
        val db = firestore ?: return
        val currentAuthUid = FirebaseAuth.getInstance().currentUser?.uid
        val targetRetailerId = currentAuthUid ?: retailerId
        if (targetRetailerId.isNullOrEmpty()) return

        try {
            ordersListener?.remove()
            ordersListener = db.collection("orders")
                .whereEqualTo("retailerId", targetRetailerId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Orders snapshot notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val orderList = snapshot.documents.mapNotNull { doc ->
                            parseOrderDocument(doc)
                        }
                        _orders.value = orderList.sortedByDescending { it.createdAt }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to attach orders listener: ${e.message}")
        }
    }

    // ==========================================
    // CART OPERATIONS & TRUSTED REVALIDATION
    // ==========================================

    fun addToCart(product: Product, quantity: Int = 1) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            current.add(CartItem(product = product, quantity = quantity))
        }
        _cartItems.value = current
    }

    fun updateCartQuantity(productId: String, newQuantity: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (newQuantity <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = newQuantity)
            }
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: String) {
        val current = _cartItems.value.toMutableList()
        current.removeAll { it.product.id == productId }
        _cartItems.value = current
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    data class RevalidationResult(
        val isValid: Boolean,
        val updatedItems: List<CartItem>,
        val priceChanged: Boolean,
        val outOfStockItems: List<String>,
        val subtotal: Double,
        val gst: Double,
        val deliveryCharge: Double,
        val total: Double
    )

    /**
     * Critical Cart Rule:
     * Never trust price or availability from client storage.
     * Revalidate against latest trusted catalog data!
     */
    fun revalidateCart(): RevalidationResult {
        val currentCart = _cartItems.value
        val latestProducts = _products.value.associateBy { it.id }

        val validated = mutableListOf<CartItem>()
        var priceChanged = false
        val outOfStock = mutableListOf<String>()

        var subtotal = 0.0
        var totalGst = 0.0

        for (item in currentCart) {
            val liveProd = latestProducts[item.product.id]
            if (liveProd == null || !liveProd.active || liveProd.stockStatus == "OUT_OF_STOCK" || liveProd.stockQuantity <= 0) {
                outOfStock.add(item.product.name)
                continue
            }
            // Use effectivePrice (retailer price) — the actual price shown to the retailer
            val livePrice = liveProd.effectivePrice
            val oldPrice = item.product.effectivePrice
            if (livePrice != oldPrice) {
                priceChanged = true
            }
            // clamp quantity to available stock
            val clampedQty = item.quantity.coerceAtMost(liveProd.stockQuantity)
            val updatedItem = CartItem(product = liveProd, quantity = clampedQty)
            validated.add(updatedItem)

            val itemSubtotal = livePrice * clampedQty
            subtotal += itemSubtotal
            totalGst += itemSubtotal * (liveProd.gstPercentage / 100.0)
        }

        val settings = _businessSettings.value
        val deliveryCharge = if (subtotal >= settings.freeDeliveryThreshold || subtotal == 0.0) 0.0 else settings.deliveryCharge
        val grandTotal = subtotal + totalGst + deliveryCharge

        // Update active cart with validated items
        _cartItems.value = validated

        return RevalidationResult(
            isValid = outOfStock.isEmpty(),
            updatedItems = validated,
            priceChanged = priceChanged,
            outOfStockItems = outOfStock,
            subtotal = subtotal,
            gst = totalGst,
            deliveryCharge = deliveryCharge,
            total = grandTotal
        )
    }

    // ==========================================
    // ORDER PLACEMENT & FIRESTORE SYNC
    // ==========================================

    suspend fun placeOrder(
        paymentMethod: String, // "COD" or "QR"
        customerNote: String = "",
        deliveryAddress: Map<String, String>? = null,
        paymentProof: PaymentProof? = null
    ): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val retailer = _currentRetailer.value ?: return@withContext Result.failure(Exception("Please login as a retailer to place orders."))
            val revalidation = revalidateCart()
            if (revalidation.updatedItems.isEmpty()) {
                return@withContext Result.failure(Exception("Cart is empty or items are no longer available."))
            }

            val orderNumber = "ANU-${SimpleDateFormat("yyyy", Locale.US).format(Date())}-${(100000..999999).random()}"
            val orderId = "ord-${System.currentTimeMillis()}"

            // Build immutable item snapshots using effectivePrice (retailer price)
            val itemSnapshots = revalidation.updatedItems.map { item ->
                val p = item.product
                val unitPrice = p.effectivePrice
                val itemSub = unitPrice * item.quantity
                val itemGst = itemSub * (p.gstPercentage / 100.0)
                OrderItemSnapshot(
                    productId = p.id,
                    sku = p.sku,
                    name = p.name,
                    imageUrl = p.imageUrl,
                    quantity = item.quantity,
                    unit = p.unit,
                    unitPrice = unitPrice,
                    mrp = p.mrp,
                    gstPercentage = p.gstPercentage,
                    gstAmount = itemGst,
                    subtotal = itemSub
                )
            }

            val currentAuthUid = FirebaseAuth.getInstance().currentUser?.uid
            val effectiveRetailerId = currentAuthUid ?: retailer.retailerId

            val retSnapshot = mutableMapOf<String, Any?>(
                "retailerId" to effectiveRetailerId,
                "name" to retailer.name,
                "shopName" to retailer.shopName,
                "phone" to (deliveryAddress?.get("phone") ?: retailer.phone),
                "address" to (deliveryAddress?.get("address") ?: retailer.address),
                "city" to (deliveryAddress?.get("city") ?: retailer.city),
                "area" to (deliveryAddress?.get("area") ?: retailer.area),
                "state" to retailer.state,
                "pincode" to (deliveryAddress?.get("pincode") ?: retailer.pincode),
                "gstNumber" to retailer.gstNumber,
                "gstin" to retailer.gstNumber
            )

            val initialStatusHistory = listOf(
                StatusUpdate(
                    status = "PENDING",
                    timestamp = System.currentTimeMillis(),
                    note = "Order placed by retailer via mobile app"
                )
            )

            val paymentStatus = if (paymentMethod == "QR" && paymentProof != null) {
                "PAYMENT_PENDING_VERIFICATION"
            } else if (paymentMethod == "QR") {
                "PAYMENT_PENDING_VERIFICATION"
            } else {
                "UNPAID"
            }

            val paymentRef = paymentProof?.utr ?: ""
            val paymentScUrl = paymentProof?.screenshotUrl ?: ""
            val paymentSubAt = paymentProof?.submittedAt ?: if (paymentMethod == "QR") System.currentTimeMillis() else 0L
            val paymentSubBy = if (paymentProof != null || paymentMethod == "QR") effectiveRetailerId else ""

            val newOrder = Order(
                orderId = orderId,
                orderNumber = orderNumber,
                retailerId = effectiveRetailerId,
                retailerSnapshot = retSnapshot,
                items = itemSnapshots,
                subtotal = revalidation.subtotal,
                discount = 0.0,
                gst = revalidation.gst,
                deliveryCharge = revalidation.deliveryCharge,
                total = revalidation.total,
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                orderStatus = "PENDING",
                statusHistory = initialStatusHistory,
                customerNote = customerNote,
                paymentProof = paymentProof,
                paymentReference = paymentRef,
                paymentScreenshotUrl = paymentScUrl,
                paymentProofSubmittedAt = paymentSubAt,
                paymentProofSubmittedBy = paymentSubBy,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Save to Firestore if connected
            val db = firestore
            if (db != null) {
                val orderMap = hashMapOf(
                    "orderId" to newOrder.orderId,
                    "orderNumber" to newOrder.orderNumber,
                    "retailerId" to newOrder.retailerId,
                    "retailerSnapshot" to newOrder.retailerSnapshot,
                    "items" to newOrder.items.map {
                        hashMapOf(
                            "productId" to it.productId,
                            "sku" to it.sku,
                            "name" to it.name,
                            "imageUrl" to it.imageUrl,
                            "quantity" to it.quantity,
                            "unit" to it.unit,
                            "unitPrice" to it.unitPrice,
                            "mrp" to it.mrp,
                            "gstPercentage" to it.gstPercentage,
                            "gstAmount" to it.gstAmount,
                            "subtotal" to it.subtotal
                        )
                    },
                    "subtotal" to newOrder.subtotal,
                    "discount" to newOrder.discount,
                    "gst" to newOrder.gst,
                    "deliveryCharge" to newOrder.deliveryCharge,
                    "total" to newOrder.total,
                    "paymentMethod" to newOrder.paymentMethod,
                    "paymentStatus" to newOrder.paymentStatus,
                    "orderStatus" to newOrder.orderStatus,
                    "statusHistory" to newOrder.statusHistory.map {
                        hashMapOf("status" to it.status, "timestamp" to it.timestamp, "note" to it.note)
                    },
                    "customerNote" to newOrder.customerNote,
                    "paymentReference" to newOrder.paymentReference,
                    "paymentScreenshotUrl" to newOrder.paymentScreenshotUrl,
                    "paymentProofSubmittedAt" to newOrder.paymentProofSubmittedAt,
                    "paymentProofSubmittedBy" to newOrder.paymentProofSubmittedBy,
                    "createdAt" to newOrder.createdAt,
                    "updatedAt" to newOrder.updatedAt
                )

                if (newOrder.paymentProof != null) {
                    orderMap["paymentProof"] = hashMapOf(
                        "utr" to newOrder.paymentProof.utr,
                        "screenshotUrl" to newOrder.paymentProof.screenshotUrl,
                        "amountPaid" to newOrder.paymentProof.amountPaid,
                        "paymentNote" to newOrder.paymentProof.paymentNote,
                        "submittedAt" to newOrder.paymentProof.submittedAt
                    )
                }

                db.collection("orders").document(orderId).set(orderMap).await()
                Log.d(tag, "Order saved directly to Firestore: $orderId ($orderNumber)")
            }

            // Also keep in memory
            val currentOrders = _orders.value.toMutableList()
            currentOrders.add(0, newOrder)
            _orders.value = currentOrders

            // Clear cart
            clearCart()

            // Push notification
            val notif = InAppNotification(
                id = "notif-${System.currentTimeMillis()}",
                retailerId = retailer.retailerId,
                title = "Order Received ($orderNumber)",
                message = "Your order for ₹${"%,.2f".format(newOrder.total)} has been submitted to Anu Tools for verification & dispatch.",
                orderId = newOrder.orderId,
                orderNumber = newOrder.orderNumber,
                read = false,
                createdAt = System.currentTimeMillis()
            )
            val currentNotifs = _notifications.value.toMutableList()
            currentNotifs.add(0, notif)
            _notifications.value = currentNotifs

            Result.success(newOrder)
        } catch (e: Exception) {
            Log.e(tag, "Failed to place order: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun submitPaymentProof(
        orderId: String,
        utr: String,
        amount: Double,
        note: String,
        screenshotUrl: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                ?: _currentRetailer.value?.retailerId
                ?: ""

            val proof = PaymentProof(
                utr = utr.trim(),
                screenshotUrl = screenshotUrl.trim(),
                amountPaid = amount,
                paymentNote = note.trim(),
                submittedAt = now
            )

            // Update local order
            val ordersList = _orders.value.toMutableList()
            val index = ordersList.indexOfFirst { it.orderId == orderId }
            if (index >= 0) {
                val updated = ordersList[index].copy(
                    paymentProof = proof,
                    paymentReference = utr.trim(),
                    paymentScreenshotUrl = screenshotUrl.trim(),
                    paymentProofSubmittedAt = now,
                    paymentProofSubmittedBy = currentUid,
                    paymentStatus = "PAYMENT_PENDING_VERIFICATION",
                    updatedAt = now
                )
                ordersList[index] = updated
                _orders.value = ordersList
            }

            // Sync to Firestore
            val db = firestore
            if (db != null) {
                val updateData = mutableMapOf<String, Any?>(
                    "paymentReference" to utr.trim(),
                    "paymentProof" to mapOf(
                        "utr" to proof.utr,
                        "screenshotUrl" to proof.screenshotUrl,
                        "amountPaid" to proof.amountPaid,
                        "paymentNote" to proof.paymentNote,
                        "submittedAt" to proof.submittedAt
                    ),
                    "paymentProofSubmittedAt" to now,
                    "paymentStatus" to "PAYMENT_PENDING_VERIFICATION",
                    "updatedAt" to now
                )
                if (screenshotUrl.isNotBlank()) {
                    updateData["paymentScreenshotUrl"] = screenshotUrl.trim()
                }
                if (currentUid.isNotBlank()) {
                    updateData["paymentProofSubmittedBy"] = currentUid
                }
                db.collection("orders").document(orderId).update(updateData).await()
                Log.d(tag, "Payment proof synced to Firestore orders/$orderId: utr=$utr, screenshot=$screenshotUrl")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "submitPaymentProof error: ${e.message}")
            Result.failure(e)
        }
    }

    fun reorder(order: Order): Int {
        var addedCount = 0
        val liveProducts = _products.value.associateBy { it.id }

        for (item in order.items) {
            val liveProd = liveProducts[item.productId]
            if (liveProd != null && liveProd.active && liveProd.stockStatus != "OUT_OF_STOCK" && liveProd.stockQuantity > 0) {
                addToCart(liveProd, item.quantity.coerceAtMost(liveProd.stockQuantity))
                addedCount++
            }
        }
        return addedCount
    }

    fun markNotificationAsRead(id: String) {
        val list = _notifications.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0) {
            list[index] = list[index].copy(read = true)
            _notifications.value = list
        }
    }

    fun markAllNotificationsAsRead() {
        val list = _notifications.value.map { it.copy(read = true) }
        _notifications.value = list
    }

    suspend fun updateRetailerProfile(retailer: Retailer): Result<Unit> {
        _currentRetailer.value = retailer
        _authState.value = AuthState.Authenticated(retailer)
        attachRetailerOrdersListener(retailer.retailerId)

        return try {
            val docData = mapOf<String, Any?>(
                "retailerId" to retailer.retailerId,
                "userId" to retailer.userId,
                "name" to retailer.name,
                "shopName" to retailer.shopName,
                "phone" to retailer.phone,
                "email" to retailer.email,
                "gstNumber" to retailer.gstNumber,
                "gstin" to retailer.gstNumber, // Sync gstin field as well for admin compatibility
                "businessType" to retailer.businessType,
                "address" to retailer.address,
                "city" to retailer.city,
                "area" to retailer.area,
                "state" to retailer.state,
                "pincode" to retailer.pincode,
                "profilePhotoUrl" to retailer.profilePhotoUrl,
                "status" to retailer.status,
                "active" to (retailer.status != "INACTIVE"),
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("retailers")?.document(retailer.retailerId)?.set(
                docData,
                com.google.firebase.firestore.SetOptions.merge()
            )?.await()
            Log.d(tag, "Profile synced to Firestore retailers/${retailer.retailerId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(tag, "Profile sync: ${e.message}")
            Result.failure(e)
        }
    }

    fun setLoggedInRetailer(retailer: Retailer) {
        val currentUid = try { FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null }
        val effectiveUid = currentUid ?: retailer.retailerId
        val mergedRetailer = retailer.copy(
            retailerId = effectiveUid,
            userId = effectiveUid
        )
        _currentRetailer.value = mergedRetailer
        _authState.value = AuthState.Authenticated(mergedRetailer)
        Log.d(tag, "setLoggedInRetailer: retailerId=$effectiveUid")
        attachRetailerOrdersListener(effectiveUid)
    }

    fun logout() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.w(tag, "FirebaseAuth sign out error: ${e.message}")
        }
        ordersListener?.remove()
        _currentRetailer.value = null
        _orders.value = emptyList()
        _cartItems.value = emptyList()
        _authState.value = AuthState.Unauthenticated
        Log.d(tag, "Retailer logged out successfully")
    }

    /**
     * Seeds the real Cloud Firestore database with Anu Tools products, categories,
     * and business settings with one tap!
     * Crucial for presentation when connecting to a fresh Firestore backend.
     */
    suspend fun seedFirestoreDatabase(): Result<String> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(Exception("Firestore is not connected yet. Please ensure google-services.json is configured in the project."))
        try {
            // 1. Business Settings
            val settings = DefaultData.defaultSettings
            db.collection("businessSettings").document("public").set(
                mapOf<String, Any?>(
                    "businessName" to settings.businessName,
                    "tagline" to settings.tagline,
                    "phone" to settings.phone,
                    "whatsapp" to settings.whatsapp,
                    "email" to settings.email,
                    "address" to settings.address,
                    "city" to settings.city,
                    "area" to settings.area,
                    "pincode" to settings.pincode,
                    "mapsUrl" to settings.mapsUrl,
                    "upiId" to settings.upiId,
                    "paymentQrUrl" to settings.paymentQrUrl,
                    "codEnabled" to settings.codEnabled,
                    "qrPaymentEnabled" to settings.qrPaymentEnabled,
                    "deliveryCharge" to settings.deliveryCharge,
                    "freeDeliveryThreshold" to settings.freeDeliveryThreshold,
                    "minimumOrderAmount" to settings.minimumOrderAmount,
                    "supportMessage" to settings.supportMessage
                )
            ).await()

            // 2. Categories
            for (cat in DefaultData.categories) {
                db.collection("categories").document(cat.id).set(
                    mapOf<String, Any?>(
                        "name" to cat.name,
                        "imageUrl" to cat.imageUrl,
                        "productCount" to cat.productCount,
                        "active" to cat.active,
                        "sortOrder" to cat.sortOrder
                    )
                ).await()
            }

            // 3. Products: Never overwrite live products from Admin App
            // Real products uploaded from Admin App remain untouched.

            // 4. Demo Retailer
            val ret = DefaultData.demoRetailer
            db.collection("retailers").document(ret.retailerId).set(
                mapOf<String, Any?>(
                    "retailerId" to ret.retailerId,
                    "userId" to ret.userId,
                    "name" to ret.name,
                    "shopName" to ret.shopName,
                    "phone" to ret.phone,
                    "email" to ret.email,
                    "gstNumber" to ret.gstNumber,
                    "businessType" to ret.businessType,
                    "address" to ret.address,
                    "city" to ret.city,
                    "area" to ret.area,
                    "pincode" to ret.pincode,
                    "status" to ret.status,
                    "createdAt" to ret.createdAt,
                    "updatedAt" to ret.updatedAt
                )
            ).await()

            Result.success("Firestore populated with ${DefaultData.products.size} products, ${DefaultData.categories.size} categories, and business settings!")
        } catch (e: Exception) {
            Log.e(tag, "Seeding failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}

// Resilient parsing helpers for Firestore DocumentSnapshot to handle any schema variations or type conversions
private fun DocumentSnapshot.safeString(field: String, vararg fallbackFields: String): String {
    val primary = get(field)?.toString()?.trim()
    if (!primary.isNullOrBlank()) return primary
    for (fb in fallbackFields) {
        val candidate = get(fb)?.toString()?.trim()
        if (!candidate.isNullOrBlank()) return candidate
    }
    return ""
}

private fun DocumentSnapshot.safeDouble(field: String, vararg fallbackFields: String, default: Double = 0.0): Double {
    val fields = listOf(field) + fallbackFields
    for (f in fields) {
        val v = get(f) ?: continue
        when (v) {
            is Number -> return v.toDouble()
            is String -> {
                val parsed = v.trim().toDoubleOrNull()
                if (parsed != null) return parsed
            }
        }
    }
    return default
}

private fun DocumentSnapshot.safeInt(field: String, vararg fallbackFields: String, default: Int = 0): Int {
    val fields = listOf(field) + fallbackFields
    for (f in fields) {
        val v = get(f) ?: continue
        when (v) {
            is Number -> return v.toInt()
            is String -> {
                val parsed = v.trim().toIntOrNull()
                if (parsed != null) return parsed
            }
        }
    }
    return default
}

private fun DocumentSnapshot.safeBoolean(field: String, default: Boolean = true): Boolean {
    val v = get(field) ?: return default
    return when (v) {
        is Boolean -> v
        is Number -> v.toInt() != 0
        is String -> {
            val lower = v.trim().lowercase()
            lower == "true" || lower == "1" || lower == "yes"
        }
        else -> default
    }
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.safeImages(field: String = "images", fallbackUrl: String = ""): List<String> {
    val rawList = get(field) as? List<*>
    val list = rawList?.mapNotNull { it?.toString()?.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    if (list.isNotEmpty()) return list
    if (fallbackUrl.isNotBlank()) return listOf(fallbackUrl)
    return emptyList()
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.safeSpecifications(field: String = "specifications"): Map<String, String> {
    val rawMap = get(field) as? Map<*, *> ?: return emptyMap()
    val clean = mutableMapOf<String, String>()
    for ((k, v) in rawMap) {
        if (k != null && v != null) {
            clean[k.toString()] = v.toString()
        }
    }
    return clean
}

