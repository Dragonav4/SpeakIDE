package com.danilian.speakide.ui

import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JPanel
import javax.swing.JWindow
import javax.swing.SwingUtilities
import javax.swing.Timer


class RecordingOverlay : RecordingIndicator {

    private var window: JWindow? = null
    private var pulseTimer: Timer? = null

    override fun show() {
        SwingUtilities.invokeLater {
            val w = JWindow().also { window = it }
            w.isAlwaysOnTop = true
            w.background = Color(0, 0, 0, 0)   // transparent frame

            val panel = OverlayPanel()
            w.contentPane = panel
            w.size = Dimension(OVERLAY_W, OVERLAY_H)

            val screen = w.graphicsConfiguration.bounds
            w.setLocation(
                screen.x + screen.width  - OVERLAY_W - 24,
                screen.y + screen.height - OVERLAY_H - 24
            )

            w.isVisible = true

            pulseTimer = Timer(600) { panel.toggleDot(); panel.repaint() }.also { it.start() }
        }
    }

    override fun hide() {
        SwingUtilities.invokeLater {
            pulseTimer?.stop()
            pulseTimer = null
            window?.dispose()
            window = null
        }
    }

    private class OverlayPanel : JPanel() {

        private var dotVisible = true

        init {
            isOpaque = false
            preferredSize = Dimension(OVERLAY_W, OVERLAY_H)
        }

        fun toggleDot() { dotVisible = !dotVisible }

        override fun paintComponent(g: Graphics) {
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            g2.color = Color(28, 28, 28, 220)
            g2.fillRoundRect(0, 0, width, height, height, height)

            if (dotVisible) {
                g2.color = Color(220, 50, 50)
                val dotSize = 10
                val dotY = (height - dotSize) / 2
                g2.fillOval(12, dotY, dotSize, dotSize)
            }

            g2.color = Color(230, 230, 230)
            g2.font = Font("SF Pro Text", Font.PLAIN, 12).takeIf { it.canDisplay('R') }
                ?: Font(Font.SANS_SERIF, Font.PLAIN, 12)
            val label = "Recording…"
            val fm = g2.fontMetrics
            val textY = (height + fm.ascent - fm.descent) / 2
            g2.drawString(label, 28, textY)
        }
    }

    companion object {
        private const val OVERLAY_W = 130
        private const val OVERLAY_H = 32
    }
}
