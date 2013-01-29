/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package jet;

import org.jetbrains.jet.rt.annotation.AssertInvisibleInResolver;

@AssertInvisibleInResolver
public final class ByteRange implements Range<Byte>, ByteIterable {
    private final byte start;
    private final int count;

    public static final ByteRange EMPTY = new ByteRange((byte) 0,0);

    public ByteRange(byte startValue, int count) {
        this.start = startValue;
        this.count = count;
    }

    @Override
    public String toString() {
        if (count == 0) {
            return "<empty range>";
        }
        else if (count > 0) {
            return getStart() + ".rangeTo(" + getEnd() + ")";
        }
        else {
            return getStart() + ".downTo(" + getEnd() + ")";
        }
    }

    @Override
    public boolean contains(Byte item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    public boolean contains(byte item) {
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ByteRange range = (ByteRange) o;
        return count == range.count && start == range.start;
    }

    @Override
    public int hashCode() {
        int result = (int) start;
        result = 31 * result + count;
        return result;
    }

    public ByteIterator step(int step) {
        if (step < 0)
            return new ByteIteratorImpl(getEnd(), getStart(), step);
        else
            return new ByteIteratorImpl(getStart(), getEnd(), step);
    }

    public byte getStart() {
        return start;
    }

    public byte getEnd() {
        return (byte) (count < 0 ? start + count + 1: count == 0 ? 0 : start+count-1);
    }

    public int getSize() {
        return count < 0 ? -count : count;
    }

    @Override
    public ByteIterator iterator() {
        return new ByteIteratorImpl(getStart(), getEnd(), 1);
    }

    private static class ByteIteratorImpl extends ByteIterator {
        private byte next;
        private final byte end;
        private final int increment;

        public ByteIteratorImpl(byte start, byte end, int increment) {
            this.next = start;
            this.end = end;
            this.increment = increment;
        }

        @Override
        public boolean hasNext() {
            return increment > 0 ? next <= end : next >= end;
        }

        @Override
        public byte nextByte() {
            byte value = next;
            next += increment;
            return value;
        }
    }
}
