public class FlagPrecedence {
    public static void main(String[] args) {
        String a = System.getProperty("precedence.a", "unset");
        String b = System.getProperty("precedence.b", "unset");
        System.out.println("Flag precedence: a=" + a + " b=" + b);
    }
}
