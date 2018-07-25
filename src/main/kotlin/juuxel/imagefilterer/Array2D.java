/* This file is a part of Image Filterer.
   See LICENSE.txt at the repository root for license information. */
package juuxel.imagefilterer;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/* Written in Java for comfort, arrays in Kotlin are ugly (E[][] vs Array<Array<E>>).
 * This particular class comes from another project of mine, MapGen,
 * I just removed every unused feature like iterators.
 * The IntArray2D and FloatArray2D (not included) are better examples of comfortable 2D arrays in Kotlin.
 * Compare: Array<IntArray> and Array<FloatArray> to int[][] and float[][]
 */

/**
 * A wrapper class around a two-dimensional array of {@code E} elements.
 * @param <E> the element type
 */
public final class Array2D<E> implements Cloneable
{
    private final E[][] array;
    private final int width;
    private final int height;

    @SuppressWarnings("unchecked")
    public Array2D(int width, int height)
    {
        array = (E[][]) new Object[height][];

        for (int i = 0; i < height; i++)
        {
            array[i] = (E[]) new Object[width];
        }

        this.width = width;
        this.height = height;
    }

    @Nullable
    public E get(int x, int y)
    {
        return array[y][x];
    }

    public void put(int x, int y, E element)
    {
        array[y][x] = element;
    }

    public void fill(E element)
    {
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                array[y][x] = element;
            }
        }
    }

    @Override
    public int hashCode()
    {
        return Arrays.deepHashCode(array);
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof Array2D && (
                obj == this ||
                        Arrays.deepEquals(array, ((Array2D<?>) obj).array));
    }

    @Override
    public String toString()
    {
        return "Array2D" + Arrays.deepToString(array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Array2D<E> clone()
    {
        try
        {
            return (Array2D<E>) super.clone();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public E[][] getCopyOfArray()
    {
        E[][] target = (E[][]) new Object[height][];
        System.arraycopy(array, 0, target, 0, array.length);

        return target;
    }
}
