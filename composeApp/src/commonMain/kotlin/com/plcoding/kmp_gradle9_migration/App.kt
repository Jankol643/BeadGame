package com.plcoding.kmp_gradle9_migration

import LoggerFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Clock
import kotlin.time.Duration
import androidx.compose.material3.*

val logger = LoggerFactory.getLogger()
const val TAG = "MyActivity"

enum class Seite { LINKS, RECHTS }

// Added the missing data classes
data class CsvRow(val tokens: List<String>)
data class Werte(
    val ausgabe: String,
    val transferCountBest: Int,
    val upDownCountBest: Int,
    val spielNr: Int,
    val farbeL: List<String>,
    val farbeR: List<String>
)

expect class AssetFileReader() {
    suspend fun readAssetFile(fileName: String): String
    suspend fun writeAssetFile(fileName: String, content: String)
}

@Composable
fun App(onErrorCaught: ((Throwable) -> Unit)? = null) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        var isAnimating by remember { mutableStateOf(false) }
        var noGroups by remember { mutableStateOf(0) }

        var farbeLGame by remember { mutableStateOf(MutableList(18) { "" }) }
        var farbeRGame by remember { mutableStateOf(MutableList(18) { "" }) }
        var farbeL by remember { mutableStateOf(MutableList(18) { "" }) }
        var farbeR by remember { mutableStateOf(MutableList(18) { "" }) }

        var transferCount by remember { mutableStateOf(0) }
        var upDownCount by remember { mutableStateOf(0) }
        var transferCountBest by remember { mutableStateOf(666) }
        var upDownCountBest by remember { mutableStateOf(666) }
        var loesung by remember { mutableStateOf("") }
        var spielNr by remember { mutableStateOf(-1) }

        // Helper arrays (if required by your perlenBewegen function)
        val farbeLneu = remember { Array(18) { "" } }
        val farbeRneu = remember { Array(18) { "" } }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Column (
                modifier = Modifier
                    .fillMaxWidth()
                .weight(0.25f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Game Title
                Text(
                    text = "Sort beads",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )
                Text(
                    text = "till only three groups are left",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text("No of groups: $noGroups", fontSize = 18.sp)
                Text("No of transfers: $transferCount, No of Up/Down: $upDownCount", fontSize = 16.sp)
                Text("best: $transferCountBest, best: $upDownCountBest", fontSize = 18.sp)
                Text(loesung, fontSize = 18.sp)
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                         onClick = {
                             try {
                                 upDownCount++
                                 perlenBewegen(1, farbeL, farbeR, farbeLneu, farbeRneu)
                             } catch(t: Throwable) {
                                 onErrorCaught?.invoke(t)
                             }
                         },
                        enabled = !isAnimating,
                        contentPadding = PaddingValues(horizontal = 35.dp)
                    ) {
                        Text("Up 1 bead", fontSize = 16.sp)
                    }
                }
            }

            LaunchedEffect(Unit) {
                try {
                    val ergebnis = tabelleEinlesen()

                    farbeL.clear()
                    farbeL.addAll(ergebnis.farbeL)
                    farbeL = farbeL.toMutableList()

                    farbeLGame.clear()
                    farbeLGame.addAll(ergebnis.farbeL)
                    farbeLGame = farbeLGame.toMutableList()

                    farbeR.clear()
                    farbeR.addAll(ergebnis.farbeR)
                    farbeR = farbeR.toMutableList()

                    farbeRGame.clear()
                    farbeRGame.addAll(ergebnis.farbeR)
                    farbeRGame = farbeRGame.toMutableList()

                    loesung = ergebnis.ausgabe
                    transferCountBest = ergebnis.transferCountBest
                    upDownCountBest = ergebnis.upDownCountBest
                    spielNr = ergebnis.spielNr

                    noGroups = colorGroups(farbeL, farbeR)
                } catch (t: Throwable) {
                    // Forward the crash directly up to the MainActivity central error shield
                    if (onErrorCaught != null) {
                        onErrorCaught(t)
                    } else {
                        throw t // Rethrow so the uncaught exception handler intercepts it
                    }
                }
            }

            suspend fun newGame() {
                val ergebnis = tabelleEinlesen()

                farbeL.clear()
                farbeL.addAll(ergebnis.farbeL)
                farbeL = farbeL.toMutableList()

                farbeLGame.clear()
                farbeLGame.addAll(ergebnis.farbeL)
                farbeLGame = farbeLGame.toMutableList()

                farbeR.clear()
                farbeR.addAll(ergebnis.farbeR)
                farbeR = farbeR.toMutableList()

                farbeRGame.clear()
                farbeRGame.addAll(ergebnis.farbeR)
                farbeRGame = farbeRGame.toMutableList()

                loesung = ergebnis.ausgabe
                transferCountBest = ergebnis.transferCountBest
                upDownCountBest = ergebnis.upDownCountBest
                spielNr = ergebnis.spielNr

                noGroups = colorGroups(farbeL, farbeR)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f),
            ) {
                // Game Board
                GameBoard(
                    farbeL,
                    farbeR
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .weight(0.25f),
            ) {
                GameControls(
                    onMoveDown = { upDownCount++; perlenBewegen(-1, farbeL, farbeR, farbeLneu, farbeRneu) },
                    onMove = {
                        transferCount++
                        noGroups = perlenSchieben(farbeL, farbeR)
                        if (noGroups == 3 && transferCount == transferCountBest && upDownCount < upDownCountBest) {
                            upDownCountBest = upDownCount
                        }
                        if ((noGroups == 3) && transferCount < transferCountBest) {
                            transferCountBest = transferCount
                            upDownCountBest = upDownCount
                            scope.launch {
                                tabelleSchreiben(
                                    spielNr = spielNr,
                                    transferCountBest = transferCountBest,
                                    upDownCountBest = upDownCountBest
                                )
                            }
                        }
                    },
                    onNewGame = {
                        upDownCount = 0
                        upDownCountBest = 999
                        transferCount = 0
                        transferCountBest = 999

                        // HIER DIE KORREKTUR:
                        scope.launch {
                            newGame()
                        }
                    },
                    replay = { upDownCount = 0; transferCount = 0; noGroups = replay(farbeL, farbeLGame, farbeR, farbeRGame) },
                    isAnimating = isAnimating,
                )

            }
        }
    }
}

