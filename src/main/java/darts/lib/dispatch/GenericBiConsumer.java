package darts.lib.dispatch;

import com.google.common.collect.ImmutableList;
import darts.lib.dispatch.error.AmbiguousMethodsException;
import darts.lib.dispatch.error.MissingMethodException;
import darts.lib.dispatch.error.NoMoreMethodsException;
import darts.lib.dispatch.util.Implication;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@SuppressWarnings({"Duplicates","rawtypes","unchecked"})
public class GenericBiConsumer implements BiConsumer<Object, Object> {

    private final String name;
    private final Map<Signature, Invoker> cache;
    private final Map<Signature, Entry> methods;

    public GenericBiConsumer(String fn) {
        name = Objects.requireNonNull(fn);
        cache = new HashMap<>();
        methods = new HashMap<>();
    }

    public void accept(Object arg1, Object arg2) {
        final Signature key = new Signature(arg1.getClass(), arg2.getClass());
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
        final Signature key = new Signature(c1, c2);
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
        final Signature key = new Signature(c1, c2);
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

    public static final class Signature {

        private final Class<?> argument1;
        private final Class<?> argument2;

        Signature(Class<?> argument1, Class<?> argument2) {
            this.argument1 = argument1;
            this.argument2 = argument2;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            else if (!(o instanceof Signature)) return false;
            else {
                final Signature s = (Signature) o;
                return argument1 == s.argument1 && argument2 == s.argument2;
            }
        }

        @Override
        public int hashCode() {
            return argument1.hashCode() * 31 + argument2.hashCode();
        }

        public boolean implies(Signature s) {
            return s.argument1.isAssignableFrom(argument1)
                && s.argument2.isAssignableFrom(argument2);
        }
    }

    private Invoker computeEffectiveMethod(Signature sig) {

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

    private Invoker missing(Signature s) {
        return (arg1, arg2) -> {
            throw new MissingMethodException(this, ImmutableList.of(arg1, arg2));
        };
    }

    private Invoker ambiguous(Signature s, Collection<Entry> cs) {
        return (arg1, arg2) -> {
            throw new AmbiguousMethodsException(this, ImmutableList.of(arg1, arg2), ImmutableList.copyOf(cs));
        };
    }

    private static abstract class Entry {

        final Signature key;

        Entry(Signature key) {
            this.key = key;
        }

        boolean implies(Entry e) {
            return key.implies(e.key);
        }

        boolean accepts(Signature s) {
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

        Leaf(Signature key, LeafMethod method) {
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

        private Inner(Signature key, InnerMethod method) {
            super(key);
            this.method = method;
        }

        @Override
        Invoker bind(Invoker next) {
            return (arg1, arg2) -> method.accept(() -> next.invoke(arg1, arg2), arg1, arg2);
        }
    }
}
