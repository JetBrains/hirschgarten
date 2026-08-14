package pro.bravit.calc.cli;

import java.util.Scanner;

public class CalcCli {
    public static void main(String[] args) {

        System.out.println("Enter your name:");
        Scanner scanner = new Scanner(System.in);

        String name = scanner.nextLine();

        System.out.println("Hello " + name + "!");
    }
}