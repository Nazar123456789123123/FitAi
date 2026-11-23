package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AiCoachInsight
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkObsidian
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.VoltLime
import com.example.ui.theme.VoltLimeDark
import com.example.ui.theme.VoltLimeSoft
import com.example.ui.theme.ZoneHighBg
import com.example.ui.theme.ZoneHighCrimson
import com.example.ui.viewmodel.AiActionState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiCoachScreen(
    insights: List<AiCoachInsight>,
    aiAnalysisState: AiActionState,
    aiChatState: AiActionState,
    onRunAnalysis: () -> Unit,
    onAskCoach: (String) -> Unit
) {
    var questionText by remember { mutableStateOf("") }
    val latestInsight = insights.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "AI-Тренер FitAI",
                    style = MaterialTheme.typography.headlineLarge,
                    color = InkObsidian
                )
                Text(
                    text = "Персональний науковий розбір твого тижня (MEV/MAV/MRV, RIR, інтерференція бігу й плавання) на базі Gemini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkCharcoal
                )

                Button(
                    onClick = onRunAnalysis,
                    enabled = aiAnalysisState !is AiActionState.Loading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkObsidian,
                        contentColor = VoltLime
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("run_ai_analysis_button")
                ) {
                    if (aiAnalysisState is AiActionState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = VoltLime,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text("Аналізую тренування…")
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Отримати AI-аналіз тижня")
                    }
                }

                if (aiAnalysisState is AiActionState.Error) {
                    Surface(
                        color = ZoneHighBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = aiAnalysisState.message,
                            color = ZoneHighCrimson,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        if (latestInsight != null) {
            item {
                Surface(
                    color = SurfaceWhite,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = VoltLimeSoft,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = latestInsight.volumeStatus,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = VoltLimeDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        InsightBlock("Загальна оцінка", latestInsight.summary)
                        InsightBlock("Відновлення та інтерференція", latestInsight.recoveryAdvice)
                        InsightBlock("Наступне тренування", latestInsight.nextWorkoutPlan)
                    }
                }
            }
        }

        // Interactive AI Q&A
        item {
            Surface(
                color = SurfaceWhite,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Запитати AI-тренера",
                        style = MaterialTheme.typography.titleLarge,
                        color = InkObsidian
                    )

                    val quickQuestions = listOf(
                        "Як розвести біг і день ніг?",
                        "Чи достатній мій обсяг MAV?",
                        "Коли мені потрібен Deload?",
                        "Склади силове тренування на сьогодні"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickQuestions.forEach { q ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    questionText = q
                                    onAskCoach(q)
                                },
                                label = { Text(q, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = questionText,
                            onValueChange = { questionText = it },
                            placeholder = { Text("Питання про RPE, обсяг, біг чи плавання…") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_question_input")
                        )
                        Surface(
                            color = InkObsidian,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(52.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (questionText.isNotBlank()) {
                                        onAskCoach(questionText)
                                    }
                                },
                                enabled = aiChatState !is AiActionState.Loading,
                                modifier = Modifier.testTag("send_ai_question_button")
                            ) {
                                if (aiChatState is AiActionState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = VoltLime,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Надіслати запитання",
                                        tint = VoltLime
                                    )
                                }
                            }
                        }
                    }

                    when (aiChatState) {
                        is AiActionState.Streaming -> {
                            Surface(
                                color = VoltLimeSoft,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = aiChatState.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkObsidian,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                        is AiActionState.Success -> {
                            Surface(
                                color = VoltLimeSoft,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = aiChatState.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkObsidian,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                        is AiActionState.Error -> {
                            Surface(
                                color = ZoneHighBg,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = aiChatState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ZoneHighCrimson,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightBlock(label: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = InkObsidian
        )
    }
}
