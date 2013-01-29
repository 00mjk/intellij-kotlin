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
public final class IntRange implements Range<Integer>, IntIterable {
    private final int start;
    private final int count;

    public static final IntRange EMPTY = new IntRange(0, 0);

    public IntRange(int startValue, int count) {
        this.start = startValue;
        this.count = count;
    }

    @Override
    public String toString() {
        return getStart() + ".." + getEnd();
    }

    @Override
    public boolean contains(Integer item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    public boolean contains(int item) {
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

        IntRange range = (IntRange) o;
        return count == range.count && start == range.start;
    }

    @Override
    public int hashCode() {
        int result = start;
        result = 31 * result + count;
        return result;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return count < 0 ? start + count + 1: count == 0 ? 0 : start+count-1;
    }

    public int getSize() {
        return count < 0 ? -count : count;
    }

    @Override
    public IntIterator iterator() {
        return new IntSequenceIterator(getStart(), getEnd(), 1);
    }
}
