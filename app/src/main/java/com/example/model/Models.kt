package com.example.model

data class Product(
    val id: String = "",
    val sku: String = "",
    val name: String = "",
    val brandName: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val shortDescription: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val retailerPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val mrp: Double = 0.0,
    val stockStatus: String = "IN_STOCK", // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
    val stockQuantity: Int = 10,
    val active: Boolean = true,
    val thumbnailUrl: String = "",
    val images: List<String> = emptyList(),
    val specifications: Map<String, String> = emptyMap(),
    val gstPercentage: Double = 18.0,
    val unit: String = "Piece"
) {
    val brand: String
        get() = brandName

    val imageUrl: String
        get() = thumbnailUrl

    val effectivePrice: Double
        get() = when {
            retailerPrice > 0.0 -> retailerPrice
            price > 0.0 -> price
            sellingPrice > 0.0 -> sellingPrice
            else -> 0.0
        }

    val isAvailable: Boolean
        get() = active && stockStatus != "OUT_OF_STOCK" && stockQuantity > 0

    val discountPercent: Int
        get() {
            val basePrice = effectivePrice
            return if (mrp > basePrice && mrp > 0) (((mrp - basePrice) / mrp) * 100).toInt() else 0
        }
}

data class Category(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val productCount: Int = 0,
    val active: Boolean = true,
    val sortOrder: Int = 0
)

data class Brand(
    val id: String = "",
    val name: String = "",
    val logoUrl: String = ""
)

data class Retailer(
    val retailerId: String = "",
    val userId: String = "",
    val name: String = "",
    val shopName: String = "",
    val phone: String = "",
    val email: String = "",
    val gstNumber: String = "",
    val businessType: String = "Hardware Store", // Hardware Store, Tool Shop, Construction Supplier, Workshop, Other
    val address: String = "",
    val city: String = "",
    val area: String = "",
    val state: String = "Gujarat",
    val pincode: String = "",
    val profilePhotoUrl: String = "",
    val status: String = "ACTIVE", // ACTIVE, PENDING, BLOCKED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class OrderItemSnapshot(
    val productId: String = "",
    val sku: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val quantity: Int = 1,
    val unit: String = "Piece",
    val unitPrice: Double = 0.0,
    val mrp: Double = 0.0,
    val gstPercentage: Double = 18.0,
    val gstAmount: Double = 0.0,
    val subtotal: Double = 0.0
)

data class StatusUpdate(
    val status: String = "PENDING",
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

data class PaymentProof(
    val utr: String = "",
    val screenshotUrl: String = "",
    val amountPaid: Double = 0.0,
    val paymentNote: String = "",
    val submittedAt: Long = System.currentTimeMillis()
)

data class Order(
    val orderId: String = "",
    val orderNumber: String = "",
    val retailerId: String = "",
    val retailerSnapshot: Map<String, Any?> = emptyMap(),
    val items: List<OrderItemSnapshot> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val gst: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val total: Double = 0.0,
    val paymentMethod: String = "COD", // COD, QR
    val paymentStatus: String = "UNPAID", // UNPAID, PAYMENT_PENDING_VERIFICATION, PAID, PARTIALLY_PAID, REFUNDED
    val orderStatus: String = "PENDING", // PENDING, CONFIRMED, PACKING, READY_FOR_DISPATCH, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    val statusHistory: List<StatusUpdate> = emptyList(),
    val customerNote: String = "",
    val paymentProof: PaymentProof? = null,
    val paymentReference: String = "",
    val paymentScreenshotUrl: String = "",
    val paymentProofSubmittedAt: Long = 0L,
    val paymentProofSubmittedBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BusinessSettings(
    val businessName: String = "Anu Tools & Service Center",
    val tagline: String = "Tools, Machinery & Authorized Service Hub",
    val phone: String = "+91 97237 55288",
    val whatsapp: String = "+919723755288",
    val email: String = "sales@anutools.com",
    val address: String = "Vesu Char Rasta, Opp. Reliance Market, Vesu",
    val city: String = "Surat",
    val area: String = "Vesu",
    val pincode: String = "395007",
    val mapsUrl: String = "https://maps.app.goo.gl/ermyxbBhyHPNej2m6",
    val upiId: String = "9723755288@icici",
    val paymentQrUrl: String = "",
    val codEnabled: Boolean = true,
    val qrPaymentEnabled: Boolean = true,
    val deliveryCharge: Double = 150.0,
    val freeDeliveryThreshold: Double = 10000.0,
    val minimumOrderAmount: Double = 1000.0,
    val supportMessage: String = "Authorized power tools distributor & repair center in Vesu, Surat. Mon-Sat 10:00 AM - 7:00 PM.",
    val cloudinaryCloudName: String = "jg8dnjho",
    val cloudinaryUploadPreset: String = "anu_tools_preset"
)

data class InAppNotification(
    val id: String = "",
    val retailerId: String = "",
    val title: String = "",
    val message: String = "",
    val orderId: String? = null,
    val orderNumber: String? = null,
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    val subtotal: Double
        get() = product.effectivePrice * quantity
}
