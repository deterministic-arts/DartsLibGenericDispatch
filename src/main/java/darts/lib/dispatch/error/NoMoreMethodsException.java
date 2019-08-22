package darts.lib.dispatch.error;

import java.util.List;

public class NoMoreMethodsException extends DispatchException {

    private List<Object> arguments;
    private Object function;

    public NoMoreMethodsException(Object function, List<Object> arguments) {
        super(String.format("there are no further applicable methods on %s when invoked with %s", function, arguments));
        this.arguments = arguments;
        this.function = function;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    public Object getFunction() {
        return function;
    }
}

