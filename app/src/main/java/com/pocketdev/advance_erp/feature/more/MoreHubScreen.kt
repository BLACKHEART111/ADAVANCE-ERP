package com.pocketdev.advance_erp.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPSectionHeader
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary

data class MoreHubItem(
    val route: String,
    val titleResId: Int,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun MoreHubScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        MoreHubItem("products", R.string.nav_products, "Catalog, variants & prices", Icons.Default.Category, PrimaryPurple),
        MoreHubItem("import_management", R.string.nav_import_management, "China sourcing & shipments", Icons.Default.Public, SecondaryTeal),
        MoreHubItem("purchases", R.string.nav_purchases, "Purchase orders pipeline", Icons.Default.ShoppingCart, Color(0xFFE11D48)),
        MoreHubItem("shipments", R.string.nav_shipments, "Air cargo & port tracking", Icons.Default.FlightTakeoff, Color(0xFF0284C7)),
        MoreHubItem("customers", R.string.nav_customers, "Wholesale & retail clients", Icons.Default.Group, Color(0xFF16A34A)),
        MoreHubItem("suppliers", R.string.nav_suppliers, "China factory partners", Icons.Default.Storefront, Color(0xFFD97706)),
        MoreHubItem("ledger", R.string.nav_ledger, "Customer & supplier ledger", Icons.Default.MenuBook, Color(0xFF7C3AED)),
        MoreHubItem("reports", R.string.nav_reports, "Business Control Center & Ledgers", Icons.Default.Assessment, Color(0xFF059669)),
        MoreHubItem("settings", R.string.nav_settings, "Language, currency & profile", Icons.Default.Settings, Color(0xFF475569))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ERPBackground)
            .padding(16.dp)
    ) {
        ERPSectionHeader(
            title = "All Modules & Tools",
            subtitle = "Single-device import business ERP features"
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(items) { item ->
                ERPCard(
                    onClick = { onNavigate(item.route) },
                    contentPadding = 14.dp
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(item.color.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.color,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(item.titleResId),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
