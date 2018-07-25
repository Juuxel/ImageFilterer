/* This file is a part of Image Filterer.
   See LICENSE.txt at the repository root for license information. */
package juuxel.imagefilterer

operator fun <E> Array2D<E>.set(x: Int, y: Int, element: E) = put(x, y, element)

fun <E> Array2D<E>.toList(): List<E>
{
    return copyOfArray.flatten().toList()
}

fun <E> array2DOf(width: Int, height: Int, vararg values: E): Array2D<E>
{
    require(values.size % width * height == 0)

    val array2D = Array2D<E>(width, height)

    for (x in 0 until width)
    {
        for (y in 0 until height)
        {
            array2D[x, y] = values[x + y * width]
        }
    }

    return array2D
}
