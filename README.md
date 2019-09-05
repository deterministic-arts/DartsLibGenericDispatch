# Generic Dispatch

This library provides a limited form of "generic functions". Generic functions
are a concept found in other languages (native to [Common Lisp](http://www.lispworks.com/documentation/lw70/CLHS/Body/07_f.htm)
and [Dylan](https://opendylan.org/books/drm/Functions_Overview#HEADING-48-7), 
available as extension module in [Python](https://pypi.org/project/PEAK-Rules/)).

The major differences between a "generic function" (as defined in this library) and
a regular function are

 1. generic functions dispatch to the appropriate implementation based on the 
    concrete run-time type of the arguments, whereas Java's method overloading is
    a purely syntactic compile-time feature. 
    
 2. all arguments are considered for the dispatch, whereas in a standard method call,
    only the implicit receiver argument has any influence over the method look up.  

This allows us to define code like:

    static void serialize(Object object, Object stream) {
        serializer.accept(object, stream);
    }

    static final GenericBiConsumer serializer = new GenericBiConsumer("serialize");
    static {
        serializer.addMethod(FancyId.class, XmlStream.class, (object, writer) -> { ... });
        serializer.addMethod(FancyId.class, BinaryStream.class, (object, writer) -> { ... });
        ...
    }
    
