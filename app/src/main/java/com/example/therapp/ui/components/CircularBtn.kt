package com.example.therapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 9/25/2025
 * @version 1.0
 */
@Composable
fun CircularBtn(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Int = 48,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.2f))
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            tint = Color.White,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun CircularBtn(
    icon: Painter,
    modifier: Modifier = Modifier,
    size: Int = 48,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.2f))
            .clickable { onClick() }
    ) {
        Icon(
            painter = icon,
            tint = Color.White,
            contentDescription = contentDescription,
        )
    }
}