package ui

import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import settings.AppSettings
import settings.SettingsHolder

// Simple JavaFX settings window, per PROJECT_BRIEF.md requirement #6.
object SettingsWindow {
    private var openStage: Stage? = null

    fun showOrFocus(owner: Stage) {
        openStage?.let {
            it.show()
            it.toFront()
            return
        }

        val settings = SettingsHolder.current

        val comPortField = TextField(settings.comPort)
        val baudField = TextField(settings.baudRate.toString())
        val intervalField = TextField(settings.pollIntervalSeconds.toString())
        val thresholdField = TextField(settings.shutdownThresholdPercent.toString())
        val delayField = TextField(settings.shutdownDelaySeconds.toString())

        val grid = GridPane().apply {
            hgap = 8.0
            vgap = 8.0
            padding = Insets(12.0)
            addRow(0, Label("COM-порт:"), comPortField)
            addRow(1, Label("Скорость (baud):"), baudField)
            addRow(2, Label("Интервал опроса (сек):"), intervalField)
            addRow(3, Label("Порог выключения (%):"), thresholdField)
            addRow(4, Label("Задержка выключения (сек):"), delayField)
        }

        val saveButton = Button("Сохранить")
        val cancelButton = Button("Отмена")
        val buttonsBox = HBox(8.0, saveButton, cancelButton).apply {
            padding = Insets(0.0, 12.0, 12.0, 12.0)
        }

        val stage = Stage()
        stage.title = "Настройки - UPS Tray Monitor"
        stage.initOwner(owner)
        stage.scene = Scene(VBox(grid, buttonsBox))

        saveButton.setOnAction {
            // TODO: show a validation error to the user instead of silently
            // falling back to old values on bad input.
            val updated = AppSettings(
                comPort = comPortField.text.trim(),
                baudRate = baudField.text.toIntOrNull() ?: settings.baudRate,
                pollIntervalSeconds = intervalField.text.toIntOrNull() ?: settings.pollIntervalSeconds,
                shutdownThresholdPercent = thresholdField.text.toIntOrNull() ?: settings.shutdownThresholdPercent,
                shutdownDelaySeconds = delayField.text.toIntOrNull() ?: settings.shutdownDelaySeconds
            )
            updated.save()
            SettingsHolder.current = updated
            stage.close()
        }
        cancelButton.setOnAction { stage.close() }

        stage.setOnHidden { openStage = null }
        openStage = stage
        stage.show()
    }
}
