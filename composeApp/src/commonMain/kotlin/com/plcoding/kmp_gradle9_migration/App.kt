package com.plcoding.kmp_gradle9_migration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.abs

val logger = LoggerFactory.getLogger()
const val TAG = "MyActivity"

// Updated data class to include layout type
data class GameState(
    val beads: List<Bead>,
    val leftSideLayout: LayoutType = LayoutType.C_SHAPE,  // Default left to C-shape
    val rightSideLayout: LayoutType = LayoutType.RING,     // Default right to ring
    val ringOffset: Int = 0
)

enum class LayoutType {
    C_SHAPE,  // 14 beads
    RING      // 18 beads
}

data class Bead(
    val id: Int,
    val color: BeadColor,
    val side: BeadSide,
    val index: Int,
    val layout: LayoutType? = null  // Optional: bead-specific layout
)

enum class BeadColor { RED, BLUE }
enum class BeadSide { LEFT, RIGHT }

@Composable
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        var gameState by remember { mutableStateOf(initialGameState) }
        var selectedZ by remember { mutableStateOf(0) }
        var isAnimating by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Game Title
            Text(
                text = "Bead Separation Game",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Status Display
            GameStatus(gameState)

            Spacer(modifier = Modifier.height(16.dp))

            // Game Board
            GameBoard(
                gameState = gameState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            GameControls(
                selectedZ = selectedZ,
                onZChanged = { selectedZ = it },
                onMove = {
                    if (!isAnimating) {
                        scope.launch {
                            isAnimating = true

                            // 1. Rotate the "Ring" beads - CREATE A NEW STATE
                            val rotatedState = gameState.rotateRing(selectedZ)
                            gameState = rotatedState

                            // 2. Wait 2 seconds
                            delay(2000)

                            // 3. Swap the 4 beads on the straight edges - CREATE ANOTHER NEW STATE
                            // val swappedState = gameState.moveBeads()
                            // gameState = swappedState

                            isAnimating = false
                        }
                    }
                },
                onReset = {
                    gameState = initialGameState
                    selectedZ = 0
                },
                isAnimating = isAnimating
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Instructions
            Instructions()
        }
    }
}