suspend fun tabelleSchreiben(
    spielNr: Int,
    transferCountBest: Int,
    upDownCountBest: Int,
    onErrorCaught: ((Throwable) -> Unit)? = null
) {
    val fileName = "Perlen_Musterlösungen.csv"

    try {
        val reader = AssetFileReader()
        // 1. Reads from internal storage mirror (or automatically extracts it from APK if first launch)
        val csvContent = reader.readAssetFile(fileName)
        val lines = csvContent.lines().toMutableList()

        if (lines.isEmpty()) {
            throw IllegalStateException("$fileName contents are empty or unreadable.")
        }

        if (spielNr < lines.size && lines[spielNr].trim().isNotEmpty()) {
            val tokens = lines[spielNr].split(",").map { it.trim() }.toMutableList()

                // Modify the specific high score columns
                tokens[0] = transferCountBest.toString()
                tokens[1] = upDownCountBest.toString()

                lines[spielNr] = tokens.joinToString(",")
        } else {
            throw IndexOutOfBoundsException("Game number index '$spielNr' maps out of valid bounds.")
        }

        // 2. Format and commit directly back using the updated AssetFileReader write method
        val neuerInhalt = lines.joinToString("\n")
        reader.writeAssetFile(fileName, neuerInhalt)

    } catch (t: Throwable) {
        // 3. Forward everything directly up to the UI shield hierarchy
        if (onErrorCaught != null) {
            onErrorCaught(t)
        } else {
            throw t
        }
    }
}

