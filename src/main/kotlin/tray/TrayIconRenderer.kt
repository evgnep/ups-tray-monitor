package tray

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image

// Tray icon: a full-height grey track (~75% of the icon width) with a
// narrower bar on top whose height is proportional to the charge and grows
// from the bottom. Green above 30%, red at or below. See PROJECT_BRIEF.md.
object TrayIconRenderer {
    private const val SIZE = 32
    private const val GREEN_THRESHOLD = 30

    private val TRACK = Color(90, 90, 90)
    private val GREEN = Color(50, 200, 70)
    private val RED = Color(220, 50, 50)

    enum class Mode { NORMAL, ON_BATTERY, CRITICAL, ERROR }

    // percent is ignored (shown as "?") when mode is ERROR - connection is lost.
    fun render(percent: Int, mode: Mode): Image {
        val image = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val trackWidth = Math.round(SIZE * 0.75f).toFloat()
        val trackLeft = (SIZE - trackWidth) / 2f

        g.color = TRACK
        g.fill(RoundRectangle2D.Float(trackLeft, 0f, trackWidth, SIZE.toFloat(), 3f, 3f))

        if (mode == Mode.ERROR) {
            drawCenteredText(g, "?", Color(210, 210, 210))
            g.dispose()
            return SwingFXUtils.toFXImage(image, null)
        }

        val p = percent.coerceIn(0, 100)
        if (p > 0) {
            val inset = 3f
            val fillWidth = trackWidth - 2 * inset
            val fillHeight = SIZE * p / 100f
            g.color = if (p > GREEN_THRESHOLD) GREEN else RED
            g.fill(RoundRectangle2D.Float(trackLeft + inset, SIZE - fillHeight, fillWidth, fillHeight, 2f, 2f))
        }

        g.dispose()
        return SwingFXUtils.toFXImage(image, null)
    }

    private fun drawCenteredText(g: Graphics2D, text: String, color: Color) {
        g.color = color
        g.font = Font("SansSerif", Font.BOLD, 18)
        val m = g.fontMetrics
        val x = (SIZE - m.stringWidth(text)) / 2f
        val y = (SIZE + m.ascent - m.descent) / 2f
        g.drawString(text, x, y)
    }
}
