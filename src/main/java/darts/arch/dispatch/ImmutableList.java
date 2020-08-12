package darts.arch.dispatch;

import java.util.AbstractList;
import java.util.Collection;
import java.util.RandomAccess;

/**
 * A trivial immutable list. Defined here, so that we do not need to
 * have a dependency on (say) Guava, etc. Can be removed, as soon as
 * we can move on from Java 8 to Java 10.
 *
 * <p>This class does not need to be optimized, since we only need
 * it in error reporting.
 */

final class ImmutableList<T> extends AbstractList<T> implements RandomAccess {

    private static final ImmutableList EMPTY = new ImmutableList(new Object[0]);
    private final Object[] array;

    private ImmutableList(Object[] array) {
        this.array = array;
    }

    @SuppressWarnings("unchecked")
    static <U> ImmutableList<U> copyOf(Collection<? extends U> cs) {
        if (cs instanceof ImmutableList) return (ImmutableList<U>) cs;
        else {
            final Object[] buffer = cs.toArray();
            if (buffer.length == 0) return EMPTY;
            return new ImmutableList<>(buffer);
        }
    }

    @SuppressWarnings("unchecked")
    static <U> ImmutableList<U> of(U... args) {
        if (args.length == 0) return EMPTY;
        else {
            final Object[] cs = args.clone();
            return new ImmutableList<>(cs);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int index) {
        return (T) array[index];
    }

    @Override
    public int size() {
        return array.length;
    }
}