@Composable
fun GameStatus(gameState: GameState) {
    // Get all beads
    val allBeads = gameState.beads

    // Count by side AND color
    val leftRedCount = allBeads.count { it.side == BeadSide.LEFT && it.color == BeadColor.RED }
    val leftBlueCount = allBeads.count { it.side == BeadSide.LEFT && it.color == BeadColor.BLUE }
    val rightRedCount = allBeads.count { it.side == BeadSide.RIGHT && it.color == BeadColor.RED }
    val rightBlueCount = allBeads.count { it.side == BeadSide.RIGHT && it.color == BeadColor.BLUE }

    // DEBUG: Log the actual beads with more details
    LaunchedEffect(gameState) {
        println("DEBUG - Total beads: ${allBeads.size}")
        println("DEBUG - Left beads: ${allBeads.filter { it.side == BeadSide.LEFT }.size}")
        println("DEBUG - Right beads: ${allBeads.filter { it.side == BeadSide.RIGHT }.size}")
        println("DEBUG - Left: R=$leftRedCount, B=$leftBlueCount")
        println("DEBUG - Right: R=$rightRedCount, B=$rightBlueCount")

        // Log all beads for verification
        allBeads.forEach { bead ->
            println("Bead ${bead.id}: ${bead.color} on ${bead.side} at index ${bead.index}")
        }
    }

    // Update the solved condition based on your actual bead distribution
    // Left has 14 beads, Right has 18 beads
    val isLeftSolved = (leftRedCount == 14 && leftBlueCount == 0) ||
            (leftBlueCount == 14 && leftRedCount == 0)
    val isRightSolved = (rightRedCount == 18 && rightBlueCount == 0) ||
            (rightBlueCount == 18 && rightRedCount == 0)

    val isSolved = isLeftSolved || isRightSolved

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("C-Shape (Left)", fontWeight = FontWeight.Medium)
                Text("Red: $leftRedCount, Blue: $leftBlueCount")
                Text("Total: ${leftRedCount + leftBlueCount} / 14", fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ring (Right)", fontWeight = FontWeight.Medium)
                Text("Red: $rightRedCount, Blue: $rightBlueCount")
                Text("Total: ${rightRedCount + rightBlueCount} / 18", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isSolved) {
            Text(
                text = "🎉 Puzzle Solved! 🎉",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        } else {
            Text(
                text = "Goal: Get all beads of one color on one side",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            // Add more specific instructions
            Text(
                text = "C-Shape: 14 beads, Ring: 18 beads",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun GameBoard(
    gameState: GameState,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val width = maxWidth.value
        val height = maxHeight.value
        val textMeasurer = rememberTextMeasurer()

        val leftBeads = gameState.beads.filter { it.side == BeadSide.LEFT }
        val rightBeads = gameState.beads.filter { it.side == BeadSide.RIGHT }

        // Calculate positions for side-by-side ovals
        val ovalWidth = width * 0.45f
        val ovalHeight = height * 0.9f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate dimensions
            val beadDiameter = canvasHeight * 0.12f
            val straightSectionHeight = beadDiameter * 4f
            val arcRadius = canvasWidth * 0.15f

            val leftCenter = Offset(canvasWidth * 0.3f, canvasHeight / 2)
            val rightCenter = Offset(canvasWidth * 0.7f, canvasHeight / 2)

            // Draw the Stadium Shapes
            drawStadium(leftCenter, straightSectionHeight, arcRadius, Color.LightGray)
            drawStadium(rightCenter, straightSectionHeight, arcRadius, Color.LightGray)

            // Draw Left side beads
            leftBeads.forEach { bead ->
                val beadCount = if (gameState.leftSideLayout == LayoutType.C_SHAPE) 14 else 18
                val pos = getBeadPositionOnStadium(
                    index = bead.index,
                    center = leftCenter,
                    straightHeight = straightSectionHeight,
                    radius = arcRadius,
                    beadCount = beadCount
                )

                drawBead(bead, pos, textMeasurer)
            }

            // Draw Right side beads
            rightBeads.forEach { bead ->
                val beadCount = if (gameState.rightSideLayout == LayoutType.C_SHAPE) 14 else 18
                val pos = getBeadPositionOnStadium(
                    index = bead.index,
                    center = rightCenter,
                    straightHeight = straightSectionHeight,
                    radius = arcRadius,
                    beadCount = beadCount
                )

                drawBead(bead, pos, textMeasurer)
            }

            // Draw labels
            val fontSize = min(16.sp.toPx(), height * 0.04f)
            val fontSizeSp = fontSize.toSp()

            // Left label - dynamic based on layout
            val leftLabel = if (gameState.leftSideLayout == LayoutType.C_SHAPE) "C-Shape" else "Ring"
            drawText(
                textMeasurer = textMeasurer,
                text = leftLabel,
                topLeft = Offset(
                    leftCenter.x - ovalWidth * 0.2f,
                    leftCenter.y - ovalHeight / 2 - height * 0.05f
                ),
                style = TextStyle(
                    color = Color.Gray,
                    fontSize = fontSizeSp
                )
            )

            // Right label - dynamic based on layout
            val rightLabel = if (gameState.rightSideLayout == LayoutType.C_SHAPE) "C-Shape" else "Ring"
            drawText(
                textMeasurer = textMeasurer,
                text = rightLabel,
                topLeft = Offset(
                    rightCenter.x - ovalWidth * 0.15f,
                    rightCenter.y - ovalHeight / 2 - height * 0.05f
                ),
                style = TextStyle(
                    color = Color.Gray,
                    fontSize = fontSizeSp
                )
            )
        }
    }
}

// Helper function to draw a bead with its index
private fun DrawScope.drawBead(
    bead: Bead,
    position: Offset,
    textMeasurer: TextMeasurer
) {
    // Draw the bead
    drawCircle(
        color = if (bead.color == BeadColor.RED) Color.Red else Color.Blue,
        radius = 10.dp.toPx(),
        center = position
    )

    // Draw the index text
    val textLayoutResult = textMeasurer.measure(
        text = bead.index.toString(),
        style = TextStyle(color = Color.White, fontSize = 10.sp)
    )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            x = position.x - textLayoutResult.size.width / 2f,
            y = position.y - textLayoutResult.size.height / 2f
        )
    )
}

// Helper to draw a stadium shape (Straight sides, rounded ends)
private fun DrawScope.drawStadium(
    center: Offset,
    straightHeight: Float,
    radius: Float,
    color: Color
) {
    val topY = center.y - straightHeight / 2
    val bottomY = center.y + straightHeight / 2

    // Draw left straight line
    drawLine(
        color = color,
        start = Offset(center.x - radius, topY),
        end = Offset(center.x - radius, bottomY),
        strokeWidth = 2.dp.toPx()
    )

    // Draw right straight line
    drawLine(
        color = color,
        start = Offset(center.x + radius, topY),
        end = Offset(center.x + radius, bottomY),
        strokeWidth = 2.dp.toPx()
    )

    // Draw top arc
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius, topY - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 2.dp.toPx())
    )

    // Draw bottom arc
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius, bottomY - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 2.dp.toPx())
    )
}

