/* This file is a part of Image Filterer.
   See LICENSE.txt at the repository root for license information. */
package juuxel.imagefilterer

import juuxel.basiks.grid.Grid
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.swing.Swing
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileFilter
import kotlin.streams.toList

class ImageFilterer {
    private var inputImage: BufferedImage? = null
        set(value) {
            if (value != null)
                inputLabel.icon = ImageIcon(value)
            field = value
        }

    private var outputImage: BufferedImage? = null
        set(value) {
            if (value != null)
                outputLabel.icon = ImageIcon(value)
            else
                outputLabel.icon = null

            field = value
        }

    private val inputLabel = JLabel()
    private val outputLabel = JLabel()
    private var frame: JFrame
    private var filter = Filter.RED_CYAN
    private var iterations = 1
    private val progressBar = JProgressBar(SwingConstants.HORIZONTAL)
    private val progressLabel = JLabel()
    private val leftScrollPane = ZoomableScrollPane(inputLabel, ::inputImage)
    private val rightScrollPane = ZoomableScrollPane(outputLabel, ::outputImage)
    private lateinit var applyButton: JMenuItem
    private val openChooser: JFileChooser by lazy {
        JFileChooser().also { fileChooser ->
            fileChooser.removeChoosableFileFilter(fileChooser.fileFilter)
            fileChooser.addChoosableFileFilter(fileFilter)
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            fileChooser.isMultiSelectionEnabled = false
        }
    }
    private val saveChooser: JFileChooser by lazy {
        JFileChooser().also { fileChooser ->
            fileChooser.removeChoosableFileFilter(fileChooser.fileFilter)
            fileChooser.addChoosableFileFilter(pngFilter)
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            fileChooser.isMultiSelectionEnabled = false
        }
    }

    companion object {
        private val fileFilter = object : FileFilter() {
            override fun accept(f: File?): Boolean {
                return f != null && (f.isDirectory || f.extension.toLowerCase(Locale.ROOT) in arrayOf(
                    "png",
                    "jpg",
                    "jpeg"
                ))
            }

            override fun getDescription() = "Images (.png, .jpg, .jpeg)"
        }

        private val pngFilter = object : FileFilter() {
            override fun accept(f: File?) =
                f != null && (f.isDirectory || f.extension.equals("png", ignoreCase = true))

            override fun getDescription() = "PNG Images (.png)"
        }

        private val license: List<String>

        init {
            val resource = ImageFilterer::class.java.getResourceAsStream("/LICENSE.txt")
            license = resource.use {
                it.bufferedReader().lines().toList()
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            ImageFilterer()
        }
    }

    init {
        class ActionImpl(name: String, private val action: () -> Unit) : AbstractAction(name) {
            override fun actionPerformed(e: ActionEvent?) = action()
        }

        frame = JFrame("Image Filterer by Juuz")

        leftScrollPane.border = BorderFactory.createTitledBorder("Input")
        rightScrollPane.border = BorderFactory.createTitledBorder("Output")
        inputLabel.horizontalAlignment = SwingConstants.CENTER
        outputLabel.horizontalAlignment = SwingConstants.CENTER

        val menuBar = JMenuBar().apply {
            val fileMenu = JMenu("File").apply {
                add(ActionImpl("Open", ::showOpenDialog))
                add(ActionImpl("Save", ::showSaveDialog))
                addSeparator()
                add(ActionImpl("About Image Filterer", ::showAboutDialog))
                add(ActionImpl("Quit") { System.exit(0) })
            }

            val filterSettings = JMenu("Filter").apply {
                val buttonGroup = ButtonGroup()
                val redCyanButton = JRadioButtonMenuItem("Red-Cyan Filter")
                val blurButton = JRadioButtonMenuItem("Blur Filter")
                val slowBlurButton = JRadioButtonMenuItem("Slow Blur Filter")

                redCyanButton.isSelected = true
                redCyanButton.addActionListener { filter = Filter.RED_CYAN }
                blurButton.addActionListener { filter = Filter.BLUR }
                slowBlurButton.addActionListener { filter = Filter.SLOW_BLUR }

                buttonGroup.add(redCyanButton)
                buttonGroup.add(blurButton)
                buttonGroup.add(slowBlurButton)

                val model = SpinnerNumberModel(1, 1, 10, 1)
                val spinner = JSpinner(model)
                spinner.addChangeListener { iterations = model.number.toInt() }
                applyButton = JMenuItem(ActionImpl("Apply Filter", ::applyFilter))

                add(redCyanButton)
                add(blurButton)
                add(slowBlurButton)
                addSeparator()
                add(JLabel("Iterations"))
                add(spinner)
                addSeparator()
                add(applyButton)
            }

            progressBar.maximumSize = Dimension(20, 50)
            progressBar.isIndeterminate = true
            progressBar.isVisible = false

            add(fileMenu)
            add(filterSettings)
            add(Box.createHorizontalGlue())
            add(progressLabel)
            add(Box.createHorizontalStrut(5))
            add(progressBar)
        }

        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.jMenuBar = menuBar
        frame.size = Dimension(640, 480)
        frame.contentPane = JPanel(BorderLayout()).apply {
            add(JPanel(GridLayout(1, 0)).apply {
                add(leftScrollPane)
                add(rightScrollPane)
            }, BorderLayout.CENTER)

            val zoomSlider = JSlider(SwingConstants.HORIZONTAL, 10, 200, 100)
            zoomSlider.addChangeListener {
                val zoom = zoomSlider.value.toDouble() / 100.0
                leftScrollPane.zoom = zoom
                rightScrollPane.zoom = zoom
                frame.repaint()
            }
            zoomSlider.maximumSize = Dimension(50, 20)

            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(Box.createHorizontalGlue())
                add(JLabel("Zoom"))
                add(Box.createHorizontalStrut(5))
                add(zoomSlider)
            }, BorderLayout.SOUTH)
        }

