package app;

import com.example.imported.ImportedClass;
import com.example.javalib.JavaLib;
import com.example.javalib.variant.JavaLibVariant;
import com.example.kotlinlib.KotlinLib;
import com.example.kotlinlib.variant.KotlinLibVariant;
import com.example.kotlinlib.variant.KotlinLibWrongFileNameVariant;

public final class App {
  public static String use() {
    return JavaLib.value() +
           JavaLibVariant.value() +
           KotlinLib.INSTANCE.value() +
           KotlinLibVariant.INSTANCE.value() +
           KotlinLibWrongFileNameVariant.INSTANCE.value() +
           ImportedClass.class.getName();
  }
}
