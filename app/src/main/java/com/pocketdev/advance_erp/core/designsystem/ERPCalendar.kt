package com.pocketdev.advance_erp.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import java.util.Calendar

@Composable
fun ERPCalendar(
    modifier: Modifier = Modifier,
    language: AppLanguage = AppLanguage.BANGLA,
    selectedTimestamp: Long = System.currentTimeMillis(),
    onDateSelected: (Long) -> Unit = {}
) {
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = selectedTimestamp } }
    var currentMonthOffset by remember { mutableIntStateOf(0) }
    var selectedDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    val activeCal = remember(currentMonthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, currentMonthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthName = remember(currentMonthOffset, language) {
        val pattern = if (language == AppLanguage.BANGLA) "MMMM yyyy" else "MMMM yyyy"
        val sdf = java.text.SimpleDateFormat(pattern, language.locale)
        val raw = sdf.format(activeCal.time)
        if (language == AppLanguage.BANGLA) Formatters.toBanglaDigits(raw) else raw
    }

    val daysInMonth = activeCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = activeCal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun, 1=Mon...

    val weekDays = if (language == AppLanguage.BANGLA) {
        listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
    } else {
        listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
    }

    // Example active days with transactions (e.g. today and a couple days)
    val activityDays = setOf(2, 5, 8, 12, 15, 18, 22, 25)

    ERPCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = ERPSurface,
        contentPadding = 14.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Month & Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )

                Row {
                    IconButton(
                        onClick = { currentMonthOffset-- },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = DeepNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { currentMonthOffset++ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = DeepNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Weekdays Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekDays.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid (up to 6 rows)
            var dayCounter = 1
            for (row in 0..5) {
                if (dayCounter > daysInMonth) break
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (col in 0..6) {
                        if (row == 0 && col < firstDayOfWeek) {
                            Box(modifier = Modifier.weight(1f).height(36.dp))
                        } else if (dayCounter <= daysInMonth) {
                            val currentDay = dayCounter
                            val isSelected = currentDay == selectedDay && currentMonthOffset == 0
                            val hasActivity = activityDays.contains(currentDay)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) PrimaryPurple else Color.Transparent
                                    )
                                    .clickable {
                                        selectedDay = currentDay
                                        val cal = Calendar.getInstance().apply {
                                            add(Calendar.MONTH, currentMonthOffset)
                                            set(Calendar.DAY_OF_MONTH, currentDay)
                                        }
                                        onDateSelected(cal.timeInMillis)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val dayText = if (language == AppLanguage.BANGLA) {
                                        Formatters.toBanglaDigits(currentDay.toString())
                                    } else {
                                        currentDay.toString()
                                    }
                                    Text(
                                        text = dayText,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                    if (hasActivity && !isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(SecondaryTeal, CircleShape)
                                        )
                                    }
                                }
                            }
                            dayCounter++
                        } else {
                            Box(modifier = Modifier.weight(1f).height(36.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}
