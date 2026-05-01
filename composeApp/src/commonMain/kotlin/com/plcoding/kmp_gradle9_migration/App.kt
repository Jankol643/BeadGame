package com.plcoding.kmp_gradle9_migration

import LoggerFactory
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kmpgradle9migration.composeapp.generated.resources.Res
import kmpgradle9migration.composeapp.generated.resources.arrow_downward
import kmpgradle9migration.composeapp.generated.resources.arrow_upward
import kmpgradle9migration.composeapp.generated.resources.swap_horiz
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val logger = LoggerFactory.getLogger()
const val TAG = "MyActivity"

enum class Seite { LINKS, RECHTS }

@Composable
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
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
                text = "Perlen farblich sortieren",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Initialize with 18 empty slots immediately
            val farbeL = remember { mutableStateListOf(*Array(18) { "" }) }
            val farbeR = remember { mutableStateListOf(*Array(18) { "" }) }

// Helper arrays
            val farbeLneu = remember { Array(18) { "" } }
            val farbeRneu = remember { Array(18) { "" } }

            LaunchedEffect(Unit) {
                // Only initialize if not already done (checking first slot)
                if (farbeL[0] == "") {
                    var anzahlRot = 16
                    var anzahlBlau = 16

                    // Linkes Oval initialisieren
                    for (i in 0..17) {
                        val gesamt = anzahlRot + anzahlBlau
                        if (gesamt > 0) {
                            val z = (1..gesamt).random()
                            if (z <= anzahlRot) {
                                anzahlRot--
                                farbeL[i] = "rot" // Direct index assignment
                            } else {
                                anzahlBlau--
                                farbeL[i] = "blau"
                            }
                        }
                    }

                    // Rechtes Oval initialisieren
                    for (i in 0..17) {
                        if (i in 9..12) {
                            farbeR[i] = "x"
                            continue
                        }

                        val gesamt = anzahlRot + anzahlBlau
                        if (gesamt > 0) {
                            val z = (1..gesamt).random()
                            if (z <= anzahlRot) {
                                anzahlRot--
                                farbeR[i] = "rot"
                            } else {
                                anzahlBlau--
                                farbeR[i] = "blau"
                            }
                        }
                    }
                }
            }

            // Game Board
            GameBoard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                farbeL,
                farbeR
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            GameControls(
                onMoveUp = { perlenBewegen(1, farbeL, farbeR, farbeLneu, farbeRneu) },
                onMoveDown = { perlenBewegen(-1, farbeL, farbeR, farbeLneu, farbeRneu) },
                onMove = { perlenSchieben(farbeL, farbeR, farbeLneu, farbeRneu) },
                isAnimating = isAnimating
            )

            Spacer(modifier = Modifier.height(8.dp))

        }
    }
}

private fun perlenSchieben(
    farbeL: SnapshotStateList<String>,
    farbeR: SnapshotStateList<String>,
    farbeLneu: Array<String>,
    farbeRneu: Array<String>
) {
    if (farbeL[0] == "x") {
        // rechte Perlen nach links schieben
        rechtePerlenSchieben(farbeR, farbeL)
    } else {
        // linke Perlen nach rechts schieben
        linkePerlenSchieben(farbeL, farbeR)
    }
}

@Composable
fun GameStatus() {
    //TODO: Ende, wenn
    // Get all beads

    /*

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

        Spacer(modifier = Modifier.height(8.dp))

        if (isSolved) {
            Text(
                text = "🎉 Puzzle gelöst! 🎉",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        } else {
            Text(
                text = "Ziel: Nur drei Gruppen",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }

     */
}

