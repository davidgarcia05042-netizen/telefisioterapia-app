package com.example.therapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun NavigationDrawerHeader(
    value: String?,
    name: String? = "No name",
    profileImgURL: String? = null
) {
    Box(
        modifier = Modifier
//            .background(Brush.horizontalGradient(listOf(Primary, Secondary))) // TODO
            .fillMaxWidth()
            .height(210.dp)
            .padding(10.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            GlideImage(
                model = profileImgURL ?: "https://http.cat/images/200.jpg",
                contentDescription = "Profile Image",
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Text(
                text = value ?: "Sin datos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                color = Color.Black
            )
            if (name != null) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Gray
                )
            }
        }

    }
}