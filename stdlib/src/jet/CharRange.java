package jet;

public final class CharRange implements Range<Character>, CharIterable, JetObject {
    private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(CharRange.class, false);

    private final char start;
    private final int count;

    public static final CharRange empty = new CharRange((char) 0,0);

    public CharRange(char startValue, int count) {
        this.start = startValue;
        this.count = count;
    }

    @Override
    public boolean contains(Character item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    public boolean getIsReversed() {
        return count < 0;
    }

    public char getStart() {
        return start;
    }

    public char getEnd() {
        return (char) (count < 0 ? start + count + 1: count == 0 ? 0 : start+count-1);
    }

    public int getSize() {
        return count < 0 ? -count : count;
    }

    public CharRange minus() {
        return new CharRange(getEnd(), -count);
    }

    public CharIterator step(int step) {
        if(step < 0)
            return new MyIterator(getEnd(), -count, -step);
        else
            return new MyIterator(start, count, step);
    }

    @Override
    public CharIterator iterator() {
        return new MyIterator(start, count, 1);
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        return typeInfo;
    }

    @Override
    public JetObject getOuterObject() {
        return null;
    }

    public static CharRange count(int length) {
        return new CharRange((char) 0, length);
    }

    private static class MyIterator extends CharIterator {
        private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(MyIterator.class, false);

        private char cur;
        private int step;
        private int count;

        private final boolean reversed;

        public MyIterator(char startValue, int count, int step) {
            cur = startValue;
            this.step = step;
            if(count < 0) {
                reversed = true;
                count = -count;
                startValue += count;
            }
            else {
                reversed = false;
            }
            this.count = count;
        }

        @Override
        public boolean getHasNext() {
            return count > 0;
        }

        @Override
        public char nextChar() {
            count -= step;
            if(reversed) {
                cur -= step;
                return (char) (cur + step);
            }
            else {
                cur += step;
                return (char) (cur - step);
            }
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }
}
