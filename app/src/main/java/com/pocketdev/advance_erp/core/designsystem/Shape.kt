package com.pocketdev.advance_erp.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ERPCornerRadius = 16.dp
val ERPCornerRadiusSmall = 8.dp
val ERPCornerRadiusLarge = 24.dp

val ERPShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(ERPCornerRadiusSmall),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(ERPCornerRadius),
    extraLarge = RoundedCornerShape(ERPCornerRadiusLarge)
)