fun getBeadPositionOnStadium(
    index: Int,
    center: Offset,
    straightHeight: Float,
    radius: Float,
    beadCount: Int  // New parameter: 14 for C-shape, 18 for ring
): Offset {
    val topY = center.y - straightHeight / 2
    val bottomY = center.y + straightHeight / 2

    return if (beadCount == 14) {
        // C-shape layout (14 beads)
        when (index) {
            in 0..4 -> {
                // Bottom Arc (0-4) - 5 beads, going from right to left
                val beadCountArc = 5
                val angleIncrement = PI.toFloat() / (beadCountArc - 1) // 45 degrees
                val angle = index * angleIncrement // from 0 to π
                Offset(center.x + radius * cos(angle), bottomY + radius * sin(angle))
            }
            in 5..9 -> {
                // Left Straight (5-9) - 5 beads, going from bottom to top
                val beadCountStraight = 5
                val spacing = straightHeight / (beadCountStraight + 1)
                val position = (index - 5 + 1) * spacing
                val y = bottomY - position
                Offset(center.x - radius, y)
            }
            in 10..13 -> {
                // Top Arc (10-13) - 4 beads, going from left to right
                val beadCountArc = 4
                val angleIncrement = PI.toFloat() / (beadCountArc - 1) // 60 degrees
                val angle = PI.toFloat() + ((index - 10) * angleIncrement)
                Offset(center.x + radius * cos(angle), topY + radius * sin(angle))
            }
            else -> Offset(center.x, center.y)
        }
    } else {
        // Ring layout (18 beads)
        when (index) {
            in 0..3 -> {
                // Left Straight (0-3) - bottom to top
                val beadCountStraight = 4
                val progress = index.toFloat() / (beadCountStraight - 1)
                val adjustedProgress = 0.05f + progress * 0.85f
                val y = bottomY - (adjustedProgress * straightHeight)
                Offset(center.x - radius, y)
            }
            in 4..7 -> {
                // Top Arc (4-7) - left to right
                val beadCountArc = 4
                val progress = (index - 4).toFloat() / (beadCountArc - 1)
                val adjustedProgress = 0.1f + progress * 0.8f
                val angle = PI.toFloat() * (1 + adjustedProgress)
                Offset(center.x + radius * cos(angle), topY + radius * sin(angle))
            }
            in 8..11 -> {
                // Right Straight (8-11) - top to bottom
                val beadCountStraight = 4
                val progress = (index - 8).toFloat() / (beadCountStraight - 1)
                val adjustedProgress = 0.1f + progress * 0.85f
                val y = topY + (adjustedProgress * straightHeight)
                Offset(center.x + radius, y)
            }
            in 12..17 -> {
                // Bottom Arc (12-17) - left to right
                val beadCountArc = 6
                val progress = (index - 12).toFloat() / (beadCountArc - 1)
                val adjustedProgress = 0.05f + progress * 0.9f
                val angle = PI.toFloat() * adjustedProgress
                Offset(center.x + radius * cos(angle), bottomY + radius * sin(angle))
            }
            else -> Offset(center.x, center.y)
        }
    }
}

