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
import kotlin.math.sin

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
    val side: Seite,
    val index: Int,
    val layout: LayoutType? = null  // Optional: bead-specific layout
)

enum class BeadColor { RED, BLUE }
enum class Seite { LINKS, RECHTS }

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
                            val swappedState = gameState.moveBeads()
                            gameState = swappedState

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
    val leftRedCount = allBeads.count { it.side == Seite.LINKS && it.color == BeadColor.RED }
    val leftBlueCount = allBeads.count { it.side == Seite.LINKS && it.color == BeadColor.BLUE }
    val rightRedCount = allBeads.count { it.side == Seite.RECHTS && it.color == BeadColor.RED }
    val rightBlueCount = allBeads.count { it.side == Seite.RECHTS && it.color == BeadColor.BLUE }

    // DEBUG: Log the actual beads with more details
    LaunchedEffect(gameState) {
        println("DEBUG - Total beads: ${allBeads.size}")
        println("DEBUG - Left beads: ${allBeads.filter { it.side == Seite.LINKS }.size}")
        println("DEBUG - Right beads: ${allBeads.filter { it.side == Seite.RECHTS }.size}")
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

        val leftBeads = gameState.beads.filter { it.side == Seite.LINKS }
        val rightBeads = gameState.beads.filter { it.side == Seite.RECHTS }

        // Calculate positions for side-by-side ovals
        val ovalWidth = width * 0.45f
        val ovalHeight = height * 0.9f

        fun DrawScope.RahmenZeichnen(canvasWidth: Float) {
            drawLine(
                color = Color.Blue,
                start = Offset(canvasWidth * 0f, canvasWidth * 0f),
                end = Offset(canvasWidth * 1f, canvasWidth * 0f),
                strokeWidth = 2.dp.toPx()
            )

            drawLine(
                color = Color.Blue,
                start = Offset(canvasWidth * 1f, canvasWidth * 0f),
                end = Offset(canvasWidth * 1f, canvasWidth * 1f),
                strokeWidth = 2.dp.toPx()
            )

            drawLine(
                color = Color.Blue,
                start = Offset(canvasWidth * 1f, canvasWidth * 1f),
                end = Offset(canvasWidth * 0f, canvasWidth * 1f),
                strokeWidth = 2.dp.toPx()
            )

            drawLine(
                color = Color.Blue,
                start = Offset(canvasWidth * 0f, canvasWidth * 1f),
                end = Offset(canvasWidth * 0f, canvasWidth * 0f),
                strokeWidth = 2.dp.toPx()
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            /* Halbkreis nach unten
            drawArc(
                color = Color.Red,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(canvasWidth * 0f, canvasWidth * 0f),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 2.dp.toPx())
            )

             */

            /* voller kreis
            drawArc(
                color = Color.Red,
                startAngle = 180f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(canvasWidth * 0f, canvasWidth * 0f),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 2.dp.toPx())
            )
            */

            /* Halbkreis links
            drawArc(
                color = Color.Red,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(canvasWidth * 0f, canvasWidth * 0f),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 2.dp.toPx())
            )

             */

            /* Halbkreis nach oben
            drawArc(
                color = Color.Red,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(canvasWidth * 0f, canvasWidth * 0f),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 2.dp.toPx())
            )

             */

            /* Viertelkreis rechts unten
            drawArc(
                color = Color.Red,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(canvasWidth * 0f, canvasWidth * 0f),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 2.dp.toPx())
            )

             */

            /* 3/4-Kreis, Öffnung rechts oben
            drawArc(
                color = Color.Red,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(canvasWidth * 0f, canvasWidth * 0f),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 2.dp.toPx())
            )

             */

            /* Halbkreis rechts
            drawArc(
                color = Color.Red,
                startAngle = 270f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(canvasWidth * 0f, canvasWidth * 0f),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 2.dp.toPx())
            )

             */

            //RahmenZeichnen(canvasWidth)

            // Radius Perle
            val rP = canvasWidth / (4 * (1 / sin(15 * PI.toFloat() / 180) + 1))
            val rBog = rP / sin(15 * PI.toFloat() / 180)
            val l = 6*rP

            // Calculate dimensions

            val xp = FloatArray(36)
            val yp = FloatArray(36)
            for (i in 0 until 4) {
                xp[i] = canvasWidth / 2 - rP
                xp[i+18] = xp[i] + canvasWidth / 2
                yp[i] = canvasWidth / 2 + 3*rP - i * 2 * rP
                yp[i+18] = yp[i]
            }

            for (i in 9 until 13) {
                xp[i] = rP
                xp[i+18] = xp[i] + canvasWidth / 2
                yp[i] = canvasWidth / 2 - 3*rP + (i-9) * 2 * rP
                yp[i+18] = canvasWidth - 3*rP + (i-18) * 2 * rP
            }
            for (i in 4 until 9) {
                xp[i] = (canvasWidth / 4 + rBog * cos(30 * PI / 180 * (i-3))).toFloat()
                xp[i+18] = (canvasWidth / 4 * 3  + rBog * cos(30 * PI / 180 * (i-3-18))).toFloat()
                yp[i] = (canvasWidth / 2 - 3*rP - rBog * sin(30 * PI / 180 * (i-3))).toFloat()
                yp[i+18] = (canvasWidth / 2 + 3*rP - rBog * sin(30 * PI / 180 * (i-3-18))).toFloat()
            }

            for (i in 13 until 18) {
                xp[i] = (canvasWidth / 4 + rBog * cos((180 + (i-12) * 30) * PI / 180 )).toFloat()
                xp[i+18] = (canvasWidth / 4 * 3 + rBog * cos((180 + (i-12-18) * 30) * PI / 180 )).toFloat()
                yp[i] = (canvasWidth / 2 + 3*rP - rBog * sin((180 + (i-12) * 30) * PI / 180 )).toFloat()
                yp[i+18] = (canvasWidth / 2 + 3*rP - rBog * sin((180 + (i-12-18) * 30) * PI / 180 )).toFloat()
            }

            // Perlen 0-15 rot, andere blau
            for (i in 0 until 35) {
                if (i in 27..30) { //freie Stellen
                    continue;
                }
                drawCircle(
                    color = if (i < 16) Color.Red else Color.Blue,
                    radius = rP,
                    center = Offset(xp[i], yp[i])
                )
            }



        }
    }
}

