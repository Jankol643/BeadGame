// In shared/src/commonMain/kotlin/LoggerFactory.kt
expect object LoggerFactory {
    fun getLogger(): Logger
}