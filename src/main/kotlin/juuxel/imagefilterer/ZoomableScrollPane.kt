package juuxel.imagefilterer

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.swing.Swing
import java.awt.Image
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JScrollPane

/**
 * A `JScrollPane` extension for zooming into a [label] with an icon.
 *
 * @property label the label
 * @property imageProvider provides the unscaled `BufferedImage` for the label
 */
class ZoomableScrollPane(
    private val label: JLabel,
    private val imageProvider: () -> BufferedImage?
) : JScrollPane(label) {
    private var job: Job? = null

    /**
     * The zoom level with 1.0 being the non-zoomed base value.
     *
     * Setting this property calls [refreshZoom].
     */
    var zoom: Double = 1.0
        set(value) {
            field = value
            refreshZoomBlocking()
        }

    private fun refreshZoomBlocking() = runBlocking { refreshZoom() }

    /**
     * Refreshes the zoom level.
     *
     * Sets the [label]'s icon to a zoomed version of [imageProvider]'s provided image.
     */
    suspend fun refreshZoom() = currentScope scope@ {
        val image = imageProvider() ?: return@scope

        job?.cancel()
        job = launch {
            val scaled = image.getScaledInstance(
                (image.width.toDouble() * zoom).toInt(),
                (image.height.toDouble() * zoom).toInt(),
                Image.SCALE_SMOOTH)
            val icon = ImageIcon(scaled)

            withContext(Dispatchers.Swing) {
                label.icon = icon
            }
        }
    }
}
