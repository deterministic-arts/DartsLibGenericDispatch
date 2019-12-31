package darts.lib.dispatch;

final class BinarySignature {

    final Class<?> argument1;
    final Class<?> argument2;

    BinarySignature(Class<?> argument1, Class<?> argument2) {
        this.argument1 = argument1;
        this.argument2 = argument2;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        else if (!(o instanceof BinarySignature)) return false;
        else {
            final BinarySignature s = (BinarySignature) o;
            return argument1 == s.argument1 && argument2 == s.argument2;
        }
    }

    @Override
    public int hashCode() {
        return argument1.hashCode() * 31 + argument2.hashCode();
    }

    public boolean implies(BinarySignature s) {
        return s.argument1.isAssignableFrom(argument1)
            && s.argument2.isAssignableFrom(argument2);
    }
}
