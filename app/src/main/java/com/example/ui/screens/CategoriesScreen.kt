package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.Category
import com.example.ui.theme.*

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLightBg)
    ) {
        val isSmall = maxWidth < 340.dp
        val screenPad = if (maxWidth < 360.dp) 12.dp else 16.dp
        val gridSpacing = if (isSmall) 8.dp else 12.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPad)
        ) {
            Text(
                text = "Product Categories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = IndustrialDark
            )
            Text(
                text = "Browse official machinery, equipment and original accessories",
                fontSize = if (isSmall) 12.sp else 13.sp,
                color = SlateGray
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = SafetyOrangePrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Loading categories from live inventory...",
                            fontSize = 13.sp,
                            color = SlateGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = if (isSmall) GridCells.Fixed(1) else GridCells.Adaptive(minSize = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(categories) { category ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onCategoryClick(category) }
                                .testTag("category_grid_item_${category.id}")
                        ) {
                            if (isSmall) {
                                // Horizontal card for small screen width
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = category.imageUrl,
                                        contentDescription = category.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE2E8F0))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = IndustrialDark,
                                            maxLines = 2,
                                            lineHeight = 16.sp,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val productCountText = when (category.productCount) {
                                            0 -> "0 Products"
                                            1 -> "1 Product Available"
                                            else -> "${category.productCount} Products Available"
                                        }
                                        Text(
                                            text = productCountText,
                                            fontSize = 11.sp,
                                            color = SafetyOrangeDark,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                // Standard vertical grid card
                                Column {
                                    AsyncImage(
                                        model = category.imageUrl,
                                        contentDescription = category.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .background(Color(0xFFE2E8F0))
                                    )

                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = IndustrialDark,
                                            maxLines = 2,
                                            lineHeight = 16.sp,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val productCountText = when (category.productCount) {
                                            0 -> "0 Products"
                                            1 -> "1 Product"
                                            else -> "${category.productCount} Products"
                                        }
                                        Text(
                                            text = productCountText,
                                            fontSize = 11.sp,
                                            color = SafetyOrangeDark,
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
    }
}
