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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun Float.toRadians(): Float = this * (PI.toFloat() / 180f)
fun Double.toRadians(): Double = this * (PI / 180.0)

// Data classes for game state
data class Bead(val id: Int, val color: BeadColor, val position: BeadPosition)

enum class BeadColor { RED, BLUE }
enum class BeadPosition { C_SHAPE, RING, GREEN_FIELD }

@Composable
fun App() {
    MaterialTheme {
        var gameState by remember { mutableStateOf(GameState()) }
        var selectedZ by remember { mutableStateOf(0) }
        var isAnimating by remember { mutableStateOf(false) }

        // Handle animation completion
        LaunchedEffect(isAnimating) {
            if (isAnimating) {
                delay(500)
                isAnimating = false
            }
        }

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
                selectedZ = selectedZ,
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
                        isAnimating = true
                        gameState = gameState.moveBeads(selectedZ)
                    }
                },
                onReset = {
                    gameState = GameState()
                    selectedZ = 0
                },
                onAutoSolve = {
                    if (!isAnimating) {
                        isAnimating = true
                        gameState = gameState.autoSolve()
                    }
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
    val leftCSide = gameState.beads
        .filter { it.position == BeadPosition.C_SHAPE }
        .take(7)
    val rightCSide = gameState.beads
        .filter { it.position == BeadPosition.C_SHAPE }
        .drop(7)

    val leftRedCount = leftCSide.count { it.color == BeadColor.RED }
    val leftBlueCount = leftCSide.count { it.color == BeadColor.BLUE }
    val rightRedCount = rightCSide.count { it.color == BeadColor.RED }
    val rightBlueCount = rightCSide.count { it.color == BeadColor.BLUE }

    val isSolved = leftRedCount == 7 || leftBlueCount == 7 ||
            rightRedCount == 7 || rightBlueCount == 7

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Left C-Side", fontWeight = FontWeight.Medium)
                Text("Red: $leftRedCount, Blue: $leftBlueCount")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Right C-Side", fontWeight = FontWeight.Medium)
                Text("Red: $rightRedCount, Blue: $rightBlueCount")
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
                text = "Goal: Get all beads of one color on one C-side",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun GameBoard(
    gameState: GameState,
    selectedZ: Int,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val width = maxWidth
        val height = maxHeight

        // Draw C-shape (14 beads)
        val cBeads = gameState.beads.filter { it.position == BeadPosition.C_SHAPE }
        val ringBeads = gameState.beads.filter { it.position == BeadPosition.RING }

        // C-shape positions (semi-circle)
        val cRadius = minOf(width, height).value * 0.35f
        val cCenter = Offset(width.value * 0.5f, height.value * 0.5f)

        // Draw C-shape
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw C-shape outline
            drawArc(
                color = Color.Gray.copy(alpha = 0.3f),
                startAngle = 45f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(cCenter.x - cRadius, cCenter.y - cRadius),
                size = Size(cRadius * 2, cRadius * 2),
                style = Stroke(2.dp.toPx())
            )

            // Draw ring outline
            val ringRadius = cRadius * 1.5f
            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                radius = ringRadius,
                center = cCenter,
                style = Stroke(2.dp.toPx())
            )

            // Draw green field
            val greenFieldAngle = 0f // Top position
            val greenFieldPos = Offset(
                cCenter.x + ringRadius * cos(greenFieldAngle.toRadians()),
                cCenter.y + ringRadius * sin(greenFieldAngle.toRadians())
            )
            drawCircle(
                color = Color.Green.copy(alpha = 0.3f),
                radius = 12.dp.toPx(),
                center = greenFieldPos
            )

            // Draw C-shape beads
            cBeads.forEachIndexed { index, bead ->
                val angle = 45f + (index * 270f / 13)
                val rad = angle.toDouble().toRadians()
                val pos = Offset(
                    cCenter.x + cRadius * cos(rad).toFloat(),
                    cCenter.y + cRadius * sin(rad).toFloat()
                )

                drawCircle(
                    color = if (bead.color == BeadColor.RED) Color.Red else Color.Blue,
                    radius = 10.dp.toPx(),
                    center = pos
                )
            }

            // Draw ring beads (18 beads)
            ringBeads.forEachIndexed { index, bead ->
                val adjustedIndex = (index + selectedZ).mod(18)
                val angle = (adjustedIndex * 360f / 18)
                val rad = angle.toDouble().toRadians()
                val pos = Offset(
                    cCenter.x + ringRadius * cos(rad).toFloat(),
                    cCenter.y + ringRadius * sin(rad).toFloat()
                )

                drawCircle(
                    color = if (bead.color == BeadColor.RED) Color.Red else Color.Blue,
                    radius = 10.dp.toPx(),
                    center = pos
                )
            }

            // Draw Z indicator
            if (selectedZ != 0) {
                val indicatorAngle = (selectedZ * 360f / 18)
                val rad = indicatorAngle.toDouble().toRadians()
                val indicatorPos = Offset(
                    cCenter.x + ringRadius * 1.2f * cos(rad).toFloat(),
                    cCenter.y + ringRadius * 1.2f * sin(rad).toFloat()
                )

                drawCircle(
                    color = Color.Yellow.copy(alpha = 0.5f),
                    radius = 8.dp.toPx(),
                    center = indicatorPos
                )

                // Draw arrow from green field
                drawLine(
                    color = Color.Yellow,
                    start = greenFieldPos,
                    end = indicatorPos,
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun GameControls(
    selectedZ: Int,
    onZChanged: (Int) -> Unit,
    onMove: () -> Unit,
    onReset: () -> Unit,
    onAutoSolve: () -> Unit,
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                enabled = !isAnimating && selectedZ != 0
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

            Button(
                onClick = onAutoSolve,
                enabled = !isAnimating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("Auto Solve")
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
            Text(
                text = "4. Use 'Auto Solve' to mix and solve automatically",
                fontSize = 12.sp
            )
        }
    }
}

// GameState class with game logic
data class GameState(
    val beads: List<Bead> = initialBeads(),
    val ringOffset: Int = 0 // Current rotation of ring
) {
    companion object {
        fun initialBeads(): List<Bead> {
            val beads = mutableListOf<Bead>()
            var id = 0

            // Create 16 red beads
            repeat(16) {
                beads.add(Bead(id++, BeadColor.RED, BeadPosition.RING))
            }

            // Create 16 blue beads
            repeat(16) {
                beads.add(Bead(id++, BeadColor.BLUE, BeadPosition.RING))
            }

            // Shuffle all beads
            beads.shuffle()

            // Move 14 beads to C-shape (first 14 from shuffled list)
            for (i in 0 until 14) {
                beads[i] = beads[i].copy(position = BeadPosition.C_SHAPE)
            }

            // Remaining 18 stay in ring
            for (i in 14 until 32) {
                beads[i] = beads[i].copy(position = BeadPosition.RING)
            }

            return beads
        }
    }

    // Move beads based on Z value
    fun moveBeads(z: Int): GameState {
        if (z == 0) return this

        val newBeads = beads.toMutableList()
        val newRingOffset = (ringOffset + z).mod(18)

        // Get the indices of beads in the ring
        val ringIndices = newBeads.indices.filter { newBeads[it].position == BeadPosition.RING }

        // Apply rotation by reordering ring beads based on Z
        val ringBeads = ringIndices.map { newBeads[it] }
        val rotatedRingBeads = if (z > 0) {
            // Move beads downward (positive Z)
            ringBeads.takeLast(z) + ringBeads.dropLast(z)
        } else {
            // Move beads upward (negative Z)
            ringBeads.take(-z) + ringBeads.drop(-z)
        }

        // Update the ring beads with new order
        ringIndices.forEachIndexed { index, originalIndex ->
            newBeads[originalIndex] = rotatedRingBeads[index]
        }

        // Find indices of 4 beads near green field (positions 0, 1, 17, 16 in ring order)
        val greenFieldPositions = listOf(0, 1, 17, 16).map { pos ->
            val adjustedPos = (pos - newRingOffset).mod(18)
            ringIndices[adjustedPos]
        }

        // Move these 4 beads to C-shape
        greenFieldPositions.forEach { index ->
            newBeads[index] = newBeads[index].copy(position = BeadPosition.C_SHAPE)
        }

        // Move 4 beads from C-shape back to ring to maintain counts
        val cShapeIndices = newBeads.indices.filter { newBeads[it].position == BeadPosition.C_SHAPE }
        cShapeIndices.take(4).forEach { index ->
            newBeads[index] = newBeads[index].copy(position = BeadPosition.RING)
        }

        return copy(beads = newBeads, ringOffset = newRingOffset)
    }

    // Check if puzzle is solved
    fun isSolved(): Boolean {
        val cBeads = beads.filter { it.position == BeadPosition.C_SHAPE }

        // Split C-shape into two sides (7 beads each)
        val leftSide = cBeads.take(7)
        val rightSide = cBeads.drop(7)

        // Check if all beads on one side are same color
        val leftAllRed = leftSide.all { it.color == BeadColor.RED }
        val leftAllBlue = leftSide.all { it.color == BeadColor.BLUE }
        val rightAllRed = rightSide.all { it.color == BeadColor.RED }
        val rightAllBlue = rightSide.all { it.color == BeadColor.BLUE }

        return leftAllRed || leftAllBlue || rightAllRed || rightAllBlue
    }

    // Auto solve: mix if solved, otherwise try to solve
    fun autoSolve(): GameState {
        var currentState = this

        // If already solved, mix it up first
        if (isSolved()) {
            repeat(10) {
                val randomZ = (-8..8).random()
                if (randomZ != 0) {
                    currentState = currentState.moveBeads(randomZ)
                }
            }
        }

        // Try to solve with heuristic approach
        repeat(50) { // Limit attempts
            if (currentState.isSolved()) {
                return currentState
            }

            // Evaluate which move gets us closer to solution
            val possibleMoves = (-8..8).filter { it != 0 }
            val bestMove = possibleMoves.minByOrNull { z ->
                val nextState = currentState.moveBeads(z)
                distanceFromSolution(nextState)
            } ?: 1

            currentState = currentState.moveBeads(bestMove)
        }

        return currentState
    }

    // Helper function to calculate distance from solution
    private fun distanceFromSolution(state: GameState): Int {
        val cBeads = state.beads.filter { it.position == BeadPosition.C_SHAPE }
        val leftSide = cBeads.take(7)
        val rightSide = cBeads.drop(7)

        // Count mismatches on each side
        val leftRedCount = leftSide.count { it.color == BeadColor.RED }
        val leftBlueCount = leftSide.count { it.color == BeadColor.BLUE }
        val rightRedCount = rightSide.count { it.color == BeadColor.RED }
        val rightBlueCount = rightSide.count { it.color == BeadColor.BLUE }

        // Distance is minimum of mismatches needed for any side to be uniform
        val leftDistance = minOf(leftBlueCount, leftRedCount)
        val rightDistance = minOf(rightBlueCount, rightRedCount)

        return minOf(leftDistance, rightDistance)
    }
}