        SwingUtilities.invokeLater {
            frame.isVisible = true
        }
    }

    private fun showOpenDialog() {
        val answer = openChooser.showOpenDialog(frame)

        if (answer == JFileChooser.APPROVE_OPTION) {
            val file = openChooser.selectedFile

            inputImage = ImageIO.read(file)
            outputImage = null
        }
    }

    private fun showSaveDialog() {
        val answer = saveChooser.showSaveDialog(frame)

        if (answer == JFileChooser.APPROVE_OPTION) {
            if (outputImage == null) {
                JOptionPane.showMessageDialog(
                    frame,
                    "Apply a filter before saving.",
                    "No output generated",
                    JOptionPane.ERROR_MESSAGE
                )
                return
            }

            val selectedFile = saveChooser.selectedFile
            val file =
                if (selectedFile.name.endsWith(".png", ignoreCase = true)) selectedFile
                else File(selectedFile.path + ".png")

            ImageIO.write(outputImage, "PNG", file)
        }
    }

    // This function must not block
    private fun applyFilter() {
        if (inputImage == null) {
            JOptionPane.showMessageDialog(frame, "Open an image first.", "No image opened", JOptionPane.ERROR_MESSAGE)
            return
        }

        // Launch a coroutine doing all the work
        GlobalScope.launch {
            // The channel sends the iterations between the worker coroutine
            // and the UI/main coroutine.
            val channel = Channel<Pair<BufferedImage?, Int>>()

            // Update these on the EDT
            withContext(Dispatchers.Swing) {
                progressBar.isVisible = true
                applyButton.isEnabled = false
            }

            // This is the worker coroutine
            launch {
                inputImage?.let {
                    val output = BufferedImage(it.width, it.height, BufferedImage.TYPE_INT_ARGB)

                    // Copy input to output
                    val g = output.createGraphics()
                    g.drawImage(it, 0, 0, null)
                    g.dispose()

                    val averageFunction = when (filter) {
                        Filter.RED_CYAN -> ::average
                        Filter.BLUR -> ::colorAverage
                        Filter.SLOW_BLUR -> ::slowColorAverage
                    }

                    if (iterations > 1) {
                        withContext(Dispatchers.Swing) {
                            progressLabel.text = "1 / $iterations"
                        }
                    }

                    for (i in 1..iterations) {
                        // Run the blur function and
                        // send the result to the channel
                        output.blur(averageFunction)
                        channel.send(output to i)
                    }

                    // Close the channel to make the iteration stop.
                    channel.close()
                }
            }

            for ((image, iteration) in channel) {
                if (iterations > 1) {
                    // Update the progress label on the EDT
                    withContext(Dispatchers.Swing) {
                        progressLabel.text = "${iteration + 1} / $iterations"
                    }
                }
                outputImage = image
                rightScrollPane.refreshZoom()
            }

            // Cleanup
            withContext(Dispatchers.Swing) {
                progressLabel.text = null
                progressBar.isVisible = false
                applyButton.isEnabled = true
            }
        }
    }

    private fun showAboutDialog() {
        JOptionPane.showMessageDialog(
            frame, license.joinToString(separator = "\n"),
            "About Image Filterer", JOptionPane.INFORMATION_MESSAGE
        )
    }

    private enum class Filter {
        RED_CYAN, BLUR, SLOW_BLUR
    }
}

