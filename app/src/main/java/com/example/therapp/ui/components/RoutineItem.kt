package com.example.therapp.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.therapp.common.toReadableDate
import com.example.therapp.domain.model.Routine
import com.example.therapp.domain.model.Therapy


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 9/19/2025
 * @version 1.0
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
@Preview(showBackground = true, showSystemUi = true)
fun RoutineItem(
    routine: Routine = Routine(
        id = 1,
        therapyId = 1,
        therapy = Therapy(
            id = 1,
            name = "name",
            description = "description",
            isActive = true,
            isModel = true,
        ),
        name = "name",
        description = "description",
        exercises = emptyList(),
        isModeled = true,
        isActive = true
    ),
    startTime: String = "startTime",
    onItemClicked: (Int) -> Unit = {}
) {
    Column {
        Spacer(Modifier.height(10.dp))
        OutlinedCard(
            modifier = Modifier
                .padding(start = 2.dp, end = 2.dp)
                .fillMaxWidth(),
            onClick = { onItemClicked(routine.id) }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(routine.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(routine.description, fontWeight = FontWeight.Light, fontSize = 14.sp)
                        Text(
                            "Inicio: ${startTime.toReadableDate()}",
                            fontWeight = FontWeight.Light,
                            fontSize = 14.sp
                        )
                    }
                    IconButton(
                        onClick = { onItemClicked(routine.id) }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Go to detail"
                        )
                    }
                }
            }
        }
    }
}
