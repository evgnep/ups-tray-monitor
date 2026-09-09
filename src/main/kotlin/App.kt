import com.dustinredmond.fxtrayicon.FXTrayIcon
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.stage.Stage
import kotlin.math.roundToInt
import estimate.DischargeEstimator
import modbus.UpsClient
import settings.SettingsHolder
import tray.TrayIconRenderer
import ui.SettingsWindow

// Tray app entry point. See PROJECT_BRIEF.md for the functional requirements.
class TrayApp : Application() {

    @Volatile
    private var running = true

    private val client = UpsClient()

    private val estimator = DischargeEstimator()

    override fun start(primaryStage: Stage) {
        // Required so closing any JavaFX window (e.g. settings) does not
        // kill the whole app - only the "Выход" menu item should.
        Platform.setImplicitExit(false)

        primaryStage.title = "UPS Tray Monitor"
        // primaryStage is never shown - it only anchors FXTrayIcon and owns
        // the settings window.

        val trayIcon = FXTrayIcon(primaryStage, TrayIconRenderer.render(0, TrayIconRenderer.Mode.NORMAL))
        trayIcon.setTrayIconTooltip("UPS Tray Monitor: опрос...")
        trayIcon.addMenuItem("Настройки") { SettingsWindow.showOrFocus(primaryStage) }
        trayIcon.addSeparator()
        trayIcon.addMenuItem("Выход") { exitApp() }
        trayIcon.show()

        startPolling(trayIcon)
    }

    private fun startPolling(trayIcon: FXTrayIcon) {
        val thread = Thread({
            var connectedPort: String? = null
            var connectedBaud: Int? = null
            var shutdownTriggered = false

            while (running) {
                val settings = SettingsHolder.current
                try {
                    if (!client.isConnected || settings.comPort != connectedPort || settings.baudRate != connectedBaud) {
                        client.connect(settings.comPort, settings.baudRate)
                        connectedPort = settings.comPort
                        connectedBaud = settings.baudRate
                    }

                    val status = client.readStatus()
                    val critical = status.onBattery && status.capacityPercent <= settings.shutdownThresholdPercent
                    val mode = when {
                        critical -> TrayIconRenderer.Mode.CRITICAL
                        status.onBattery -> TrayIconRenderer.Mode.ON_BATTERY
                        else -> TrayIconRenderer.Mode.NORMAL
                    }

                    if (status.onBattery) {
                        estimator.addSample(status.capacityPercent)
                    } else {
                        estimator.reset()
                    }
                    val etaSeconds = if (status.onBattery) {
                        estimator.estimateSecondsToThreshold(settings.shutdownThresholdPercent)
                    } else {
                        null
                    }

                    Platform.runLater {
                        trayIcon.setGraphic(TrayIconRenderer.render(status.capacityPercent.roundToInt(), mode))
                        val state = if (status.onBattery) "на батарее" else "от сети"
                        val eta = when {
                            !status.onBattery -> ""
                            etaSeconds == null -> ", оценка времени..."
                            etaSeconds == 0L -> ", порог достигнут"
                            etaSeconds < 60L -> ", менее минуты до порога"
                            else -> ", ~${formatEta(etaSeconds)} до порога"
                        }
                        trayIcon.setTrayIconTooltip("UPS: ${status.capacityPercent}% ($state)$eta")
                    }

                    if (critical) {
                        if (!shutdownTriggered) {
                            shutdownTriggered = true
                            triggerShutdown(settings.shutdownDelaySeconds)
                        }
                    } else if (!status.onBattery) {
                        // mains is back - allow shutdown to trigger again next time
                        shutdownTriggered = false
                    }
                } catch (e: Exception) {
                    client.disconnect()
                    connectedPort = null
                    connectedBaud = null
                    Platform.runLater {
                        trayIcon.setGraphic(TrayIconRenderer.render(0, TrayIconRenderer.Mode.ERROR))
                        trayIcon.setTrayIconTooltip("UPS: нет связи (${e.javaClass.simpleName})")
                    }
                }

                Thread.sleep(settings.pollIntervalSeconds.coerceAtLeast(1) * 1000L)
            }
        }, "ups-poller")
        thread.isDaemon = true
        thread.start()
    }

    private fun formatEta(seconds: Long): String = when {
        seconds < 3600 -> "${seconds / 60} мин"
        else -> "${seconds / 3600} ч ${(seconds % 3600) / 60} мин"
    }

    private fun triggerShutdown(delaySeconds: Int) {
        try {
            ProcessBuilder("shutdown", "/s", "/t", delaySeconds.toString()).start()
        } catch (e: Exception) {
            Platform.runLater {
                Alert(Alert.AlertType.ERROR).apply {
                    title = "UPS Tray Monitor"
                    headerText = "Не удалось запустить выключение"
                    contentText = "Команда shutdown не выполнилась: ${e.message}\n" +
                        "Выключите компьютер вручную, пока заряд ИБП не иссяк."
                    isResizable = true
                }.show()
            }
        }
    }

    private fun exitApp() {
        running = false
        client.disconnect()
        Platform.exit()
        System.exit(0)
    }
}

fun main(args: Array<String>) {
    Application.launch(TrayApp::class.java, *args)
}