@Composable
fun GameControls(
    selectedZ: Int,
    onZChanged: (Int) -> Unit,
    onMove: () -> Unit,
    onReset: () -> Unit,
    isAnimating: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Z selector
        Text("Move ring beads by Z positions:", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onZChanged((selectedZ - 1).coerceAtLeast(-8)) },
                enabled = !isAnimating
            ) {
                Text("-")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Z = $selectedZ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = { onZChanged((selectedZ + 1).coerceAtMost(8)) },
                enabled = !isAnimating
            ) {
                Text("+")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onMove,
                enabled = !isAnimating
            ) {
                Text("Move Beads")
            }

            Button(
                onClick = onReset,
                enabled = !isAnimating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Reset")
            }
        }
    }
}

@Composable
fun Instructions() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "How to Play:",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "1. Select Z value (how many positions to move ring beads)",
                fontSize = 12.sp
            )
            Text(
                text = "2. Click 'Move Beads' to move ring beads and transfer 4 beads near green field to C-shape",
                fontSize = 12.sp
            )
            Text(
                text = "3. Goal: Get all 7 beads on one side of C to be same color",
                fontSize = 12.sp
            )
        }
    }
}

// Move beads based on Z value
fun GameState.rotateRing(z: Int): GameState {
    if (z == 0) return this.copy()

    // 1. Identify which side is the ring (based on layout)
    val ringSide = if (rightSideLayout == LayoutType.RING) BeadSide.RIGHT else BeadSide.LEFT

    // 2. Filter beads on the ring side
    val ringBeadsWithIndices = beads.mapIndexed { index, bead -> index to bead }
        .filter { it.second.side == ringSide }

    if (ringBeadsWithIndices.isEmpty()) return this.copy()

    val ringBeads = ringBeadsWithIndices.map { it.second }
    val ringSize = ringBeads.size

    // 3. Calculate effective rotation
    val effectiveShift = z.mod(ringSize)

    // 4. Apply rotation (positive z rotates forward, negative backward)
    val rotatedBeads = if (effectiveShift > 0) {
        ringBeads.drop(ringSize - effectiveShift) + ringBeads.take(ringSize - effectiveShift)
    } else {
        ringBeads.drop(-effectiveShift) + ringBeads.take(-effectiveShift)
    }

    // 5. Update beads list
    val newBeads = beads.toMutableList()
    ringBeadsWithIndices.forEachIndexed { i, (originalIdx, _) ->
        val rotatedBead = rotatedBeads[i]
        // Update bead with correct index position
        newBeads[originalIdx] = rotatedBead.copy(
            index = i // Update index to reflect new position
        )
    }

    // 6. Return new state
    return this.copy(
        beads = newBeads,
        ringOffset = (ringOffset + z).mod(ringSize)
    )
}

private fun GameState.moveBeads(): GameState {
    val newBeads = beads.toMutableList()

    // Determine which side has 18 beads (ring side) and which has 14 (c-side)
    val leftBeads = beads.count { it.side == BeadSide.LEFT }
    val rightBeads = beads.count { it.side == BeadSide.RIGHT }

    val (ringSide, cSide) = when {
        leftBeads == 18 && rightBeads == 14 -> Pair(BeadSide.LEFT, BeadSide.RIGHT)
        rightBeads == 18 && leftBeads == 14 -> Pair(BeadSide.RIGHT, BeadSide.LEFT)
        else -> return this.copy() // Invalid state, return unchanged
    }

    // Get all beads on each side
    val ringSideBeads = beads
        .withIndex()
        .filter { (_, bead) -> bead.side == ringSide }
        .sortedBy { (_, bead) -> bead.index }

    val cSideBeads = beads
        .withIndex()
        .filter { (_, bead) -> bead.side == cSide }
        .sortedBy { (_, bead) -> bead.index }

    // Move beads from indices 0, 1, 2, 3 on ring side to c-side
    val beadsToMove = ringSideBeads
        .filter { (_, bead) -> bead.index in 0..3 }
        .take(4)

    if (beadsToMove.size < 4) {
        return this.copy()
    }

    // Move beads to c-side
    beadsToMove.forEachIndexed { i, (originalIndex, bead) ->
        newBeads[originalIndex] = bead.copy(
            side = cSide,
            index = cSideBeads.size + i // Start after existing c-side beads
        )
    }

    // Reindex remaining ring side beads: they should now have indices 0-13
    val remainingRingSideBeads = ringSideBeads
        .filter { (_, bead) -> bead.index !in 0..3 }
        .sortedBy { (_, bead) -> bead.index }

    remainingRingSideBeads.forEachIndexed { newIndex, (originalIndex, bead) ->
        newBeads[originalIndex] = bead.copy(
            side = ringSide,
            index = newIndex // 0-13
        )
    }

    // Reindex ALL c-side beads (original + moved) to 0-17
    val allCSideBeads = newBeads
        .withIndex()
        .filter { (_, bead) -> bead.side == cSide }
        .sortedBy { (_, bead) -> bead.index }

    allCSideBeads.forEachIndexed { newIndex, (originalIndex, bead) ->
        newBeads[originalIndex] = bead.copy(
            side = cSide,
            index = newIndex // 0-17 for all c-side beads
        )
    }

    // Swap sides: c-side becomes ring side and vice versa
    return copy(
        beads = newBeads,
        leftSideLayout = if (ringSide == BeadSide.LEFT) LayoutType.C_SHAPE else LayoutType.RING,
        rightSideLayout = if (ringSide == BeadSide.LEFT) LayoutType.RING else LayoutType.C_SHAPE
    )
}