// Helper to draw a stadium shape (Straight sides, rounded ends)
private fun DrawScope.drawOval(
    seite: Seite,
    rP: Float,
    rBog: Float,
    l: Float
) {
    val canvasWidth = size.width
    val canvasHeight = size.height

    val x1 = rP
    val y1 = canvasWidth / 2 - l / 2 - rBog

    var add = 0f
    if (seite == Seite.RECHTS) {
        add = canvasWidth / 2
    }

    // Kreis oben
    drawArc(
        color = Color.Red,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(x1 + add, y1),
        size = Size(rBog * 2, rBog * 2),
        style = Stroke(width = 2.dp.toPx())
    )

    drawLine(
        color = Color.Red,
        start = Offset(x1 + add, canvasWidth / 2 - l / 2),
        end = Offset(x1 + add, canvasWidth / 2 + l / 2),
        strokeWidth = 2.dp.toPx()
    )

    drawLine(
        color = Color.Red,
        start = Offset(canvasWidth / 2 - rP + add, canvasWidth / 2 - l / 2),
        end = Offset(canvasWidth / 2 - rP + add, canvasWidth / 2 + l / 2),
        strokeWidth = 2.dp.toPx()
    )

    // Kreis unten
    drawArc(
        color = Color.Red,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(x1 + add, canvasWidth / 2 + l / 2 - rBog),
        size = Size(rBog * 2, rBog * 2),
        style = Stroke(width = 2.dp.toPx())
    )

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
    val ringSide = if (rightSideLayout == LayoutType.RING) Seite.RECHTS else Seite.LINKS

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
    val leftBeads = beads.count { it.side == Seite.LINKS }
    val rightBeads = beads.count { it.side == Seite.RECHTS }

    val (ringSide, cSide) = when {
        leftBeads == 18 && rightBeads == 14 -> Pair(Seite.LINKS, Seite.RECHTS)
        rightBeads == 18 && leftBeads == 14 -> Pair(Seite.RECHTS, Seite.LINKS)
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

    // Move beads to c-side - place them at the beginning of the straight section (positions 5-8)
    beadsToMove.forEachIndexed { i, (originalIndex, bead) ->
        newBeads[originalIndex] = bead.copy(
            side = cSide,
            index = 5 + i // Place at positions 5, 6, 7, 8 (straight section of C-shape)
        )
    }

    // Reindex remaining ring side beads: shift remaining beads down to fill the gap
    val remainingRingSideBeads = ringSideBeads
        .filter { (_, bead) -> bead.index !in 0..3 }
        .sortedBy { (_, bead) -> bead.index }

    remainingRingSideBeads.forEachIndexed { i, (originalIndex, bead) ->
        newBeads[originalIndex] = bead.copy(
            side = ringSide,
            index = i // 0-13 (14 beads remain on ring side)
        )
    }

    // Reindex ALL c-side beads (original + moved) - maintain C-shape layout
    // C-shape layout: 0-4 bottom arc, 5-9 straight, 10-13 top arc
    val allCSideBeads = newBeads
        .withIndex()
        .filter { (_, bead) -> bead.side == cSide }
        .sortedBy { (_, bead) -> bead.index }

    // Reindex to maintain proper C-shape ordering
    allCSideBeads.forEachIndexed { i, (originalIndex, bead) ->
        // Determine new index based on C-shape layout
        val newIndex = when {
            i < 5 -> i // Bottom arc: 0-4
            i < 14 -> i // Straight + moved beads: 5-13
            else -> i // Top arc: 14-17
        }
        newBeads[originalIndex] = bead.copy(
            side = cSide,
            index = newIndex
        )
    }

    // Swap sides: c-side becomes ring side and vice versa
    return copy(
        beads = newBeads,
        leftSideLayout = if (ringSide == Seite.LINKS) LayoutType.C_SHAPE else LayoutType.RING,
        rightSideLayout = if (ringSide == Seite.LINKS) LayoutType.RING else LayoutType.C_SHAPE
    )
}

val initialGameState = GameState(
    beads = listOf(
        // Left side beads (C-shape, indices 0-13)
        Bead(1, BeadColor.RED, Seite.LINKS, 0),
        Bead(2, BeadColor.BLUE, Seite.LINKS, 1),
        Bead(3, BeadColor.RED, Seite.LINKS, 2),
        Bead(4, BeadColor.BLUE, Seite.LINKS, 3),
        Bead(5, BeadColor.RED, Seite.LINKS, 4),
        Bead(6, BeadColor.BLUE, Seite.LINKS, 5),
        Bead(7, BeadColor.RED, Seite.LINKS, 6),
        Bead(8, BeadColor.BLUE, Seite.LINKS, 7),
        Bead(9, BeadColor.RED, Seite.LINKS, 8),
        Bead(10, BeadColor.BLUE, Seite.LINKS, 9),
        Bead(11, BeadColor.RED, Seite.LINKS, 10),
        Bead(12, BeadColor.BLUE, Seite.LINKS, 11),
        Bead(13, BeadColor.RED, Seite.LINKS, 12),
        Bead(14, BeadColor.BLUE, Seite.LINKS, 13),

        // Right side beads (Ring, indices 0-17)
        Bead(15, BeadColor.RED, Seite.RECHTS, 0),
        Bead(16, BeadColor.BLUE, Seite.RECHTS, 1),
        Bead(17, BeadColor.RED, Seite.RECHTS, 2),
        Bead(18, BeadColor.BLUE, Seite.RECHTS, 3),
        Bead(19, BeadColor.RED, Seite.RECHTS, 4),
        Bead(20, BeadColor.BLUE, Seite.RECHTS, 5),
        Bead(21, BeadColor.RED, Seite.RECHTS, 6),
        Bead(22, BeadColor.BLUE, Seite.RECHTS, 7),
        Bead(23, BeadColor.RED, Seite.RECHTS, 8),
        Bead(24, BeadColor.BLUE, Seite.RECHTS, 9),
        Bead(25, BeadColor.RED, Seite.RECHTS, 10),
        Bead(26, BeadColor.BLUE, Seite.RECHTS, 11),
        Bead(27, BeadColor.RED, Seite.RECHTS, 12),
        Bead(28, BeadColor.BLUE, Seite.RECHTS, 13),
        Bead(29, BeadColor.RED, Seite.RECHTS, 14),
        Bead(30, BeadColor.BLUE, Seite.RECHTS, 15),
        Bead(31, BeadColor.RED, Seite.RECHTS, 16),
        Bead(32, BeadColor.BLUE, Seite.RECHTS, 17),
    ),
    leftSideLayout = LayoutType.C_SHAPE,
    rightSideLayout = LayoutType.RING
)