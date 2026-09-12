package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.data.AnuToolsRepository
import com.example.model.Order
import com.example.ui.components.OrderStatusTimeline
import com.example.ui.theme.*
import com.example.util.CloudinaryUploader
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrderDetailScreen(
    orderId: String,
    initialOrder: Order? = null,
    repository: AnuToolsRepository,
    onBack: () -> Unit,
    onReorder: (Order) -> Unit,
    onSubmitProof: (orderId: String, utr: String, amount: Double, note: String, screenshotUrl: String) -> Unit,
    onContactSupport: (Order) -> Unit,
    modifier: Modifier = Modifier
) {
    // Real-time Firestore snapshot listener on orders/{orderId}
    val liveOrder by repository.getOrderFlow(orderId).collectAsState(initial = initialOrder)
    val order = liveOrder

    if (order == null) {
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
                        text = "Order Details",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = SafetyOrangePrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading order details...",
                        fontSize = 13.sp,
                        color = SlateGray
                    )
                }
            }
        }
        return
    }

    OrderDetailContent(
        order = order,
        onBack = onBack,
        onReorder = onReorder,
        onSubmitProof = onSubmitProof,
        onContactSupport = onContactSupport,
        modifier = modifier
    )
}

@Composable
fun OrderDetailScreen(
    order: Order,
    onBack: () -> Unit,
    onReorder: (Order) -> Unit,
    onSubmitProof: (orderId: String, utr: String, amount: Double, note: String, screenshotUrl: String) -> Unit,
    onContactSupport: (Order) -> Unit,
    modifier: Modifier = Modifier
) {
    OrderDetailContent(
        order = order,
        onBack = onBack,
        onReorder = onReorder,
        onSubmitProof = onSubmitProof,
        onContactSupport = onContactSupport,
        modifier = modifier
    )
}

