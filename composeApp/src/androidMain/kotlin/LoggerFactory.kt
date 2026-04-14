// In shared/src/androidMain/kotlin/LoggerFactory.kt
actual object LoggerFactory {
    actual fun getLogger(): Logger = AndroidLogger()
}