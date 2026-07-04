package com.plcoding.kmp_gradle9_migration

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.startup.Initializer

private const val TAG = "KMP_DEBUG"

class MainApplication : Application() {
    companion object {
        private var instance: MainApplication? = null

        fun getContext(): Context {
            val currentInstance = instance
            Log.d(TAG, "getContext() called. Current instance state: ${if (currentInstance == null) "NULL" else "AVAILABLE"}")
            return currentInstance?.applicationContext
                ?: throw IllegalStateException("Application context is not available yet. App is initializing out of order!")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MainApplication: onCreate() started")
        instance = this

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "FATAL CRASH UNCAUGHT ON THREAD [${thread.name}]:", throwable)
        }
    }
}

// --- AUTOMATIC JETPACK STARTUP CONTEXT INITIALIZER ---
// This class runs automatically before anything else, initializing context values safely
class AppContextInitializer : Initializer<Context> {
    override fun create(context: Context): Context {
        Log.d(TAG, "AppContextInitializer: Injecting context provider hooks early")
        return context.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}

///data/data/<deine.package.name>/files/<fileName>
actual class AssetFileReader {
    actual suspend fun readAssetFile(fileName: String): String {
        Log.d(TAG, "readAssetFile entry point for: $fileName")
        return try {
            val context = MainApplication.getContext()
            // Locate the writable app-private internal storage file
            val localFile = java.io.File(context.filesDir, fileName)

            // Copy-on-Write: Extract from assets to internal storage if missing
            if (!localFile.exists()) {
                Log.d(TAG, "$fileName missing from local cache. Initializing first-copy from APK assets...")
                context.assets.open(fileName).use { inputStream ->
                    localFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d(TAG, "Successfully extracted $fileName to internal writable storage.")
            }

            // Always read from the persistent internal storage file thereafter
            localFile.readText().also {
                Log.d(TAG, "Successfully read file from persistent storage: $fileName (${it.length} chars)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: AssetFileReader failed to open/read: $fileName", e)
            throw RuntimeException("AssetFileReader failed for '$fileName': ${e.localizedMessage}", e)
        }
    }

    // New helper exposing write capability to the internal storage mirror without external dependencies
    actual suspend fun writeAssetFile(fileName: String, content: String) {
        Log.d(TAG, "writeAssetFile entry point for: $fileName")
        try {
            val context = MainApplication.getContext()
            val localFile = java.io.File(context.filesDir, fileName)

            // Overwrite the persistent internal storage mirror file
            localFile.writeText(content)
            Log.d(TAG, "Successfully wrote updated data back to persistent mirror: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: AssetFileReader failed to write: $fileName", e)
            throw RuntimeException("AssetFileReader failed to write '$fileName': ${e.localizedMessage}", e)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity: onCreate() started")

        try {
            setContent {
                var globalUiError by remember { mutableStateOf<String?>(null) }

                MaterialTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (globalUiError == null) {
                            // App signature remains ready to feed pipeline exceptions upward
                            App(onErrorCaught = { throwable ->
                                Log.e(TAG, "Error caught via App pipeline parameter callback", throwable)
                                globalUiError = throwable.stackTraceToString()
                            })
                        }

                        globalUiError?.let { errorMessage ->
                            ComposeErrorLayout(errorMessage) { globalUiError = null }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "FATAL COMPOSE PASS INITIATION CRASH DETECTED!", t)
            renderFallbackNativeView(t)
        }
    }

    private fun renderFallbackNativeView(t: Throwable) {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF220000.toInt())
            setPadding(40, 40, 40, 40)
        }

        val headerText = TextView(this).apply {
            text = "⚠️ FATAL BOOTSTRAP CRASH"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 20)
        }
        rootLayout.addView(headerText)

        val subheaderText = TextView(this).apply {
            text = "The application crashed during static generation before Compose could render. Stack trace details below:"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 14f
            setPadding(0, 0, 0, 30)
        }
        rootLayout.addView(subheaderText)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            setBackgroundColor(0xFF111111.toInt())
            setPadding(16, 16, 16, 16)
        }

        val traceText = TextView(this).apply {
            text = t.stackTraceToString()
            setTextColor(0xFFFF8888.toInt())
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
        }

        scrollView.addView(traceText)
        rootLayout.addView(scrollView)
        setContentView(rootLayout)
    }
}

// --- MERGED DEBUG OVERLAY ---
// Handles both internal Initialization errors and full application execution crashes safely
@Composable
fun ComposeErrorLayout(errorMessage: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)) // Blended opacity background from part 1
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = "⚠️ Initialization Error / Crash Intercepted",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dismiss / Clear & Retry Startup")
                }
            }
        }
    }
}
