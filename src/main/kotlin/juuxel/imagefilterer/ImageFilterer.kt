/* This file is a part of Image Filterer.
   See LICENSE.txt at the repository root for license information. */
package juuxel.imagefilterer

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileFilter
import kotlin.concurrent.thread
import kotlin.streams.toList

class ImageFilterer
{
    private var inputImage: BufferedImage? = null
        set(value)
        {
            if (value != null)
                inputLabel.icon = ImageIcon(value)
            field = value
        }

    private var outputImage: BufferedImage? = null
        set(value)
        {
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
    private lateinit var applyButton: JMenuItem

    companion object
    {
        private val fileFilter = object : FileFilter() {
            override fun accept(f: File?): Boolean
            {
                return f != null && (f.isDirectory || f.extension.toLowerCase(Locale.ROOT) in arrayOf("png", "jpg", "jpeg"))
            }

            override fun getDescription() = "Images (.png, .jpg, .jpeg)"
        }

        private val pngFilter = object : FileFilter() {
            override fun accept(f: File?): Boolean
            {
                return f != null && (f.isDirectory || f.extension.equals("png", ignoreCase = true))
            }

            override fun getDescription() = "PNG Images (.png)"
        }

        private val license: List<String>

        init
        {
            val resource = ImageFilterer::class.java.getResourceAsStream("/LICENSE.txt")
            license = resource.use {
                it.bufferedReader().lines().toList()
            }
        }

        @JvmStatic
        fun main(args: Array<String>)
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            ImageFilterer()
        }
    }

    init
    {
        class ActionImpl(name: String, private val action: () -> Unit) : AbstractAction(name)
        {
            override fun actionPerformed(e: ActionEvent?) = action()
        }

        frame = JFrame("Image Filterer by Juuz")

        inputLabel.border = BorderFactory.createTitledBorder("Input")
        outputLabel.border = BorderFactory.createTitledBorder("Output")
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

                redCyanButton.isSelected = true
                redCyanButton.addActionListener { filter = Filter.RED_CYAN }
                blurButton.addActionListener { filter = Filter.BLUR }

                buttonGroup.add(redCyanButton)
                buttonGroup.add(blurButton)

                val model = SpinnerNumberModel(1, 1, 10, 1)
                val spinner = JSpinner(model)
                spinner.addChangeListener { iterations = model.number.toInt() }
                applyButton = JMenuItem(ActionImpl("Apply Filter", ::applyFilter))

                add(redCyanButton)
                add(blurButton)
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
            add(progressBar)
        }

        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.jMenuBar = menuBar
        frame.size = Dimension(640, 480)
        frame.contentPane = JPanel(GridLayout(1, 0)).apply {
            add(JScrollPane(inputLabel))
            add(JScrollPane(outputLabel))
        }

