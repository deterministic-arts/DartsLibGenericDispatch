package darts.lib.dispatch;

import com.google.common.collect.ImmutableList;
import darts.lib.dispatch.error.AmbiguousMethodsException;
import darts.lib.dispatch.error.MissingMethodException;
import darts.lib.dispatch.error.NoMoreMethodsException;
import darts.lib.dispatch.util.Implication;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * A bi-function, that can be extended at run-time. This class is modelled
 * after "generic functions", a concept found in languages like <a href="http://www.lispworks.com/documentation/lw70/CLHS/Body/07_f.htm">Common Lisp</a>
 * or <a href="https://opendylan.org/books/drm/Functions_Overview#HEADING-48-7">Dylan</a>.
 * Generic functions dispatch to the concrete implementations at run-time based
 * on the dynamic type of the arguments; all arguments are considered, when trying
 * to find an implementation handling the given combination of arguments.
 *
 * <p>New behaviour is added to a generic function in the form of "methods",
 * which can either be {@linkplain LeafMethod leaf methods} (i.e., do not call less
 * specific variants), or {@linkplain InnerMethod inner methods}, which may delegate
 * part of the work to less specific variants.
 *
 * @implNote The implementation maintains a cache of sorted applicable methods
 * for each combination of actual argument classes encountered, which is computed
 * when a particular combination is seen for the first time. This cache needs to
 * be purged, whenever the set of registered methods is changed. For this reason,
 * a program should register all methods first, before invoking the function, and
 * should avoid adding methods later, as that can degrade performance.
 *
 * @param <R> type of the function's result
 */
@SuppressWarnings({"Duplicates","rawtypes","unchecked"})
public final class GenericBiFunction<R> implements BiFunction<Object, Object,R> {

    private final String name;
    private final Map<Signature,Invoker> cache;
    private final Map<Signature,Entry> methods;

    /**
     * Constructs a new instance of this class. The given string {@code fn}
     * is used as a symbolic function name. It is used only by {@link #toString()},
     * and has no significance otherwise. It may not be null.
     *
     * @param fn symbolic function name
     */

    public GenericBiFunction(String fn) {
        name = Objects.requireNonNull(fn);
        cache = new HashMap<>();
        methods = new HashMap<>();
    }

    /**
     * Invoke this function with the given arguments. When called, searches
     * among all registered methods for those, which are applicable to the
     * given arguments, i.e., whose argument classes indicate, that they can
     * be called with the given values. It then sorts those methods such,
     * that the most specific method (the method, whose argument classes are
     * closest to the dynamic types of the arguments) are first, followed
     * by less specific classes. It then invokes that method, passing the
     * argument values along, and returns, whatever the method returns.
     *
     * @param arg1 first argument to pass to methods
     * @param arg2 second argument to pass to methods
     *
     * @return whatever the effective method returns
     *
     * @throws AmbiguousMethodsException if there are multiple methods, which
     *         might potentially handle the combination of input arguments,
     *         and none of them is more specific than all others.
     *
     * @throws MissingMethodException if there is no applicable method for
     *         the given combination of arguments at all registered on this
     *         generic function.
     *
     * @throws NoMoreMethodsException if an inner methods wanted to invoke
     *         its next less specific variant, and there was no less specific
     *         variant to invoke.
     */

    public R apply(Object arg1, Object arg2) {
        final Signature key = new Signature(arg1.getClass(), arg2.getClass());
        final Invoker em;
        synchronized (cache) {
            em = cache.computeIfAbsent(key, this::computeEffectiveMethod);
        }
        return (R) em.invoke(arg1, arg2);
    }

    @Override
    public String toString() {
        return "GenericBiFunction(" + name + ")";
    }

    /**
     * Adds {@code method} to this generic function, and declares it applicable
     * for arguments, whose dynamic types are compatible with classes {@code c1}
     * and {@code c2}.
     *
     * @param c1 type of accepted values for the first argument
     * @param c2 type of accepted values for the second argument
     * @param method function to invoke
     */

    public <A1,A2> void addMethod(Class<A1> c1, Class<A2> c2, LeafMethod<? super A1, ? super A2, ? extends R> method) {
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

    /**
     * Adds {@code method} to this generic function, and declares it applicable
     * for arguments, whose dynamic types are compatible with classes {@code c1}
     * and {@code c2}.
     *
     * <p>The method function {@code method} may require to invoke the next
     * specific method to perform parts of the work. It can do so by calling
     * {@link NextMethod#next() next()} on the specially provided first argument.
     * If no further methods are available, this will raise an {@link NoMoreMethodsException}.
     *
     * @param c1 type of accepted values for the second argument
     * @param c2 type of accepted values for the third argument
     * @param method function to invoke
     */

    public <A1,A2> void addMethod(Class<A1> c1, Class<A2> c2, InnerMethod<? super A1, ? super A2, ? extends R> method) {
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

    /**
     * Represents the next "less specific" method in an invocation
     * of an inner method.
     */

    public interface NextMethod<R> {

        /**
         * Invokes the next less specific method. The method receives the
         * same arguments as the calling method does; this happens automatically,
         * and hence, this function does not take any arguments.
         *
         * @return the result of the invocation
         */

        R next();
    }

    @FunctionalInterface
    public interface LeafMethod<A1,A2,R> {
        R apply(A1 arg1, A2 arg2);
    }

    @FunctionalInterface
    public interface InnerMethod<A1,A2,R> {
        R apply(NextMethod<R> next, A1 arg1, A2 arg2);
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
        Object invoke(Object arg1, Object arg2);
    }

    private final Invoker noMoreMethods = (arg1, arg2) -> {
        throw new NoMoreMethodsException(GenericBiFunction.this, ImmutableList.of(arg1, arg2));
    };

    private Invoker missing(Signature s) {
        return (arg1, arg2) -> {
            throw new MissingMethodException(GenericBiFunction.this, ImmutableList.of(arg1, arg2));
        };
    }

    private Invoker ambiguous(Signature s, Collection<Entry> cs) {
        return (arg1, arg2) -> {
            throw new AmbiguousMethodsException(GenericBiFunction.this, ImmutableList.of(arg1, arg2), ImmutableList.copyOf(cs));
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
        public Object invoke(Object arg1, Object arg2) {
            return method.apply(arg1, arg2);
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
            return (arg1, arg2) -> method.apply(() -> next.invoke(arg1, arg2), arg1, arg2);
        }
    }
}
