package darts.arch.dispatch.error;

import java.util.Collection;
import java.util.List;

public class AmbiguousMethodsException extends DispatchException {

    private final List<Object> arguments;
    private final Object function;
    private final Collection<Object> candidates;

    public AmbiguousMethodsException(Object function, List<Object> arguments, Collection<Object> candidates) {
        super(String.format("ambiguous method selection on %s when invoked with %s; candidates are %s", function, arguments, candidates));
        this.arguments = arguments;
        this.function = function;
        this.candidates = candidates;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    public Object getFunction() {
        return function;
    }

    public Collection<Object> getCandidates() {
        return candidates;
    }
}
