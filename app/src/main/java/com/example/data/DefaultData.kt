package com.example.data

import com.example.model.*

object DefaultData {

    val defaultSettings = BusinessSettings(
        businessName = "Anu Tools & Service Center",
        tagline = "Tools, Machinery & Authorized Service Hub",
        phone = "+91 97237 55288",
        whatsapp = "+919723755288",
        email = "sales@anutools.com",
        address = "Vesu Char Rasta, Opp. Reliance Market, Vesu",
        city = "Surat",
        area = "Vesu",
        pincode = "395007",
        mapsUrl = "https://maps.app.goo.gl/ermyxbBhyHPNej2m6",
        upiId = "9723755288@icici",
        paymentQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=upi://pay?pa=9723755288@icici%26pn=Anu%20Tools%20and%20Service%20Center%26cu=INR",
        codEnabled = true,
        qrPaymentEnabled = true,
        deliveryCharge = 150.0,
        freeDeliveryThreshold = 8000.0,
        minimumOrderAmount = 1000.0,
        supportMessage = "Authorized power tools distributor & repair center. Located at Vesu Char Rasta, Opp. Reliance Market, Surat. Mon-Sat 10:00 AM - 7:00 PM."
    )

    data class OfficialCategory(
        val name: String,
        val defaultId: String,
        val defaultImageUrl: String,
        val aliases: List<String>
    )

    // The ONLY 10 official categories as specified by the business
    val OFFICIAL_CATEGORIES = listOf(
        OfficialCategory(
            name = "Grinder Machine",
            defaultId = "cat_grinder",
            defaultImageUrl = "https://images.unsplash.com/photo-1504148455328-c376907d081c?w=600&auto=format&fit=crop&q=80",
            aliases = listOf("grinder machine", "grinder machines", "grinder", "grinders", "cat_grinder", "cat_grinders", "angle grinder")
        ),
        OfficialCategory(
            name = "Cutting Machine",
            defaultId = "cat_cutting",
            defaultImageUrl = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=600&auto=format&fit=crop&q=80",
            aliases = listOf("cutting machine", "cutting machines", "cutter", "cutters", "cat_cutting", "cat_cutters", "marble cutter", "cut off machine")
        ),
        OfficialCategory(
            name = "Drill Machine",
            defaultId = "cat_drill",
            defaultImageUrl = "https://images.unsplash.com/photo-1572981779307-38b8cabb2407?w=600&auto=format&fit=crop&q=80",
            aliases = listOf("drill machine", "drill machines", "drill", "drills", "cat_drill", "cat_drills", "rotary hammer", "impact drill")
        ),
        OfficialCategory(
            name = "Welding Machine",
            defaultId = "cat_welding",
            defaultImageUrl = "https://images.unsplash.com/photo-1504917599217-d4dc5ebe6122?w=600&auto=format&fit=crop&q=80",
            aliases = listOf("welding machine", "welding machines", "welder", "welders", "cat_welding", "cat_welders", "arc welding", "inverter welding")
        ),
        OfficialCategory(
            name = "Power Tools",
            defaultId = "cat_power_tools",
            defaultImageUrl = "https://images.unsplash.com/photo-1530124566582-a618bc2615dc?w=600&auto=format&fit=crop&q=80",
            aliases = listOf("power tools", "power tool", "cat_power_tools", "cat_powertools", "powertools")
        ),
        OfficialCategory(
            name = "Hand Tools",
            defaultId = "cat_hand_tools",
            defaultImageUrl = "https://images.unsplash.com/photo-1581092335397-9583fe92d232?w=600&auto=format&fit=crop&q=80",
            aliases = listOf("hand tools", "hand tool", "cat_hand_tools", "cat_handtools", "handtools")
        ),
        OfficialCategory(
            name = "Cutting and Grinding Disk",
            defaultId = "cat_cutting_disk",
            defaultImageUrl = "https://images.unsplash.com/photo-1504148455328-c376907d081c?w=600&auto=format&fit=crop&q=80",
            aliases = listOf("cutting and grinding disk", "cutting and grinding disks", "cutting & grinding disk", "cutting & grinding disks", "cutting disk", "grinding disk", "cat_cutting_disk", "cat_disk", "disks", "discs", "flap disc")
        ),
        OfficialCategory(
            name = "Machine Accessories",
            defaultId = "cat_machine_accessories",
            defaultImageUrl = "https://images.unsplash.com/photo-1581092335397-9583fe92d232?w=600&auto=format&fit=crop&q=80",
            aliases = listOf("machine accessories", "accessories", "machine accessory", "cat_machine_accessories", "cat_accessories")
        ),
        OfficialCategory(
            name = "Spare Parts",
            defaultId = "cat_spare_parts",
            defaultImageUrl = "https://images.unsplash.com/photo-1581092162384-8987c1d64718?w=600&auto=format&fit=crop&q=80",
            aliases = listOf("spare parts", "spares", "spare part", "cat_spare_parts", "cat_spares")
        ),
        OfficialCategory(
            name = "Coil & Armature",
            defaultId = "cat_coil_armature",
            defaultImageUrl = "https://images.unsplash.com/photo-1581092162384-8987c1d64718?w=600&auto=format&fit=crop&q=80",
            aliases = listOf("coil & armature", "coil and armature", "armature", "coil", "cat_coil_armature", "cat_armature", "field coil", "stator")
        )
    )

