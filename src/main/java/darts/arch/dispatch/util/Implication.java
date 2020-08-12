package darts.arch.dispatch.util;

import java.util.*;

public final class Implication<E> {

    @FunctionalInterface
    public interface Test<E> {

        boolean implies(E lhs, E rhs);
    }

    private final Test<? super E> test;

    public Implication(Test<? super E> test) {
        this.test = test;
    }

    public Collection<E> dominators(Collection<E> candidates) {

        final Iterator<E> iter = candidates.iterator();

        if( !iter.hasNext() ) return Collections.emptySet();
        else {

            final ArrayList<E> best = new ArrayList<>();

            best.add(iter.next());

            if( iter.hasNext() ) {

                while( iter.hasNext() ) {

                    final E newitem = iter.next();

                    inner: {

                        for(Iterator<E> olditer = best.iterator(); olditer.hasNext(); ) {

                            final E olditem = olditer.next();
                            final boolean oldimpliesnew = test.implies(olditem, newitem);
                            final boolean newimpliesold = test.implies(newitem, olditem);

                            if( newimpliesold ) {

                                if( !oldimpliesnew )
                                    olditer.remove();

                            } else
                            if( oldimpliesnew )
                                break inner;
                        }

                        best.add(newitem);
                    }
                }
            }

            return best;
        }
    }

    public List<Collection<E>> layers(Collection<E> input) {

        final Collection<E> candidates = new ArrayList<>(input);
        final ArrayList<Collection<E>> result = new ArrayList<>();

        while (!candidates.isEmpty()) {

            final Collection<E> layer = dominators(candidates);

            result.add(layer);
            candidates.removeAll(layer);
        }

        return result;
    }
}
