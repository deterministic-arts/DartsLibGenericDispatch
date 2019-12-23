package darts.lib.dispatch;

import darts.lib.dispatch.error.AmbiguousMethodsException;
import darts.lib.dispatch.error.MissingMethodException;
import darts.lib.dispatch.error.NoMoreMethodsException;
import darts.lib.dispatch.util.Implication;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings({"Duplicates","rawtypes","unchecked"})
public final class GenericPredicate implements Predicate<Object> {

    private final String name;
    private final Map<Class<?>, Invoker> cache;
    private final Map<Class<?>, Entry> methods;

    public GenericPredicate(String fn) {
        name = Objects.requireNonNull(fn);
        cache = new HashMap<>();
        methods = new HashMap<>();
    }

    @Override
    public boolean test(Object arg1) {
        final Class<?> key = arg1.getClass();
        final Invoker em;
        synchronized (cache) {
            em = cache.computeIfAbsent(key, this::computeEffectiveMethod);
        }
        return em.invoke(arg1);
    }

    @Override
    public String toString() {
        return "GenericFunction(" + name + ")";
    }

    public <A1> void addMethod(Class<A1> key, LeafMethod<? super A1> method) {
        synchronized (cache) {
            cache.clear();
            if (methods.containsKey(key)) throw new IllegalStateException();
            else {
                final Leaf entry = new Leaf(key, method);
                methods.put(key, entry);
            }
        }
    }

    public <A1> void addMethod(Class<A1> key, InnerMethod<? super A1> method) {
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
        boolean next();
    }

    @FunctionalInterface
    public interface LeafMethod<A1> {
        boolean apply(A1 arg1);
    }

    @FunctionalInterface
    public interface InnerMethod<A1> {
        boolean apply(NextMethod next, A1 arg1);
    }

    private static final Implication<Entry> implication = new Implication<>(Entry::implies);

    private Invoker computeEffectiveMethod(Class<?> sig) {

        final List<Entry> candidates = methods
            .values()
            .stream()
            .filter(e -> e.accepts(sig))
            .collect(Collectors.toList());

        if (candidates.isEmpty()) return missingMethod;
        else {

            final List<Collection<Entry>> layers = implication.layers(candidates);
            final ListIterator<Collection<Entry>> iterator = layers.listIterator(layers.size());
            Invoker seed = noMoreMethods;

            while (iterator.hasPrevious()) {

                final Collection<Entry> here = iterator.previous();

                switch (here.size()) {
                case 0: throw new AssertionError();
                case 1: seed = here.iterator().next().bind(seed); break;
                default: seed = ambiguous(here); break;
                }
            }

            return seed;
        }
    }

    private interface Invoker {
        boolean invoke(Object arg1);
    }

    private final Invoker noMoreMethods = arg1 -> {
        throw new NoMoreMethodsException(this, ImmutableList.of(arg1));
    };

    private final Invoker missingMethod = arg1 -> {
        throw new MissingMethodException(this, ImmutableList.of(arg1));
    };

    private Invoker ambiguous(Collection<Entry> cs) {
        return arg1 -> {
            throw new AmbiguousMethodsException(this, ImmutableList.of(arg1), ImmutableList.copyOf(cs));
        };
    }

    private static abstract class Entry {

        final Class<?> key;

        Entry(Class<?> key) {
            this.key = key;
        }

        boolean implies(Entry e) {
            return e.key.isAssignableFrom(key);
        }

        boolean accepts(Class<?> s) {
            return key.isAssignableFrom(s);
        }

        abstract Invoker bind(Invoker next);

        @Override
        public String toString() {
            return key.toString();
        }
    }

    private static final class Leaf extends Entry implements Invoker {

        final LeafMethod method;

        Leaf(Class<?> key, LeafMethod method) {
            super(key);
            this.method = method;
        }

        @Override
        Invoker bind(Invoker next) {
            return this;
        }

        @Override
        public boolean invoke(Object arg1) {
            return method.apply(arg1);
        }
    }

    private static final class Inner extends Entry {

        final InnerMethod method;

        private Inner(Class<?> key, InnerMethod method) {
            super(key);
            this.method = method;
        }

        @Override
        Invoker bind(Invoker next) {
            return arg1 -> method.apply(() -> next.invoke(arg1), arg1);
        }
    }
}
