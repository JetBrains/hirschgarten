public class A1 {
    public void usesB() {
        B b = null;
        var c1 = b.getC();  // Compiles because var
        b.getC().fooBar(); // No error
        <warning descr="Using type C from an indirect dependency @//:C">C</warning> c2 = null;
    }
}