private fun replay(farbeL: MutableList<String>, farbeLGame: MutableList<String>, farbeR: MutableList<String>, farbeRGame: MutableList<String>): Int {
    for (i in 0..17) {
        farbeL[i] = farbeLGame[i]
    }
    for (i in 0..17) {
        if (i in 7..10) {
            farbeRGame[i] = "x"
        }
        farbeR[i] = farbeRGame[i]
    }
    return colorGroups(farbeL, farbeR)
}

private fun perlenSchieben(
    farbeL: MutableList<String>,
    farbeR: MutableList<String>,
): Int {
    if (farbeL[7] == "x") {
        // rechte Perlen nach links schieben
        rechtePerlenSchieben(farbeR, farbeL)
    } else {
        // linke Perlen nach rechts schieben
        linkePerlenSchieben(farbeL, farbeR)
    }
    return colorGroups(farbeL, farbeR)
}

/**
    Das Programm soll jene vier Züge machen, die zu den wenigsten Gruppen führen und die wenigsten Verschiebungen benötigen
 */
private fun showSolution(
    farbeL: MutableList<String>,
    farbeR: MutableList<String>
): Duration {
    var farbeLalt = { Array(18) { "" } }
    var farbeRalt = { Array(18) { "" } }

    val clock: Clock = Clock.System
    val start = clock.now()

    /*
    // linke und rechte Ausgangsstellung einlesen
    for (i in 0 .. 17) {
        farbeLalt[i] = farbeL[i]
        farbeRalt[i]  = farbeR[i]
    }

     */

    // Es wird vier Züge voraus berechnet, dann ein Zug gemacht. Das wird solange wiederholt bis nur
    // mehr 3 Farbgruppen übrig sind, also das Problem gelöst ist

    var seite: Seite

    // Seite="links": links befinnden sich 18 Perlen, rechts 14
    if (farbeR[7] == "x") {
        seite = Seite.LINKS
    } else {
        seite = Seite.RECHTS
    }

    var AnzTr = 0; var AnzV = 0 // AnzTr … Anzahl der Transfers, AnzV … Anzahl der Verschiebungen
    var gmin = 999; var vmin = 999
    var x1min = 999; var x2min = 999; var x3min = 999; var x4min = 999
    var xV = 999;

    // while (gmin > 3) {
        AnzTr = AnzTr + 1

        for (x1 in - 8 .. 9) {
            for (x2 in - 8 .. 9) {
                for (x3 in - 8 .. 9) {
                    for (x4 in - 8 .. 9) {
                        /*
                        if (x4 == 0) x4 = x4+1

                        // linke und rechte Ausgangsstellung herstellen
                        for (i in 0 .. 17) {
                            farbeLalt[i] = farbeL[i]
                            farbeRalt[i] = farbeR[i]
                        }

                        // Verschieben um x1
                        zug(FLalt, FLneu, FRalt, FRneu, seite, x1)
                        // Verschieben um x2
                        zug(FLalt, FLneu, FRalt, FRneu, seite, x2)
                        // Verschieben um x3
                        zug(FLalt, FLneu, FRalt, FRneu, seite, x3)
                        // Verschieben um x4
                        zug(FLalt, FLneu, FRalt, FRneu, seite, x4)


                         */
                        // Anzahl der Farbgruppen berechnen
                        val g = colorGroups(farbeL, farbeR)

                        /*
                        // Neues Minimum gefunden?
                        val v = abs(x1) + abs(x2)
                        if (g == gmin && v < vmin) {
                            vmin = v
                            x1min = x1; x2min = x2; x3min = x3; x4min = x4
                        }
                         if (g < gmin) {
                             gmin = g; vmin = v
                             x1min = x1; x2min = x2; x3min = x3; x4min = x4
                         }

                         */
                    }
                }
            }
        }

    val end = clock.now()
    val dauer = end - start
    val msg = "Dauer: " + dauer
    logger.v(TAG, msg)

    return dauer

        // den berechneten Zug machen
        if (x1min != 0) {
            xV = x1min
        }
        if (x1min == 0 && x2min != 0) {
            xV = x2min
        }
        if (x1min == 0 && x2min == 0 && x3min != 0) {
            xV = x3min
        }
        if (x1min == 0 && x2min == 0 && x3min == 0 && x4min != 0) {
            xV = x4min
        }
        AnzV = AnzV + abs(xV)
        // zug(farbeL, farbeLneu, farbeR, farbeRneu, Seite, xV)

        // Zug ausgeben
        // Cells(15 + AnzTr, 6) = xV

        // Anzahl der Farbgruppen berechnen
        gmin = colorGroups(farbeL, farbeR)
    // }

    // TODO: Ausgabe von Rechenzeit, Anzahl der Transfers, Anzahl der Verschiebungen
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
    farbeL: List<String>,
    farbeR: List<String>
) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            //rahmenZeichnen(canvasWidth)

            // Radius Perle
            val rP = canvasWidth / (4 * (1 / sin(15 * PI.toFloat() / 180) + 1))
            val rBog = rP / sin(15 * PI.toFloat() / 180)
            val l = 6 * rP

            // Perlen zeichnen

            val xpl = FloatArray(18)
            val xpr = FloatArray(18)
            val yp = FloatArray(18)

            for (i in 0..1) {
                xpl[i] = rP
                xpr[i] = canvasWidth - rP
                yp[i] = canvasWidth / 2 + (2*i+1)*rP
            }

            for (i in 2..6) {
                xpl[i] = (canvasWidth / 4 + rBog * cos((180 + (i - 1) * 30) * PI / 180)).toFloat()
                xpr[i] = (canvasWidth * 3 / 4 + rBog * cos((360 - (i - 1) * 30) * PI / 180)).toFloat()
                yp[i] =
                    (canvasWidth / 2 + 3 * rP - rBog * sin((180 + (i - 1) * 30) * PI / 180)).toFloat()
            }

            for (i in 7..10) {
                xpl[i] = canvasWidth / 2 - rP
                xpr[i] = canvasWidth / 2 + rP
                yp[i] = canvasWidth / 2 + 3 * rP - (i-7) * 2 * rP
            }

            for (i in 11..15) {
                xpl[i] = (canvasWidth / 4 + rBog * cos(((i - 10) * 30) * PI / 180)).toFloat()
                xpr[i] = (canvasWidth * 3 / 4 + rBog * cos((180 - 30 * (i - 10)) * PI / 180)).toFloat()
                yp[i] = (canvasWidth / 2 - 3 * rP - rBog * sin((30 * (i - 10)) * PI / 180)).toFloat()
            }

            for (i in 16..17) {
                xpl[i] = rP
                xpr[i] = canvasWidth - rP
                yp[i] = canvasWidth / 2 - (2*(17-i)+1)*rP
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
    farbeL[7] = farbeR[7]
    farbeL[8] = farbeR[8]
    farbeL[9] = farbeR[9]
    farbeL[10] = farbeR[10]
    for (i in 7..10) {
        farbeR[i] = "x"
    }
}

