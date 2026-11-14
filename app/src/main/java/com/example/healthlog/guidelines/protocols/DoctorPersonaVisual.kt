package com.example.healthlog.guidelines.protocols

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.ui.theme.HealthLogTheme

// Custom speech bubble shape with a small triangular tail pointing towards the left avatar.
val LeftSpeechBubbleShape = GenericShape { size, _ ->
    val tailWidth = 20f
    val tailTop = 45f
    val tailBottom = 75f
    val cornerRadius = 40f

    // Start after top-left corner
    moveTo(tailWidth + cornerRadius, 0f)

    // Top edge to top-right
    lineTo(size.width - cornerRadius, 0f)
    arcTo(
        rect = Rect(size.width - cornerRadius * 2, 0f, size.width, cornerRadius * 2),
        startAngleDegrees = 270f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
    )

    // Right edge to bottom-right
    lineTo(size.width, size.height - cornerRadius)
    arcTo(
        rect = Rect(size.width - cornerRadius * 2, size.height - cornerRadius * 2, size.width, size.height),
        startAngleDegrees = 0f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
    )

    // Bottom edge to bottom-left
    lineTo(tailWidth + cornerRadius, size.height)
    arcTo(
        rect = Rect(tailWidth, size.height - cornerRadius * 2, tailWidth + cornerRadius * 2, size.height),
        startAngleDegrees = 90f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
    )

    // Left edge up to speech tail bottom
    lineTo(tailWidth, tailBottom)

    // Triangle speech tail pointing left towards the avatar
    lineTo(0f, (tailTop + tailBottom) / 2f)
    lineTo(tailWidth, tailTop)

    // Left edge up to top-left corner
    lineTo(tailWidth, cornerRadius)
    arcTo(
        rect = Rect(tailWidth, 0f, tailWidth + cornerRadius * 2, cornerRadius * 2),
        startAngleDegrees = 180f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
    )
    close()
}

// Speech cloud bubble container with conversational styling.
@Composable
fun SpeechBubbleCard(
    title: String,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
    content: @Composable () -> Unit,
) {
    val containerColor = if (isSecondary) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    }
    val titleColor = if (isSecondary) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 20.dp,
            bottomEnd = 20.dp,
            bottomStart = 20.dp,
        ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )
            content()
        }
    }
}

// Doctor persona header with an authentic speech bubble cloud pointing directly at the doctor.
@Composable
fun DoctorPersonaIntro(
    clinicalReferenceRes: Int,
    summaryRes: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Doctor Vector Avatar
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 2.dp,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(68.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_doctor_guide),
                contentDescription = stringResource(R.string.guide_title),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // True Speech Cloud Bubble with triangular tail pointing at the doctor
        Surface(
            shape = LeftSpeechBubbleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 1.dp,
            modifier = Modifier.weight(1f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Extra start padding so text stays inside the bubble body past the tail
                    .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Clinical Authority Reference Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = stringResource(clinicalReferenceRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                // Doctor's Spoken Words inside the Speech Cloud
                Text(
                    text = stringResource(summaryRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorPersonaIntroPreview() {
    HealthLogTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DoctorPersonaIntro(
                clinicalReferenceRes = R.string.protocol_weight_reference,
                summaryRes = R.string.protocol_weight_summary,
            )
        }
    }
}
