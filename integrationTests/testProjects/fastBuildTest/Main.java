package example;

public class Main {
    public static void main(String[] args) {
        Calculator calculator = new Calculator();
        System.out.println("Before hotswap");
        System.out.println("2 + 2 = " + calculator.add(2, 2));
    }
}

