package com.example.healthlog.guidelines.protocols

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.theme.HealthLogTheme

// Visual content displaying clinical protocol steps, guidelines, and rules for a measurement type.
@Composable
fun MeasurementGuideContent(
    protocol: MeasurementProtocol,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Doctor Persona + Intro Speech Cloud
        DoctorPersonaIntro(
            clinicalReferenceRes = protocol.clinicalReferenceRes,
            summaryRes = protocol.summaryRes,
        )

        // Cloud 1: Step-by-Step Instructions Speech Bubble
        SpeechBubbleCard(
            title = stringResource(R.string.guide_section_steps),
            isSecondary = false,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                protocol.steps.forEachIndexed { index, step ->
                    ProtocolStepCard(
                        stepNumber = index + 1,
                        step = step,
                    )
                }
            }
        }

        // Cloud 2: Important Tips & Rules Speech Bubble
        if (protocol.tips.isNotEmpty()) {
            SpeechBubbleCard(
                title = stringResource(R.string.guide_section_tips),
                isSecondary = true,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    protocol.tips.forEach { tipRes ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary),
                            )
                            Text(
                                text = stringResource(tipRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

// Visual card for an individual protocol step.
@Composable
private fun ProtocolStepCard(
    stepNumber: Int,
    step: ProtocolStep,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Step Number Badge or Icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (step.iconRes != null) {
                        Icon(
                            painter = painterResource(step.iconRes),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(step.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(step.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MeasurementGuideContentBpPreview() {
    HealthLogTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            MeasurementGuideContent(
                protocol = getMeasurementProtocol(MeasurementType.BLOOD_PRESSURE),
            )
        }
    }
}
