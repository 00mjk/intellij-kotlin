/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jet.runtime;

import jet.*;

/**
 * @author alex.tkachman
 */
public abstract class ArrayIterator<T> implements Iterator<T>, JetObject {
    private final int size;
    protected int index;

    protected ArrayIterator(int size) {
        this.size = size;
    }

    @Override
    public boolean getHasNext() {
        return index < size;
    }

    private static class GenericIterator<T> extends ArrayIterator<T> {
        private final T[] array;

        private GenericIterator(T[] array) {
            super(array.length);
            this.array = array;
        }

        @Override
        public T next() {
            return array[index++];
        }

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }
    
    public static <T> Iterator<T> iterator(T[] array) {
        return new GenericIterator<T>(array);
    }

    private static class ArrayByteIterator extends ByteIterator {
        private final byte[] array;
        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayByteIterator(byte[] array) {
            this.array = array;
        }

        @Override
        public byte nextByte() {
            return array[index++];
        }

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }

    public static ByteIterator iterator(byte[] array) {
        return new ArrayByteIterator(array);
    }

    private static class ArrayShortIterator extends ShortIterator {
        private final short[] array;

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayShortIterator(short[] array) {
            this.array = array;
        }

        @Override
        public short nextShort() {
            return array[index++];
        }

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }

    public static ShortIterator iterator(short[] array) {
        return new ArrayShortIterator(array);
    }

    private static class ArrayIntegerIterator extends IntIterator {
        private final int[] array;

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayIntegerIterator(int[] array) {
            this.array = array;
        }

        @Override
        public int nextInt() {
            return array[index++];
        }

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }

    public static IntIterator iterator(int[] array) {
        return new ArrayIntegerIterator(array);
    }

    private static class ArrayLongIterator extends LongIterator {
        private final long[] array;

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayLongIterator(long[] array) {
            this.array = array;
        }

        @Override
        public long nextLong() {
            return array[index++];
        }

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }

    public static LongIterator iterator(long[] array) {
        return new ArrayLongIterator(array);
    }

    private static class ArrayFloatIterator extends FloatIterator {
        private final float[] array;

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayFloatIterator(float[] array) {
            this.array = array;
        }

        @Override
        public float nextFloat() {
            return array[index++];
        }

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }

    public static FloatIterator iterator(float[] array) {
        return new ArrayFloatIterator(array);
    }

    private static class ArrayDoubleIterator extends DoubleIterator {
        private final double[] array;

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayDoubleIterator(double[] array) {
            this.array = array;
        }

        @Override
        public double nextDouble() {
            return array[index++];
        }

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }

    public static DoubleIterator iterator(double[] array) {
        return new ArrayDoubleIterator(array);
    }

    private static class ArrayCharacterIterator extends CharIterator {
        private final char[] array;

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayCharacterIterator(char[] array) {
            this.array = array;
        }

        @Override
        public char nextChar() {
            return array[index++];
        }

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }

    public static CharIterator iterator(char[] array) {
        return new ArrayCharacterIterator(array);
    }

    private static class ArrayBooleanIterator extends BooleanIterator {
        private final boolean[] array;

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayBooleanIterator(boolean[] array) {
            this.array = array;
        }

        @Override
        public boolean nextBoolean() {
            return array[index++];
        }

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }

    public static BooleanIterator iterator(boolean[] array) {
        return new ArrayBooleanIterator(array);
    }
}