        SwingUtilities.invokeLater {
            frame.isVisible = true
        }
    }

    private fun showOpenDialog()
    {
        val fileChooser = JFileChooser()
        fileChooser.removeChoosableFileFilter(fileChooser.fileFilter)
        fileChooser.addChoosableFileFilter(fileFilter)
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        fileChooser.isMultiSelectionEnabled = false

        val answer = fileChooser.showOpenDialog(frame)

        if (answer == JFileChooser.APPROVE_OPTION)
        {
            val file = fileChooser.selectedFile

            inputImage = ImageIO.read(file)
            outputImage = null
        }
    }

    private fun showSaveDialog()
    {
        val fileChooser = JFileChooser()
        fileChooser.removeChoosableFileFilter(fileChooser.fileFilter)
        fileChooser.addChoosableFileFilter(pngFilter)
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        fileChooser.isMultiSelectionEnabled = false

        val answer = fileChooser.showSaveDialog(frame)

        if (answer == JFileChooser.APPROVE_OPTION)
        {
            if (outputImage == null)
            {
                JOptionPane.showMessageDialog(frame, "Apply a filter before saving.", "No output generated", JOptionPane.ERROR_MESSAGE)
                return
            }

            val selectedFile = fileChooser.selectedFile
            val file = if (selectedFile.name.endsWith(".png", ignoreCase = true)) selectedFile
                       else File(selectedFile.path + ".png")

            ImageIO.write(outputImage, "PNG", file)
        }
    }

    private fun applyFilter()
    {
        if (inputImage == null)
        {
            JOptionPane.showMessageDialog(frame, "Open an image first.", "No image opened", JOptionPane.ERROR_MESSAGE)
            return
        }

        thread(start = true) {
            SwingUtilities.invokeAndWait {
                progressBar.isVisible = true
                applyButton.isEnabled = false
            }
            inputImage?.let {
                val output = BufferedImage(it.width, it.height, BufferedImage.TYPE_INT_ARGB)

                // Copy input to output
                val g = output.createGraphics()
                g.drawImage(it, 0, 0, null)
                g.dispose()

                val averageFunction = when (filter)
                {
                    Filter.RED_CYAN -> ::average
                    Filter.BLUR -> ::colorAverage
                }

                for (i in 1..iterations)
                    output.blur(averageFunction)
                outputImage = output
            }

            SwingUtilities.invokeLater {
                progressBar.isVisible = false
                applyButton.isEnabled = true
            }
        }
    }

    private fun showAboutDialog()
    {
        JOptionPane.showMessageDialog(frame, license.joinToString(separator = "\n"),
                                      "About Image Filterer", JOptionPane.INFORMATION_MESSAGE)
    }

    private enum class Filter
    {
        RED_CYAN, BLUR
    }
}

/**
 * Blurs `this` image.
 *
 * For each pixel, calculates the average of the surrounding colors using [averageFunction]
 * and stores it back in the image.
 */
fun BufferedImage.blur(averageFunction: (colors: IntArray) -> Int)
{
    operator fun BufferedImage.get(x: Int, y: Int) = getRGB(x, y)

    for (x in 0 until width)
    {
        for (y in 0 until height)
        {
            val excludeL = x == 0
            val excludeR = x == width - 1
            val excludeU = y == 0
            val excludeD = y == height - 1

            val functions = array2DOf(3, 3, { this[x - 1, y - 1] }, { this[x, y - 1] }, { this[x + 1, y - 1] },
                                            { this[x - 1, y] },     { this[x, y] },     { this[x + 1, y] },
                                            { this[x - 1, y + 1] }, { this[x, y + 1] }, { this[x + 1, y + 1] })

            val values = functions.toList().toMutableList()

            if (excludeL)
            {
                values.remove(functions[0, 0])
                values.remove(functions[0, 1])
                values.remove(functions[0, 2])
            }

            if (excludeR)
            {
                values.remove(functions[2, 0])
                values.remove(functions[2, 1])
                values.remove(functions[2, 2])
            }

            if (excludeU)
            {
                values.remove(functions[0, 0])
                values.remove(functions[1, 0])
                values.remove(functions[2, 0])
            }

            if (excludeD)
            {
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
fun average(vararg ints: Int): Int
{
    require(ints.isNotEmpty())

    return ints.sum() / ints.size
}

/**
 * Calculates the average of the color components of the [colors].
 */
fun colorAverage(vararg colors: Int): Int
{
    require(colors.isNotEmpty())

    val colorObjects = colors.map { Color(it) }
    val reds = colorObjects.map { it.red }.toTypedArray().toIntArray()
    val greens = colorObjects.map { it.green }.toTypedArray().toIntArray()
    val blues = colorObjects.map { it.blue }.toTypedArray().toIntArray()
    val alphas = colorObjects.map { it.alpha }.toTypedArray().toIntArray()

    return Color(average(*reds), average(*greens), average(*blues), average(*alphas)).rgb
}

fun BufferedImage.scaleImageToMaxSize(size: Int): Image
{
    return if (width <= size)
    {
        if (height <= size)
            this
        else
            getScaledInstance(width * size / height, size, Image.SCALE_SMOOTH)
    }
    else if (height <= size || width > height)
        getScaledInstance(size, height * size / width, Image.SCALE_SMOOTH)
    else if (height > width)
        getScaledInstance(width * size / height, size, Image.SCALE_SMOOTH)
    else if (width == height)
        getScaledInstance(size, size, Image.SCALE_SMOOTH)
    else this
}