@Composable
private fun OrderDetailContent(
    order: Order,
    onBack: () -> Unit,
    onReorder: (Order) -> Unit,
    onSubmitProof: (orderId: String, utr: String, amount: Double, note: String, screenshotUrl: String) -> Unit,
    onContactSupport: (Order) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showProofDialog by remember { mutableStateOf(false) }
    var viewingScreenshotUrl by remember { mutableStateOf<String?>(null) }

    var proofUtr by remember(order.paymentReference, order.paymentProof) {
        mutableStateOf(order.paymentReference.ifBlank { order.paymentProof?.utr ?: "" })
    }
    var proofNote by remember(order.paymentProof) {
        mutableStateOf(order.paymentProof?.paymentNote ?: "")
    }
    var selectedProofImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingProof by remember { mutableStateOf(false) }
    var proofErrorMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedProofImageUri = uri
            showProofDialog = true
        }
    }

    val currentAuthUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isOwner = order.retailerId.isBlank() || currentAuthUid.isBlank() || order.retailerId == currentAuthUid

    val effectiveScreenshotUrl = order.paymentScreenshotUrl.ifBlank {
        order.paymentProof?.screenshotUrl ?: ""
    }
    val effectiveUtr = order.paymentReference.ifBlank {
        order.paymentProof?.utr ?: ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLightBg)
    ) {
        // Top Bar
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Order Details",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = order.orderNumber,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Order Summary Header Card
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
                        Column {
                            Text(
                                text = "Order ${order.orderNumber}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = IndustrialDark
                            )
                            val orderDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                .format(Date(order.createdAt))
                            Text(
                                text = "Placed on $orderDate",
                                fontSize = 12.sp,
                                color = SlateGray
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (order.orderStatus) {
                                "DELIVERED" -> StockGreenBg
                                "CANCELLED" -> StockRedBg
                                else -> StockAmberBg
                            }
                        ) {
                            Text(
                                text = order.orderStatus.replace("_", " "),
                                color = when (order.orderStatus) {
                                    "DELIVERED" -> StockGreen
                                    "CANCELLED" -> StockRed
                                    else -> StockAmber
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SlateBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Payment Mode", fontSize = 11.sp, color = SlateGray)
                            Text(
                                text = if (order.paymentMethod == "COD") "Cash on Delivery" else "UPI / QR Scanner",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IndustrialDark
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Payment Status", fontSize = 11.sp, color = SlateGray)
                            Text(
                                text = order.paymentStatus.replace("_", " "),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (order.paymentStatus.uppercase() == "PAID") StockGreen else StockAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Order ID: ${order.orderId}",
                        fontSize = 11.sp,
                        color = SlateGray
                    )
                }
            }

            // Dedicated Payment Proof & Reference Card (QR/UPI Payments)
            if (order.paymentMethod == "QR") {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, if (order.paymentStatus.uppercase() == "PAID") StockGreen.copy(alpha = 0.5f) else SafetyOrangePrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = SafetyOrangePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Payment Verification & Proof",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = IndustrialDark
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (order.paymentStatus.uppercase() == "PAID") StockGreenBg else StockAmberBg
                            ) {
                                Text(
                                    text = if (order.paymentStatus.uppercase() == "PAID") "VERIFIED" else "PENDING VERIFICATION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (order.paymentStatus.uppercase() == "PAID") StockGreen else StockAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // UTR / Reference
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Payment Reference / UTR:", fontSize = 12.sp, color = SlateGray)
                            Text(
                                text = effectiveUtr.ifBlank { "Not submitted yet" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (effectiveUtr.isNotBlank()) IndustrialDark else StockAmber
                            )
                        }

                        // Submitted timestamp
                        val subAt = if (order.paymentProofSubmittedAt > 0) order.paymentProofSubmittedAt else order.paymentProof?.submittedAt ?: 0L
                        if (subAt > 0) {
                            val formattedSub = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(subAt))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Proof Submitted At:", fontSize = 12.sp, color = SlateGray)
                                Text(formattedSub, fontSize = 12.sp, color = SlateGray)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Screenshot preview or upload affordance
                        if (effectiveScreenshotUrl.isNotBlank() && !effectiveScreenshotUrl.startsWith("receipt_ref_") && !effectiveScreenshotUrl.startsWith("qr_receipt_")) {
                            Text(
                                text = "Payment Screenshot (Click to view full receipt):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IndustrialDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SlateLightBg)
                                    .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewingScreenshotUrl = effectiveScreenshotUrl },
                                contentAlignment = Alignment.Center
                            ) {
                                SubcomposeAsyncImage(
                                    model = effectiveScreenshotUrl,
                                    contentDescription = "Payment Screenshot Preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        CircularProgressIndicator(
                                            color = SafetyOrangePrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                )
                            }
                        } else {
                            Surface(
                                color = SlateLightBg,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = SlateGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "No payment screenshot attached yet. Please upload your payment receipt for instant admin verification.",
                                        fontSize = 12.sp,
                                        color = SlateGray
                                    )
                                }
                            }
                        }

                        // Action buttons (Only if not verified as PAID, and only by owner)
                        if (order.paymentStatus.uppercase() != "PAID" && isOwner) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrangePrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (effectiveScreenshotUrl.isNotBlank()) "Replace Screenshot" else "Upload Payment Screenshot",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showProofDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, SafetyOrangePrimary)
                                ) {
                                    Text(
                                        text = "Edit UTR",
                                        color = SafetyOrangePrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real-time Status Timeline (7 Admin stages)
            OrderStatusTimeline(
                currentStatus = order.orderStatus,
                history = order.statusHistory,
                paymentStatus = order.paymentStatus
            )

            // Purchased Items List
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Purchased Items (${order.items.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = IndustrialDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    order.items.forEachIndexed { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF1F5F9))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IndustrialDark
                                )
                                Text(
                                    text = "SKU: ${item.sku} • Qty: ${item.quantity} ${item.unit}",
                                    fontSize = 11.sp,
                                    color = SlateGray
                                )
                            }
                            Text(
                                text = "₹${"%,.0f".format(item.subtotal)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndustrialDark
                            )
                        }
                        if (index < order.items.size - 1) {
                            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SlateBorder)

                    // Pricing breakdown
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", fontSize = 12.sp, color = SlateGray)
                        Text("₹${"%,.2f".format(order.subtotal)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    if (order.discount > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount", fontSize = 12.sp, color = StockGreen)
                            Text("-₹${"%,.2f".format(order.discount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StockGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GST (18% Tax)", fontSize = 12.sp, color = SlateGray)
                        Text("₹${"%,.2f".format(order.gst)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery Fee", fontSize = 12.sp, color = SlateGray)
                        Text(if (order.deliveryCharge == 0.0) "FREE" else "₹${"%,.2f".format(order.deliveryCharge)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateBorder)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Grand Total", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IndustrialDark)
                        Text("₹${"%,.2f".format(order.total)}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = SafetyOrangePrimary)
                    }
                }
            }

            // Delivery Address Snapshot
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Delivery Destination",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = IndustrialDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val s = order.retailerSnapshot
                    Text(
                        text = s["shopName"] as? String ?: "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = IndustrialDark
                    )
                    Text(
                        text = "${s["name"] as? String ?: ""} • ${s["phone"] as? String ?: ""}",
                        fontSize = 12.sp,
                        color = SlateGray
                    )
                    Text(
                        text = "${s["address"] as? String ?: ""}, ${s["area"] as? String ?: ""}, ${s["city"] as? String ?: ""} - ${s["pincode"] as? String ?: ""}",
                        fontSize = 12.sp,
                        color = SlateGray
                    )
                    val gstin = (s["gstNumber"] as? String) ?: (s["gstin"] as? String)
                    if (!gstin.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "GSTIN: $gstin",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IndustrialCharcoal
                        )
                    }
                }
            }

            // Bottom Actions: Reorder + Inquire
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onReorder(order) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrangePrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("order_reorder_button")
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reorder All Items", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onContactSupport(order) },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, IndustrialCharcoal),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = IndustrialCharcoal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inquire Order", color = IndustrialCharcoal, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Fullscreen Screenshot Viewer Dialog
    if (viewingScreenshotUrl != null) {
        Dialog(onDismissRequest = { viewingScreenshotUrl = null }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Payment Proof Receipt",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = { viewingScreenshotUrl = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SubcomposeAsyncImage(
                        model = viewingScreenshotUrl,
                        contentDescription = "Full Receipt",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp),
                        loading = {
                            CircularProgressIndicator(
                                color = SafetyOrangePrimary,
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    )
                }
            }
        }
    }

    // Submit Payment Reference & Screenshot Dialog
    if (showProofDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isUploadingProof) {
                    showProofDialog = false
                    proofErrorMessage = null
                }
            },
            title = {
                Text(
                    text = "Submit Payment Verification",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your 12-digit UPI UTR and attach payment screenshot after scanning the Anu Tools QR.",
                        fontSize = 12.sp,
                        color = SlateGray
                    )

                    OutlinedTextField(
                        value = proofUtr,
                        onValueChange = { proofUtr = it },
                        label = { Text("12-Digit UTR / Reference Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = proofNote,
                        onValueChange = { proofNote = it },
                        label = { Text("Payer Name / Note (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Screenshot selector inside dialog
                    Text(
                        text = "Payment Screenshot:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IndustrialDark
                    )

                    val previewUri = selectedProofImageUri
                    if (previewUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, SafetyOrangePrimary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            SubcomposeAsyncImage(
                                model = previewUri,
                                contentDescription = "New Receipt Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    selectedProofImageUri = null
                                    proofErrorMessage = null
                                }
                            ) {
                                Text("Remove Screenshot", fontSize = 12.sp, color = StockRed)
                            }
                            TextButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text("Change Screenshot", fontSize = 12.sp, color = SafetyOrangePrimary)
                            }
                        }
                    } else if (effectiveScreenshotUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, SlateBorder, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            SubcomposeAsyncImage(
                                model = effectiveScreenshotUrl,
                                contentDescription = "Existing Receipt",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        TextButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Select New Screenshot", fontSize = 12.sp, color = SafetyOrangePrimary)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SafetyOrangePrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Screenshot from Device", fontSize = 12.sp)
                        }
                    }

                    if (proofErrorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StockRedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = proofErrorMessage ?: "",
                                    fontSize = 11.sp,
                                    color = StockRed,
                                    lineHeight = 15.sp
                                )
                                if (selectedProofImageUri != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = {
                                            if (proofUtr.isBlank()) {
                                                proofErrorMessage = "Please enter the 12-digit UTR reference."
                                                return@OutlinedButton
                                            }
                                            coroutineScope.launch {
                                                onSubmitProof(
                                                    order.orderId,
                                                    proofUtr.trim(),
                                                    order.total,
                                                    proofNote.trim(),
                                                    effectiveScreenshotUrl.trim()
                                                )
                                                showProofDialog = false
                                                selectedProofImageUri = null
                                                proofErrorMessage = null
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = IndustrialDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Proceed & Submit with UTR ($proofUtr)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    if (isUploadingProof) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = SafetyOrangePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Uploading receipt to Cloudinary...",
                                fontSize = 11.sp,
                                color = SafetyOrangePrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (proofUtr.isBlank()) {
                            proofErrorMessage = "Please enter the 12-digit UTR reference."
                            return@Button
                        }
                        isUploadingProof = true
                        proofErrorMessage = null

                        coroutineScope.launch {
                            try {
                                var finalScreenshotUrl = effectiveScreenshotUrl
                                val uriToUpload = selectedProofImageUri

                                if (uriToUpload != null) {
                                    val uploadResult = CloudinaryUploader.uploadImageFromUri(
                                        context = context,
                                        imageUri = uriToUpload,
                                        folder = CloudinaryUploader.FOLDER_PAYMENT_PROOFS,
                                        cloudName = CloudinaryUploader.DEFAULT_CLOUD_NAME,
                                        uploadPreset = CloudinaryUploader.PAYMENT_PROOFS_UPLOAD_PRESET
                                    )
                                    if (uploadResult.isSuccess) {
                                        finalScreenshotUrl = uploadResult.getOrThrow()
                                    } else {
                                        val err = uploadResult.exceptionOrNull()?.message ?: "Upload failed"
                                        proofErrorMessage = "Could not upload image: $err"
                                        isUploadingProof = false
                                        return@launch
                                    }
                                }

                                onSubmitProof(
                                    order.orderId,
                                    proofUtr.trim(),
                                    order.total,
                                    proofNote.trim(),
                                    finalScreenshotUrl.trim()
                                )
                                isUploadingProof = false
                                showProofDialog = false
                                selectedProofImageUri = null
                            } catch (e: Exception) {
                                isUploadingProof = false
                                proofErrorMessage = "Submission failed: ${e.message}"
                            }
                        }
                    },
                    enabled = !isUploadingProof,
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrangePrimary)
                ) {
                    if (isUploadingProof) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submitting...")
                    } else {
                        Text("Submit to Anu Tools")
                    }
                }
            },
            dismissButton = {
                if (!isUploadingProof) {
                    TextButton(onClick = {
                        showProofDialog = false
                        proofErrorMessage = null
                    }) {
                        Text("Cancel", color = SlateGray)
                    }
                }
            }
        )
    }
}
