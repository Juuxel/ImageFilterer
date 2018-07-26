package juuxel.imagefilterer

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
class ZoomableScrollPane(private val label: JLabel,
                         private val imageProvider: () -> BufferedImage?) : JScrollPane(label)
{
    companion object
    {
        private val executor = TaskExecutor()
    }

    /**
     * The zoom level with 1.0 being the non-zoomed base value.
     *
     * Setting this property calls [refreshZoom].
     */
    var zoom: Double = 1.0
        set(value)
        {
            field = value
            refreshZoom()
        }

    /**
     * Refreshes the zoom level.
     *
     * Sets the [label]'s icon to a zoomed version of [imageProvider]'s provided image.
     */
    fun refreshZoom()
    {
        val image = imageProvider() ?: return

        executor.stopTask(id = this)
        executor.addTask(id = this) {
            label.icon = ImageIcon(image.getScaledInstance((image.width.toDouble() * zoom).toInt(),
                                                           (image.height.toDouble() * zoom).toInt(),
                                                           Image.SCALE_SMOOTH))
        }
    }
}
