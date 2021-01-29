package darts.lib.dispatch;

public final class TernarySignature {

    final Class<?> argument1;
    final Class<?> argument2;
    final Class<?> argument3;

    TernarySignature(Class<?> argument1, Class<?> argument2, Class<?> argument3) {
        this.argument1 = argument1;
        this.argument2 = argument2;
        this.argument3 = argument3;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        else if (!(o instanceof TernarySignature)) return false;
        else {
            final TernarySignature s = (TernarySignature) o;
            return argument1 == s.argument1 && argument2 == s.argument2 && argument3 == s.argument3;
        }
    }

    @Override
    public int hashCode() {
        return argument1.hashCode() * 31 * 31 + argument2.hashCode() * 31 + argument3.hashCode();
    }

    public boolean implies(TernarySignature s) {
        return s.argument1.isAssignableFrom(argument1)
            && s.argument2.isAssignableFrom(argument2)
            && s.argument3.isAssignableFrom(argument3);
    }
}