    fun buildCategoriesFromProducts(productList: List<Product>): List<Category> {
        val activeProducts = productList.filter { it.active }

        return OFFICIAL_CATEGORIES.mapIndexed { index, official ->
            // Find products matching either categoryId or categoryName
            val matchingProducts = activeProducts.filter { prod ->
                prod.categoryId.equals(official.defaultId, ignoreCase = true) ||
                prod.categoryName.equals(official.name, ignoreCase = true) ||
                official.aliases.any { alias ->
                    prod.categoryId.equals(alias, ignoreCase = true) ||
                    prod.categoryName.equals(alias, ignoreCase = true)
                }
            }

            // If products exist, use their exact categoryId from Firestore so filtering by categoryId works
            val effectiveCategoryId = if (matchingProducts.isNotEmpty()) {
                matchingProducts.first().categoryId.ifBlank { official.defaultId }
            } else {
                official.defaultId
            }

            // Cloudinary URL from first product if available, else default
            val productCloudinaryThumb = matchingProducts.firstOrNull { it.thumbnailUrl.isNotBlank() }?.thumbnailUrl
                ?: matchingProducts.firstOrNull { it.images.isNotEmpty() }?.images?.firstOrNull { it.isNotBlank() }

            Category(
                id = effectiveCategoryId,
                name = official.name,
                imageUrl = productCloudinaryThumb ?: official.defaultImageUrl,
                productCount = matchingProducts.size,
                active = true,
                sortOrder = index + 1
            )
        }
    }

    val categories: List<Category>
        get() = buildCategoriesFromProducts(products)

    val brands = listOf(
        Brand(id = "bosch", name = "Bosch"),
        Brand(id = "makita", name = "Makita"),
        Brand(id = "dewalt", name = "Dewalt"),
        Brand(id = "dongcheng", name = "Dongcheng"),
        Brand(id = "anu-ind", name = "Anu Industrial"),
        Brand(id = "hitachi-hikoki", name = "Hikoki")
    )

    // Strictly empty list - NO fake or demo products. ONLY real Firestore products are used.
    val products: List<Product> = emptyList()

    val demoRetailer = Retailer(
        retailerId = "ret-anu-demo-01",
        userId = "ret-anu-demo-01",
        name = "Jagdish Patel",
        shopName = "Surat Power Tools & Hardware",
        phone = "+91 98251 23456",
        email = "surattools@gmail.com",
        gstNumber = "24AABCS1234F1Z9",
        businessType = "Hardware Store",
        address = "Shop 14, Vesu Main Road, Near Reliance Market",
        city = "Surat",
        area = "Vesu",
        pincode = "395007",
        status = "ACTIVE"
    )
}