@Composable
fun GameBoard(
    modifier: Modifier = Modifier,
    farbeL: SnapshotStateList<String>,
    farbeR: SnapshotStateList<String>
) {

    BoxWithConstraints(modifier = modifier) {

        // Calculate positions for side-by-side ovals

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            rahmenZeichnen(canvasWidth)

            // Radius Perle
            val rP = canvasWidth / (4 * (1 / sin(15 * PI.toFloat() / 180) + 1))
            val rBog = rP / sin(15 * PI.toFloat() / 180)
            val l = 6 * rP

            // Perlen zeichnen

            // Perlen Farben setzen

            val xpl = FloatArray(18)
            val xpr = FloatArray(18)
            val yp = FloatArray(18)

            /*

            for (i in 0 .. 15) {
                farbeL[i] = 'r'
            }
            farbeL[16] = 'b'
            farbeL[17] = 'b'
            for (i in 0 .. 17) {
                if (i in 9 .. 12) {
                    farbeR[i] = 'x'
                }
                farbeR[i] = 'b'
            }
            val farbeLneu = CharArray(18)
            val farbeRneu = CharArray(18)

            for (k in 0..1000) {
                perlenMischen(farbeL, farbeR, farbeLneu, farbeRneu)
            }

             */

            // Positionen berechnen
            for (i in 0..3) {
                xpl[i] = canvasWidth / 2 - rP
                xpr[i] = xpl[i] + canvasWidth / 2
                yp[i] = canvasWidth / 2 + 3 * rP - i * 2 * rP
            }

            for (i in 9..12) {
                xpl[i] = rP
                xpr[i] = xpl[i] + canvasWidth / 2
                yp[i] = canvasWidth / 2 - 3 * rP + (i - 9) * 2 * rP
            }
            for (i in 4..8) {
                xpl[i] = (canvasWidth / 4 + rBog * cos(30 * PI / 180 * (i - 3))).toFloat()
                xpr[i] = xpl[i] + canvasWidth / 2
                yp[i] = (canvasWidth / 2 - 3 * rP - rBog * sin(30 * PI / 180 * (i - 3))).toFloat()
            }

            for (i in 13..17) {
                xpl[i] = (canvasWidth / 4 + rBog * cos((180 + (i - 12) * 30) * PI / 180)).toFloat()
                xpr[i] = xpl[i] + canvasWidth / 2
                yp[i] =
                    (canvasWidth / 2 + 3 * rP - rBog * sin((180 + (i - 12) * 30) * PI / 180)).toFloat()
            }

            // Perlen zeichnen
            for (i in 0..17) {
                if (farbeL[i] == "x") {
                    continue
                }
                drawCircle(
                    color = if (farbeL[i] == "rot") Color.Red else if (farbeL[i] == "blau") Color.Blue else Color.Gray,
                    radius = rP,
                    center = Offset(xpl[i], yp[i])
                )
            }

            for (i in 0..17) {
                if (farbeR[i] == "x") {
                    continue
                }
                drawCircle(
                    color = if (farbeR[i] == "rot") Color.Red else if (farbeR[i] == "blau") Color.Blue else Color.Gray,
                    radius = rP,
                    center = Offset(xpr[i], yp[i])
                )
            }

        }
    }
}

// linke Perlen um m Plätze gegen Uhrzeigersinn im Oval vorrücken

// New helper to handle the identical rotation logic
private fun rotieren(m: Int, neu: Array<String>, alt: MutableList<String>) {
    for (i in 0..17) {
        val j = if ((i - m) < 0) i - m + 18 else (i - m) % 18
        neu[i] = alt[j]
    }
    for (i in 0..17) {
        alt[i] = neu[i]
    }
}

private fun rechtePerlenSchieben(
    farbeR: MutableList<String>,
    farbeL: MutableList<String>
) {
    // Perlen 9,10,11,12 von rechts nach links schieben
    farbeL[0] = farbeR[12]
    farbeL[1] = farbeR[11]
    farbeL[2] = farbeR[10]
    farbeL[3] = farbeR[9]
    for (i in 9..12) farbeR[i] = "x"
}

private fun linkePerlenSchieben(
    farbeL: MutableList<String>,
    farbeR: MutableList<String>
) {
    // Perlen 0,1,2,3 von links nach rechts schieben
    farbeR[12] = farbeL[0]
    farbeR[11] = farbeL[1]
    farbeR[10] = farbeL[2]
    farbeR[9] = farbeL[3]
    for (i in 0..3) {
        farbeL[i] = "x"
    }
}

private fun perlenBewegen(
    direction: Int,
    farbeL: SnapshotStateList<String>,
    farbeR: SnapshotStateList<String>,
    farbeLneu: Array<String>,
    farbeRneu: Array<String>
) {
    if (direction == 1) { // Counter-clockwise
        if (farbeL[0] == "x") { // rechts drehen
            rotieren(1, farbeRneu, farbeR)
        } else {
            rotieren(1, farbeLneu, farbeL)
        }
    } else if (direction == -1) { // Clockwise
        if (farbeL[0] == "x") { // rechts drehen
            rotieren(-1, farbeRneu, farbeR)
        } else {
            rotieren(-1, farbeLneu, farbeL)
        }
    }
}


@Composable
fun GameControls(
    onMove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isAnimating: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Move ring beads by Z positions:", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(onClick = onMoveUp, enabled = !isAnimating) {
                Icon(
                    painter = painterResource(Res.drawable.arrow_upward),
                    contentDescription = "Hoch"
                )
            }
            Button(onClick = onMoveDown, enabled = !isAnimating) {
                Icon(
                    painter = painterResource(Res.drawable.arrow_downward),
                    contentDescription = "Runter"
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onMove,
            enabled = !isAnimating
        ) {
                Icon(
                    painter = painterResource(Res.drawable.swap_horiz),
                    contentDescription = "Links/Rechts"
                )
            Spacer(Modifier.width(8.dp))
            Text("Move")
        }

        // ... Reset Button analog mit Res.drawable.refresh
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

private fun DrawScope.kreiseZeichnen() {
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
}

private fun DrawScope.rahmenZeichnen(canvasWidth: Float) {
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