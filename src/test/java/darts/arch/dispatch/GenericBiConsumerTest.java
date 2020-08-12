package darts.arch.dispatch;

import darts.arch.dispatch.error.AmbiguousMethodsException;
import org.junit.Assert;
import org.junit.Test;

public class GenericBiConsumerTest {

    @Test
    public void innerMethodDispatch() {

        final GenericBiConsumer func = new GenericBiConsumer("func");

        func.addMethod(Object.class, StringBuilder.class, (s1, s2) -> s2.append("O"));
        func.addMethod(CharSequence.class, StringBuilder.class, (n, s1, s2) -> { s2.append("C"); n.next(); });
        func.addMethod(String.class, StringBuilder.class, (n, s1, s2) -> { s2.append("S"); n.next(); });

        final StringBuilder sb = new StringBuilder();

        func.accept("A", sb);
        Assert.assertEquals("SCO", sb.toString());
        sb.setLength(0);

        func.accept(sb, sb);
        Assert.assertEquals("CO", sb.toString());
        sb.setLength(0);

        func.accept(func, sb);
        Assert.assertEquals("O", sb.toString());
        sb.setLength(0);
    }

    @Test(expected= AmbiguousMethodsException.class)
    public void ambiguityDetection() {

        final GenericBiConsumer func = new GenericBiConsumer("func");

        func.addMethod(Object.class, Object.class, (s1, s2) -> {});
        func.addMethod(CharSequence.class, String.class, (n, s1, s2) -> {});
        func.addMethod(String.class, CharSequence.class, (n, s1, s2) -> {});

        func.accept("A", "B");
    }
}