/**
 * Blurs `this` image.
 *
 * For each pixel, calculates the average of the surrounding colors using [averageFunction]
 * and stores it back in the image.
 */
fun BufferedImage.blur(averageFunction: (colors: IntArray) -> Int) {
    operator fun BufferedImage.get(x: Int, y: Int) = getRGB(x, y)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val excludeL = x == 0
            val excludeR = x == width - 1
            val excludeU = y == 0
            val excludeD = y == height - 1

            val functions = Grid(3, 3) { gx, gy -> {
                this[x - 1 + gx, y - 1 + gy]
            }}

            val values = functions.toList().toMutableList()

            if (excludeL) {
                values.remove(functions[0, 0])
                values.remove(functions[0, 1])
                values.remove(functions[0, 2])
            }

            if (excludeR) {
                values.remove(functions[2, 0])
                values.remove(functions[2, 1])
                values.remove(functions[2, 2])
            }

            if (excludeU) {
                values.remove(functions[0, 0])
                values.remove(functions[1, 0])
                values.remove(functions[2, 0])
            }

            if (excludeD) {
                values.remove(functions[0, 2])
                values.remove(functions[1, 2])
                values.remove(functions[2, 2])
            }

            setRGB(x, y, averageFunction(values.map { it() }.toTypedArray().toIntArray()))
        }
    }
}

/**
 * Calculates the average of the [ints].
 */
fun average(vararg ints: Int): Int {
    require(ints.isNotEmpty())

    return ints.sum() / ints.size
}

// TODO Replace with inline class in Kotlin 1.3
typealias IntColor = Int

inline val IntColor.red: Int get() = (this shr 16) and 0xFF
inline val IntColor.green: Int get() = (this shr 8) and 0xFF
inline val IntColor.blue: Int get() = (this shr 0) and 0xFF
inline val IntColor.alpha: Int get() = (this shr 24) and 0xFF

fun intColorOf(r: Int, g: Int, b: Int, a: Int): IntColor =
    a and 0xFF shl 24 or
        (r and 0xFF shl 16) or
        (g and 0xFF shl 8) or
        (b and 0xFF shl 0)

/**
 * Calculates the average of the color components of the [colors].
 */
fun colorAverage(vararg colors: IntColor): IntColor {
    require(colors.isNotEmpty())

    val reds = colors.map { it.red }.toIntArray()
    val greens = colors.map { it.green }.toIntArray()
    val blues = colors.map { it.blue }.toIntArray()
    val alphas = colors.map { it.alpha }.toIntArray()

    return intColorOf(average(*reds), average(*greens), average(*blues), average(*alphas))
}

/**
 * Calculates the average of the color components of the [colors].
 */
fun slowColorAverage(vararg colors: IntColor): IntColor {
    require(colors.isNotEmpty())

    val colorObjects = colors.map { Color(it) }
    val reds = colorObjects.map { it.red }.toTypedArray().toIntArray()
    val greens = colorObjects.map { it.green }.toTypedArray().toIntArray()
    val blues = colorObjects.map { it.blue }.toTypedArray().toIntArray()
    val alphas = colorObjects.map { it.alpha }.toTypedArray().toIntArray()

    return Color(average(*reds), average(*greens), average(*blues), average(*alphas)).rgb
}
