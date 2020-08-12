package darts.arch.dispatch;

import darts.arch.dispatch.error.AmbiguousMethodsException;
import darts.arch.dispatch.error.MissingMethodException;
import darts.arch.dispatch.error.NoMoreMethodsException;
import darts.arch.dispatch.util.Implication;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@SuppressWarnings({"Duplicates","rawtypes","unchecked"})
public class GenericBiConsumer implements BiConsumer<Object, Object> {

    private final String name;
    private final Map<BinarySignature, Invoker> cache;
    private final Map<BinarySignature, Entry> methods;

    public GenericBiConsumer(String fn) {
        name = Objects.requireNonNull(fn);
        cache = new HashMap<>();
        methods = new HashMap<>();
    }

    public void accept(Object arg1, Object arg2) {
        final BinarySignature key = new BinarySignature(arg1.getClass(), arg2.getClass());
        final Invoker em;
        synchronized (cache) {
            em = cache.computeIfAbsent(key, this::computeEffectiveMethod);
        }
        em.invoke(arg1, arg2);
    }

    @Override
    public String toString() {
        return "GenericBiConsumer(" + name + ")";
    }

    public <A1,A2> void addMethod(Class<A1> c1, Class<A2> c2, LeafMethod<? super A1, ? super A2> method) {
        final BinarySignature key = new BinarySignature(c1, c2);
        synchronized (cache) {
            cache.clear();
            if (methods.containsKey(key)) throw new IllegalStateException();
            else {
                final Leaf entry = new Leaf(key, method);
                methods.put(key, entry);
            }
        }
    }

    public <A1,A2> void addMethod(Class<A1> c1, Class<A2> c2, InnerMethod<? super A1, ? super A2> method) {
        final BinarySignature key = new BinarySignature(c1, c2);
        synchronized (cache) {
            cache.clear();
            if (methods.containsKey(key)) throw new IllegalStateException();
            else {
                final Inner entry = new Inner(key, method);
                methods.put(key, entry);
            }
        }
    }

    public interface NextMethod {
        void next();
    }

    @FunctionalInterface
    public interface LeafMethod<A1,A2> {
        void accept(A1 arg1, A2 arg2);
    }

    @FunctionalInterface
    public interface InnerMethod<A1,A2> {
        void accept(NextMethod next, A1 arg1, A2 arg2);
    }

    private static final Implication<Entry> implication = new Implication<>(Entry::implies);

    private Invoker computeEffectiveMethod(BinarySignature sig) {

        final List<Entry> candidates = methods
            .values()
            .stream()
            .filter(e -> e.accepts(sig))
            .collect(Collectors.toList());

        if (candidates.isEmpty()) return missing(sig);
        else {

            final List<Collection<Entry>> layers = implication.layers(candidates);
            final ListIterator<Collection<Entry>> iterator = layers.listIterator(layers.size());
            Invoker seed = noMoreMethods;

            while (iterator.hasPrevious()) {

                final Collection<Entry> here = iterator.previous();

                switch (here.size()) {
                    case 0: throw new AssertionError();
                    case 1: seed = here.iterator().next().bind(seed); break;
                    default: seed = ambiguous(sig, here); break;
                }
            }

            return seed;
        }
    }

    private interface Invoker {
        void invoke(Object arg1, Object arg2);
    }

    private final Invoker noMoreMethods = (arg1, arg2) -> {
        throw new NoMoreMethodsException(GenericBiConsumer.this, ImmutableList.of(arg1, arg2));
    };

    private Invoker missing(BinarySignature s) {
        return (arg1, arg2) -> {
            throw new MissingMethodException(this, ImmutableList.of(arg1, arg2));
        };
    }

    private Invoker ambiguous(BinarySignature s, Collection<Entry> cs) {
        return (arg1, arg2) -> {
            throw new AmbiguousMethodsException(this, ImmutableList.of(arg1, arg2), ImmutableList.copyOf(cs));
        };
    }

    private static abstract class Entry {

        final BinarySignature key;

        Entry(BinarySignature key) {
            this.key = key;
        }

        boolean implies(Entry e) {
            return key.implies(e.key);
        }

        boolean accepts(BinarySignature s) {
            return s.implies(key);
        }

        abstract Invoker bind(Invoker next);

        @Override
        public String toString() {
            return String.format("(%s, %s)", key.argument1, key.argument2);
        }
    }

    private static final class Leaf extends Entry implements Invoker {

        final LeafMethod method;

        Leaf(BinarySignature key, LeafMethod method) {
            super(key);
            this.method = method;
        }

        @Override
        Invoker bind(Invoker next) {
            return this;
        }

        @Override
        public void invoke(Object arg1, Object arg2) {
            method.accept(arg1, arg2);
        }
    }

    private static final class Inner extends Entry {

        final InnerMethod method;

        private Inner(BinarySignature key, InnerMethod method) {
            super(key);
            this.method = method;
        }

        @Override
        Invoker bind(Invoker next) {
            return (arg1, arg2) -> method.accept(() -> next.invoke(arg1, arg2), arg1, arg2);
        }
    }
}
