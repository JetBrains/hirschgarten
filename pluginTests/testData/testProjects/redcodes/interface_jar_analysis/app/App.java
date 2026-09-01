package app;

import com.example.kotlinlib.JavaHalf;
import com.example.kotlinlib.KotlinHalf;
import com.example.lib.InterfaceJarLib;

public final class App {

  public static boolean twoCallsDiffer() {
    int first = InterfaceJarLib.next();
    int second = InterfaceJarLib.next();
    return first != second;
  }

  public static boolean kotlinTwoCallsDiffer() {
    int first = KotlinHalf.INSTANCE.next();
    int second = KotlinHalf.INSTANCE.next();
    return first != second;
  }

  public static boolean javaHalfTwoCallsDiffer() {
    int first = JavaHalf.next();
    int second = JavaHalf.next();
    return first != second;
  }

  private App() {
  }
}
