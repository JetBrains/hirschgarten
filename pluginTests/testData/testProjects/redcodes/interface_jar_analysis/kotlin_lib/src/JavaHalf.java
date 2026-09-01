package com.example.kotlinlib;

public final class JavaHalf {

  private static int counter;

  public static int next() {
    return ++counter;
  }

  private JavaHalf() {
  }
}
