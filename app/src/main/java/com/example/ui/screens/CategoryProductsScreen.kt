package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Category
import com.example.model.Product
import com.example.ui.components.ProductCard
import com.example.ui.theme.*

@Composable
fun CategoryProductsScreen(
    category: Category,
    products: List<Product>,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBrand by remember { mutableStateOf<String?>(null) }
    var inStockOnly by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf("DEFAULT") } // DEFAULT, PRICE_ASC, PRICE_DESC, POPULAR

    val categoryProducts = remember(products, category.id, category.name) {
        val official = com.example.data.DefaultData.OFFICIAL_CATEGORIES.firstOrNull {
            it.name.equals(category.name, ignoreCase = true) ||
            it.defaultId.equals(category.id, ignoreCase = true)
        }
        products.filter { prod ->
            prod.active && (
                prod.categoryId.equals(category.id, ignoreCase = true) ||
                (official != null && (
                    prod.categoryId.equals(official.defaultId, ignoreCase = true) ||
                    prod.categoryName.equals(official.name, ignoreCase = true) ||
                    official.aliases.any { alias ->
                        prod.categoryId.equals(alias, ignoreCase = true) ||
                        prod.categoryName.equals(alias, ignoreCase = true)
                    }
                ))
            )
        }
    }

    val availableBrands = remember(categoryProducts) {
        categoryProducts.map { it.brand }.distinct().sorted()
    }

    val filteredProducts = remember(categoryProducts, selectedBrand, inStockOnly, sortOption) {
        var list = categoryProducts.filter { prod ->
            (selectedBrand == null || prod.brand.equals(selectedBrand, ignoreCase = true)) &&
            (!inStockOnly || prod.isAvailable)
        }

        list = when (sortOption) {
            "PRICE_ASC" -> list.sortedBy { it.effectivePrice }
            "PRICE_DESC" -> list.sortedByDescending { it.effectivePrice }
            else -> list
        }
        list
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLightBg)
    ) {
        val isSmall = maxWidth < 340.dp
        val screenPad = if (maxWidth < 360.dp) 12.dp else 16.dp
        val gridSpacing = if (isSmall) 8.dp else 12.dp
        val gridColumns = if (isSmall) GridCells.Fixed(1) else GridCells.Adaptive(minSize = 145.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = screenPad)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Category Header Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = IndustrialDark
                    )
                    Text(
                        text = "Showing ${filteredProducts.size} tools & machinery items",
                        fontSize = if (isSmall) 11.sp else 12.sp,
                        color = SlateGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Filters (Brand chips & In-Stock toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedBrand == null,
                    onClick = { selectedBrand = null },
                    label = { Text("All Brands") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SafetyOrangePrimary,
                        selectedLabelColor = Color.White
                    )
                )

                availableBrands.forEach { brand ->
                    FilterChip(
                        selected = selectedBrand == brand,
                        onClick = { selectedBrand = if (selectedBrand == brand) null else brand },
                        label = { Text(brand) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SafetyOrangePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                FilterChip(
                    selected = inStockOnly,
                    onClick = { inStockOnly = !inStockOnly },
                    label = { Text("In Stock Only") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StockGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sorting options row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Sort", tint = SlateGray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sort: ", fontSize = 12.sp, color = SlateGray)
                TextButton(onClick = {
                    sortOption = when (sortOption) {
                        "PRICE_ASC" -> "PRICE_DESC"
                        "PRICE_DESC" -> "DEFAULT"
                        else -> "PRICE_ASC"
                    }
                }) {
                    Text(
                        text = when (sortOption) {
                            "PRICE_ASC" -> "Price: Low to High ↑"
                            "PRICE_DESC" -> "Price: High to Low ↓"
                            else -> "Default Relevance"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafetyOrangeDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No tools match this filter in ${category.name}",
                            fontWeight = FontWeight.SemiBold,
                            color = SlateGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                selectedBrand = null
                                inStockOnly = false
                                sortOption = "DEFAULT"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SafetyOrangePrimary)
                        ) {
                            Text("Reset Filters")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = gridColumns,
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts) { product ->
                        ProductCard(
                            product = product,
                            onProductClick = onProductClick,
                            onAddToCart = onAddToCart
                        )
                    }
                }
            }
        }
    }
}
