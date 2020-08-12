package darts.arch.dispatch;

import darts.arch.dispatch.error.AmbiguousMethodsException;
import org.junit.Test;

import java.io.Serializable;

import static org.junit.Assert.assertEquals;

public class GenericFunctionTest {

    @Test
    public void argumentPrecedenceOrder() {
        final GenericFunction<String> subject = new GenericFunction<>("test");
        subject.addMethod(Object.class, o -> "Object");
        subject.addMethod(CharSequence.class, (n, o) -> "CharSequence:" + n.next());
        subject.addMethod(String.class, (n, o) -> "String:" + n.next());
        assertEquals("Object", subject.apply(1));
        assertEquals("CharSequence:Object", subject.apply(new StringBuilder()));
        assertEquals("String:CharSequence:Object", subject.apply("Hello"));
    }

    @Test(expected = AmbiguousMethodsException.class)
    public void ambiguityDetection() {
        final GenericFunction<String> subject = new GenericFunction<>("test");
        subject.addMethod(CharSequence.class, (n, o) -> "CharSequence:" + n.next());
        subject.addMethod(Serializable.class, (n, o) -> "Serializable:" + n.next());
        subject.apply("Hello");
    }
}