private fun linkePerlenSchieben(
    farbeL: MutableList<String>,
    farbeR: MutableList<String>
) {
    // Perlen von links nach rechts schieben
    farbeR[7] = farbeL[7]
    farbeR[8] = farbeL[8]
    farbeR[9] = farbeL[9]
    farbeR[10] = farbeL[10]
    for (i in 7..10) {
        farbeL[i] = "x"
    }
}

/**
 * perlenBewegen
 * @param direction Richtung, in die bewegt wird (1 = gegen Uhrzeigersinn, -1 im Uhrzeigersinn)
 */
private fun perlenBewegen(
    direction: Int,
    farbeL: MutableList<String>,
    farbeR: MutableList<String>,
    farbeLneu: Array<String>,
    farbeRneu: Array<String>,
) {
    if (direction == 1) { // nach oben
        if (farbeL[7] == "x") { // rechts drehen
            rotieren(1, farbeRneu, farbeR)
        } else {
            rotieren(1, farbeLneu, farbeL)
        }
    } else if (direction == -1) { // nach unten
        if (farbeL[7] == "x") { // rechts drehen
            rotieren(-1, farbeRneu, farbeR)
        } else {
            rotieren(-1, farbeLneu, farbeL)
        }
    }
}

@Composable
fun GameControls(
    onMove: () -> Unit,
    onMoveDown: () -> Unit,
    isAnimating: Boolean = false,
    onNewGame: () -> Unit,
    replay: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onMoveDown, enabled = !isAnimating) {
                Text("Down 1 bead", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onMove,
                enabled = !isAnimating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Green,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.Black
                )
            ) {
                Text("Transfer 4 beads", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = replay,
                enabled = !isAnimating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Cyan,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 25.dp)
            ) {
                Text("Replay", fontSize = 16.sp)
            }

            Button(
                onClick = onNewGame,
                enabled = !isAnimating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Yellow,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.Black
                )
            ) {
                Text("New Game", fontSize = 16.sp)
            }
        }
    }
}

