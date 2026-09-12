package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AnuToolsRepository
import com.example.model.BusinessSettings
import com.example.model.Order
import com.example.model.PaymentProof
import com.example.model.Retailer
import com.example.ui.theme.*
import com.example.util.OrderSuccessSound
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun CheckoutScreen(
    retailer: Retailer,
    revalidation: AnuToolsRepository.RevalidationResult,
    businessSettings: BusinessSettings,
    repository: AnuToolsRepository,
    onOrderPlaced: (Order) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) } // 1: Delivery, 2: Summary, 3: Payment

    // Delivery fields (pre-filled from Retailer)
    var shopName by remember { mutableStateOf(retailer.shopName) }
    var contactName by remember { mutableStateOf(retailer.name) }
    var phone by remember { mutableStateOf(retailer.phone) }
    var address by remember { mutableStateOf(retailer.address) }
    var area by remember { mutableStateOf(retailer.area) }
    var city by remember { mutableStateOf(retailer.city) }
    var pincode by remember { mutableStateOf(retailer.pincode) }
    var customerNote by remember { mutableStateOf("") }

    // Payment fields
    var selectedPaymentMethod by remember { mutableStateOf("COD") } // "COD" or "QR"
    var utrNumber by remember { mutableStateOf("") }
    var paymentNote by remember { mutableStateOf("") }
    var isPlacingOrder by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLightBg)
    ) {
        // Step Header
        Surface(
            color = IndustrialCharcoal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (step > 1) step-- else onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Checkout - Step $step of 3",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Step Progress Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val titles = listOf("1. Delivery Info", "2. Order Summary", "3. Payment & Place")
                    titles.forEachIndexed { index, label ->
                        val active = step >= (index + 1)
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(
                                        if (active) SafetyOrangePrimary else Color.White.copy(alpha = 0.2f),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (step == index + 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }

        // Body content per step
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMessage != null) {
                Surface(
                    color = StockRedBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = StockRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            when (step) {
                1 -> {
                    // STEP 1: Delivery Information
                    Text(
                        text = "Delivery & Workshop Address",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IndustrialDark
                    )
                    Text(
                        text = "Please verify the delivery location for heavy tools dispatch",
                        fontSize = 12.sp,
                        color = SlateGray
                    )

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = shopName,
                                onValueChange = { shopName = it },
                                label = { Text("Shop / Business Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = contactName,
                                onValueChange = { contactName = it },
                                label = { Text("Contact Person Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Mobile Phone Number") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Shop / Workshop Address") },
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
                            OutlinedTextField(
                                value = pincode,
                                onValueChange = { pincode = it },
                                label = { Text("Pincode") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                2 -> {
                    // STEP 2: Order Summary
                    Text(
                        text = "Review Order Items & Pricing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IndustrialDark
                    )

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            revalidation.updatedItems.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.product.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IndustrialDark
                                        )
                                        Text(
                                            text = "${item.quantity} × ₹${"%,.0f".format(item.product.effectivePrice)} / ${item.product.unit}",
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
                                Divider(color = SlateBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal", fontSize = 13.sp, color = SlateGray)
                                Text("₹${"%,.2f".format(revalidation.subtotal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GST (18% Tax)", fontSize = 13.sp, color = SlateGray)
                                Text("₹${"%,.2f".format(revalidation.gst)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Delivery Fee", fontSize = 13.sp, color = SlateGray)
                                Text(
                                    if (revalidation.deliveryCharge == 0.0) "FREE" else "₹${"%,.2f".format(revalidation.deliveryCharge)}",
                                    fontSize = 13.sp,
                                    color = if (revalidation.deliveryCharge == 0.0) StockGreen else IndustrialDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = SlateBorder)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Payable", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IndustrialDark)
                                Text("₹${"%,.2f".format(revalidation.total)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = SafetyOrangePrimary)
                            }
                        }
                    }

                    // Special Instructions / Note
                    OutlinedTextField(
                        value = customerNote,
                        onValueChange = { customerNote = it },
                        label = { Text("Special Dispatch Instructions (Optional)") },
                        placeholder = { Text("e.g. Call before delivery, deliver after 11 AM...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                3 -> {
                    // STEP 3: Payment Method
                    Text(
                        text = "Select Payment Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IndustrialDark
                    )

                    // Option A: Cash on Delivery (COD)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(
                            if (selectedPaymentMethod == "COD") 2.dp else 1.dp,
                            if (selectedPaymentMethod == "COD") SafetyOrangePrimary else SlateBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPaymentMethod = "COD" }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == "COD",
                                onClick = { selectedPaymentMethod = "COD" },
                                colors = RadioButtonDefaults.colors(selectedColor = SafetyOrangePrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cash on Delivery (COD)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = IndustrialDark
                                )
                                Text(
                                    text = "Pay in cash or shop cheque upon machinery delivery",
                                    fontSize = 12.sp,
                                    color = SlateGray
                                )
                            }
                        }
                    }

                    // Option B: QR / Scanner Payment (UPI manual payment)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(
                            if (selectedPaymentMethod == "QR") 2.dp else 1.dp,
                            if (selectedPaymentMethod == "QR") SafetyOrangePrimary else SlateBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPaymentMethod = "QR" }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedPaymentMethod == "QR",
                                    onClick = { selectedPaymentMethod = "QR" },
                                    colors = RadioButtonDefaults.colors(selectedColor = SafetyOrangePrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "QR / Scanner Payment (UPI)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = IndustrialDark
                                    )
                                    Text(
                                        text = "Scan official Anu Tools QR with any UPI App (GPay, PhonePe, Paytm)",
                                        fontSize = 12.sp,
                                        color = SlateGray
                                    )
                                }
                            }

                            if (selectedPaymentMethod == "QR") {
                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = SlateBorder)

                                // QR Code Box
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SlateLightBg, RoundedCornerShape(8.dp))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Anu Tools & Service Center Official QR",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = IndustrialDark
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    AsyncImage(
                                        model = businessSettings.paymentQrUrl,
                                        contentDescription = "Anu Tools Payment QR",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(170.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "UPI ID: ${businessSettings.upiId}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SafetyOrangePrimary
                                    )
                                    Text(
                                        text = "Payable Amount: ₹${"%,.2f".format(revalidation.total)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = IndustrialDark
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "• Note: Status will be 'Pending Verification' until verified by Anu Tools Admin.",
                                        fontSize = 11.sp,
                                        color = SlateGray
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Payment Proof Details:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IndustrialDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = utrNumber,
                                    onValueChange = { utrNumber = it },
                                    label = { Text("12-Digit UTR / Transaction Reference") },
                                    placeholder = { Text("e.g. 409823489123") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = paymentNote,
                                    onValueChange = { paymentNote = it },
                                    label = { Text("Payment Note / Depositor Name (Optional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Footer
        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                // Inline error message
                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFD32F2F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                Column {
                    Text("Total", fontSize = 11.sp, color = SlateGray)
                    Text(
                        text = "₹${"%,.2f".format(revalidation.total)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafetyOrangePrimary
                    )
                }

                Button(
                    onClick = {
                        when (step) {
                            1 -> {
                                if (shopName.isBlank() || phone.isBlank() || address.isBlank()) {
                                    errorMessage = "Please complete shop name, phone number, and address."
                                } else {
                                    errorMessage = null
                                    step = 2
                                }
                            }
                            2 -> {
                                step = 3
                            }
                            3 -> {
                                isPlacingOrder = true
                                errorMessage = null
                                val addressMap = mapOf(
                                    "shopName" to shopName,
                                    "name" to contactName,
                                    "phone" to phone,
                                    "address" to address,
                                    "area" to area,
                                    "city" to city,
                                    "pincode" to pincode
                                )
                                val proof = if (selectedPaymentMethod == "QR" && utrNumber.isNotBlank()) {
                                    PaymentProof(
                                        utr = utrNumber,
                                        screenshotUrl = "qr_receipt_$utrNumber",
                                        amountPaid = revalidation.total,
                                        paymentNote = paymentNote,
                                        submittedAt = System.currentTimeMillis()
                                    )
                                } else null

                                    coroutineScope.launch {
                                        val result = repository.placeOrder(
                                            paymentMethod = selectedPaymentMethod,
                                            customerNote = customerNote,
                                            deliveryAddress = addressMap,
                                            paymentProof = proof
                                        )
                                        isPlacingOrder = false
                                        if (result.isSuccess) {
                                            OrderSuccessSound.playSuccessChime(context)
                                            onOrderPlaced(result.getOrThrow())
                                        } else {
                                            errorMessage = result.exceptionOrNull()?.message
                                                ?: "Failed to place order. Please try again."
                                        }
                                    }
                            }
                        }
                    },
                    enabled = !isPlacingOrder,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrangePrimary),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("checkout_continue_button")
                ) {
                    if (isPlacingOrder) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Placing Order...", fontWeight = FontWeight.Bold)
                    } else {
                        Text(
                            text = if (step == 3) "Place Order" else "Continue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
            }
        }
    }
}
