package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.BusinessSettings
import com.example.model.CartItem
import com.example.ui.theme.*

@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    businessSettings: BusinessSettings,
    onQuantityChange: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onProceedToCheckout: () -> Unit,
    onBrowseProducts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtotal = cartItems.sumOf { it.subtotal }
    val totalGst = cartItems.sumOf { it.subtotal * (it.product.gstPercentage / 100.0) }
    val deliveryCharge = if (subtotal >= businessSettings.freeDeliveryThreshold || subtotal == 0.0) 0.0 else businessSettings.deliveryCharge
    val grandTotal = subtotal + totalGst + deliveryCharge

    if (cartItems.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SlateLightBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(SafetyOrangeContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = "Empty Cart",
                        tint = SafetyOrangePrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your cart is waiting for its first tool.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IndustrialDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Add grinders, drill machines, cutters or spares for wholesale delivery",
                    fontSize = 13.sp,
                    color = SlateGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onBrowseProducts,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrangePrimary),
                    modifier = Modifier.testTag("empty_cart_browse_button")
                ) {
                    Text("Browse Products", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(SlateLightBg)
        ) {
            val isSmall = maxWidth < 360.dp
            val screenPad = if (isSmall) 12.dp else 16.dp

            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = screenPad),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Free Delivery Banner
                    item {
                        val remainingForFree = businessSettings.freeDeliveryThreshold - subtotal
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (remainingForFree <= 0) StockGreenBg else SafetyOrangeContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (remainingForFree <= 0) Icons.Default.CheckCircle else Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = if (remainingForFree <= 0) StockGreen else SafetyOrangeDark
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (remainingForFree <= 0) {
                                        "Your order qualifies for FREE Express Delivery!"
                                    } else {
                                        "Add ₹${"%,.0f".format(remainingForFree)} more for FREE Delivery"
                                    },
                                    fontSize = if (isSmall) 11.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remainingForFree <= 0) StockGreen else OnSafetyOrangeContainer
                                )
                            }
                        }
                    }

                // Cart Item Cards
                items(cartItems, key = { it.product.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.product.imageUrl,
                                contentDescription = item.product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IndustrialDark,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "SKU: ${item.product.sku}",
                                    fontSize = 11.sp,
                                    color = SlateGray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹${"%,.0f".format(item.product.effectivePrice)} × ${item.quantity} = ₹${"%,.0f".format(item.subtotal)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafetyOrangePrimary
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Quantity buttons & delete
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(SlateLightBg, RoundedCornerShape(6.dp))
                                            .border(1.dp, SlateBorder, RoundedCornerShape(6.dp))
                                    ) {
                                        IconButton(
                                            onClick = { onQuantityChange(item.product.id, item.quantity - 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                        }
                                        Text(
                                            text = item.quantity.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        IconButton(
                                            onClick = { onQuantityChange(item.product.id, item.quantity + 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    IconButton(
                                        onClick = { onRemoveItem(item.product.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Remove",
                                            tint = StockRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bill Breakdown Card
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Order Pricing Breakdown",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndustrialDark
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Items Subtotal (${cartItems.sumOf { it.quantity }} units)", fontSize = 13.sp, color = SlateGray)
                                Text("₹${"%,.2f".format(subtotal)}", fontSize = 13.sp, color = IndustrialDark, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Applicable GST (18% Tax)", fontSize = 13.sp, color = SlateGray)
                                Text("₹${"%,.2f".format(totalGst)}", fontSize = 13.sp, color = IndustrialDark, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Delivery / Logistics", fontSize = 13.sp, color = SlateGray)
                                Text(
                                    text = if (deliveryCharge == 0.0) "FREE" else "₹${"%,.2f".format(deliveryCharge)}",
                                    fontSize = 13.sp,
                                    color = if (deliveryCharge == 0.0) StockGreen else IndustrialDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = SlateBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Estimated Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IndustrialDark)
                                Text("₹${"%,.2f".format(grandTotal)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = SafetyOrangePrimary)
                            }
                        }
                    }
                }
            }

            // Checkout Footer
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = screenPad, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Amount", fontSize = 11.sp, color = SlateGray)
                        Text(
                            text = "₹${"%,.2f".format(grandTotal)}",
                            fontSize = if (isSmall) 16.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafetyOrangePrimary
                        )
                    }

                    Button(
                        onClick = onProceedToCheckout,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyOrangePrimary),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("cart_proceed_to_checkout_button")
                    ) {
                        Text(
                            text = if (isSmall) "Checkout" else "Proceed to Checkout",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
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