/**
 * Einlesen Tabelle möglicher Spiele samt Musterlösungen
 * Spalten 1- 18: Farben links
 * Spalten 19-32: Farben rechts
 * Spalten 33-44: Züge der Musterlösung
 */
/*
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
        farbeLGame[i] = farbeL[i]
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
        farbeRGame[i] = farbeR[i]
    }
 */

// File Path Location:
// Save your CSV file in your project directory at:
// composeApp/src/androidMain/assets/daten.csv

// Remove the mutable lists from the parameters
suspend fun tabelleEinlesen(): Werte {
    // ⚠️ CRITICAL: Ensure this matches the file name in your assets folder!
    val fileName = "Perlen_Musterlösungen.csv"

    try {
        val reader = AssetFileReader()
        val csvContent = reader.readAssetFile(fileName)
        val rows = mutableListOf<CsvRow>()
        val lines = csvContent.lines()

        if (lines.isEmpty()) throw IllegalStateException("$fileName content is empty.")

        for (i in 0 .. (lines.size-1)) {
            val line = lines[i].trim()
            if (line.isNotEmpty()) {
                val tokens = line.split(",").map { it.trim() }
                    rows.add(CsvRow(tokens = tokens))
            }
        }

        val noLines = rows.size
        if (noLines == 0) throw IllegalStateException("$fileName contains no valid structural rows.")

        val spielNr = (0 .. (noLines-1)).random()
        val tabelleGetrennt = rows.map { it.tokens }

        val transferCountBest = tabelleGetrennt[spielNr][0].toIntOrNull() ?: 0
        val upDownCountBest = tabelleGetrennt[spielNr][1].toIntOrNull() ?: 0

        // Create temporary normal arrays to safely hold values
        val tempFarbeL = Array(18) { "" }
        val tempFarbeR = Array(18) { "" }

        val farbenTauschen = (0..1).random()

        // Fill left side
        for (i in 0..17) {
            val farbe = tabelleGetrennt[spielNr][i + 2]
            if (farbenTauschen == 0) {
                if (farbe == "r") {
                    tempFarbeL[i] = "rot"
                } else {
                    tempFarbeL[i] = "blau"
                }
            } else {
                if (farbe == "r") {
                    tempFarbeL[i] = "blau"
                } else {
                    tempFarbeL[i] = "rot"
                }
            }
        }

        // Fill right side
        for (i in 0..6) {
            val farbe = tabelleGetrennt[spielNr][20 + i]
            if (farbenTauschen == 0) {
                if (farbe == "r") {
                    tempFarbeR[i] = "rot"
                } else {
                    tempFarbeR[i] = "blau"
                }
            } else {
                if (farbe == "r") {
                    tempFarbeR[i] = "blau"
                } else {
                    tempFarbeR[i] = "rot"
                }
            }
        }
        for (i in 7..10) {
            tempFarbeR[i] = "x"
        }
        for (i in 11..17) {
            val farbe = tabelleGetrennt[spielNr][16 + i]
            if (farbenTauschen == 0) {
                if (farbe == "r") {
                    tempFarbeR[i] = "rot"
                } else {
                    tempFarbeR[i] = "blau"
                }
            } else {
                if (farbe == "r") {
                    tempFarbeR[i] = "blau"
                } else {
                    tempFarbeR[i] = "rot"
                }
            }
        }

        val loesungArray = Array(12) { 0 }
        var anzTrM = 0
        var anzVerM = 0

        for (i in 0..11) {
            val spaltenIndex = 34 + i
            val zahl = tabelleGetrennt[spielNr].getOrNull(spaltenIndex)?.toIntOrNull() ?: 77
            loesungArray[i] = zahl
            if (zahl != 77) {
                anzTrM += 1
                anzVerM += abs(zahl)
            }
        }

        var ausgabe = "$anzTrM,$anzVerM:   "
        if (anzTrM > 0) {
            val listToJoin = mutableListOf<String>()
            for (i in 0 until anzTrM) {
                listToJoin.add(loesungArray[i].toString())
            }
            ausgabe += listToJoin.joinToString(",")
        }

        // Pass arrays back inside your values dataclass
        return Werte(
            ausgabe = ausgabe,
            transferCountBest = transferCountBest,
            upDownCountBest = upDownCountBest,
            spielNr = spielNr,
            farbeL = tempFarbeL.toList(),
            farbeR = tempFarbeR.toList()
        )

    } catch (e: Exception) {
        throw e
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
        color = Color.Black,
        start = Offset(canvasWidth * 0f, canvasWidth * 0f),
        end = Offset(canvasWidth * 1f, canvasWidth * 0f),
        strokeWidth = 2.dp.toPx()
    )

    drawLine(
        color = Color.Black,
        start = Offset(canvasWidth * 1f, canvasWidth * 0f),
        end = Offset(canvasWidth * 1f, canvasWidth * 1f),
        strokeWidth = 2.dp.toPx()
    )

    drawLine(
        color = Color.Black,
        start = Offset(canvasWidth * 1f, canvasWidth * 1f),
        end = Offset(canvasWidth * 0f, canvasWidth * 1f),
        strokeWidth = 2.dp.toPx()
    )

    drawLine(
        color = Color.Black,
        start = Offset(canvasWidth * 0f, canvasWidth * 1f),
        end = Offset(canvasWidth * 0f, canvasWidth * 0f),
        strokeWidth = 2.dp.toPx()
    )
}

