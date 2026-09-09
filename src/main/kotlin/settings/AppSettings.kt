package settings

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

// Simple settings holder. Stored as a properties file next to the app
// (working directory), per PROJECT_BRIEF.md requirement #5.
data class AppSettings(
    val comPort: String = "COM4",
    val baudRate: Int = 9600, // confirmed working value, see PROJECT_BRIEF.md
    val pollIntervalSeconds: Int = 2,
    val shutdownThresholdPercent: Int = 20,
    val shutdownDelaySeconds: Int = 60
) {
    fun save() {
        val props = Properties()
        props.setProperty("comPort", comPort)
        props.setProperty("baudRate", baudRate.toString())
        props.setProperty("pollIntervalSeconds", pollIntervalSeconds.toString())
        props.setProperty("shutdownThresholdPercent", shutdownThresholdPercent.toString())
        props.setProperty("shutdownDelaySeconds", shutdownDelaySeconds.toString())
        Files.newOutputStream(FILE).use { out ->
            props.store(out, "UPS Tray Monitor settings")
        }
    }

    companion object {
        private val FILE: Path = Path.of("settings.properties")

        fun load(): AppSettings {
            if (!Files.exists(FILE)) {
                val defaults = AppSettings()
                defaults.save()
                return defaults
            }
            val props = Properties()
            Files.newInputStream(FILE).use { props.load(it) }
            val defaults = AppSettings()
            return AppSettings(
                comPort = props.getProperty("comPort", defaults.comPort),
                baudRate = props.getProperty("baudRate")?.toIntOrNull() ?: defaults.baudRate,
                pollIntervalSeconds = props.getProperty("pollIntervalSeconds")?.toIntOrNull()
                    ?: defaults.pollIntervalSeconds,
                shutdownThresholdPercent = props.getProperty("shutdownThresholdPercent")?.toIntOrNull()
                    ?: defaults.shutdownThresholdPercent,
                shutdownDelaySeconds = props.getProperty("shutdownDelaySeconds")?.toIntOrNull()
                    ?: defaults.shutdownDelaySeconds
            )
        }
    }
}

// Mutable holder so the settings window and the poller thread see the same,
// up-to-date settings without passing state around everywhere.
object SettingsHolder {
    @Volatile
    var current: AppSettings = AppSettings.load()
}
