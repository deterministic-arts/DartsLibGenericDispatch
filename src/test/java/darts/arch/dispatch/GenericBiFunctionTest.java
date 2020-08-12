package darts.arch.dispatch;

import darts.arch.dispatch.error.AmbiguousMethodsException;
import org.junit.Assert;
import org.junit.Test;

public class GenericBiFunctionTest {

    @Test
    public void innerMethodDispatch() {

        final GenericBiFunction<Object> func = new GenericBiFunction<>("func");

        func.addMethod(Object.class, Object.class, (s1, s2) -> "OO");
        func.addMethod(CharSequence.class, String.class, (n, s1, s2) -> "CS" + n.next());
        func.addMethod(String.class, String.class, (n, s1, s2) -> "SS" + n.next());

        final StringBuilder sb = new StringBuilder();

        Assert.assertEquals("SSCSOO", func.apply("A", "B"));
        Assert.assertEquals("CSOO", func.apply(sb, "B"));
        Assert.assertEquals("OO", func.apply(sb, sb));
    }

    @Test(expected= AmbiguousMethodsException.class)
    public void ambiguityDetection() {

        final GenericBiFunction<Object> func = new GenericBiFunction<>("func");

        func.addMethod(Object.class, Object.class, (s1, s2) -> "OO");
        func.addMethod(CharSequence.class, String.class, (n, s1, s2) -> "CS" + n.next());
        func.addMethod(String.class, CharSequence.class, (n, s1, s2) -> "SC" + n.next());

        func.apply("A", "B");
    }
}