private fun colorGroups(farbeL: List<String>, farbeR: List<String>): Int {
    var noGroups = 0
    if (farbeR[7] == "x") { // links sind 18 Perlen
        for (i in 0..16) {
            if (farbeL[i+1] != farbeL[i]) {
                noGroups++
            }
        }
        if (farbeL[17] != farbeL[0]) {
            noGroups++
        }
        for (i in 0..5) {
            if (farbeR[i+1] != farbeR[i]) {
                noGroups++
            }
        }
        for (i in 11..16) {
            if (farbeR[i+1] != farbeR[i]) {
                noGroups++
            }
        }
        if (farbeR[17] != farbeR[0]) {
            noGroups++
        }
    } else { // Ring ist links
        for (i in 0..16) {
            if (farbeR[i+1] != farbeR[i]) {
                noGroups++
            }
        }
        if (farbeR[17] != farbeR[0]) {
            noGroups++
        }
        for (i in 0..5) {
            if (farbeL[i+1] != farbeL[i]) {
                noGroups++
            }
        }
        for (i in 11..16) {
            if (farbeL[i+1] != farbeL[i]) {
                noGroups++
            }
        }
        if (farbeL[17] != farbeL[0]) {
            noGroups++
        }
    }
    noGroups++
    return noGroups
}