val initialGameState = GameState(
    beads = listOf(
        // Left side beads (C-shape, indices 0-13)
        Bead(1, BeadColor.RED, BeadSide.LEFT, 0),
        Bead(2, BeadColor.BLUE, BeadSide.LEFT, 1),
        Bead(3, BeadColor.RED, BeadSide.LEFT, 2),
        Bead(4, BeadColor.BLUE, BeadSide.LEFT, 3),
        Bead(5, BeadColor.RED, BeadSide.LEFT, 4),
        Bead(6, BeadColor.BLUE, BeadSide.LEFT, 5),
        Bead(7, BeadColor.RED, BeadSide.LEFT, 6),
        Bead(8, BeadColor.BLUE, BeadSide.LEFT, 7),
        Bead(9, BeadColor.RED, BeadSide.LEFT, 8),
        Bead(10, BeadColor.BLUE, BeadSide.LEFT, 9),
        Bead(11, BeadColor.RED, BeadSide.LEFT, 10),
        Bead(12, BeadColor.BLUE, BeadSide.LEFT, 11),
        Bead(13, BeadColor.RED, BeadSide.LEFT, 12),
        Bead(14, BeadColor.BLUE, BeadSide.LEFT, 13),

        // Right side beads (Ring, indices 0-17)
        Bead(15, BeadColor.RED, BeadSide.RIGHT, 0),
        Bead(16, BeadColor.BLUE, BeadSide.RIGHT, 1),
        Bead(17, BeadColor.RED, BeadSide.RIGHT, 2),
        Bead(18, BeadColor.BLUE, BeadSide.RIGHT, 3),
        Bead(19, BeadColor.RED, BeadSide.RIGHT, 4),
        Bead(20, BeadColor.BLUE, BeadSide.RIGHT, 5),
        Bead(21, BeadColor.RED, BeadSide.RIGHT, 6),
        Bead(22, BeadColor.BLUE, BeadSide.RIGHT, 7),
        Bead(23, BeadColor.RED, BeadSide.RIGHT, 8),
        Bead(24, BeadColor.BLUE, BeadSide.RIGHT, 9),
        Bead(25, BeadColor.RED, BeadSide.RIGHT, 10),
        Bead(26, BeadColor.BLUE, BeadSide.RIGHT, 11),
        Bead(27, BeadColor.RED, BeadSide.RIGHT, 12),
        Bead(28, BeadColor.BLUE, BeadSide.RIGHT, 13),
        Bead(29, BeadColor.RED, BeadSide.RIGHT, 14),
        Bead(30, BeadColor.BLUE, BeadSide.RIGHT, 15),
        Bead(31, BeadColor.RED, BeadSide.RIGHT, 16),
        Bead(32, BeadColor.BLUE, BeadSide.RIGHT, 17),
    ),
    leftSideLayout = LayoutType.C_SHAPE,
    rightSideLayout = LayoutType.RING
)