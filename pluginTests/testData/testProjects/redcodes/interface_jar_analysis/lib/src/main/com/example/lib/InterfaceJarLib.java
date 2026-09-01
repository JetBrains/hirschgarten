package com.example.lib;

public final class InterfaceJarLib {

  private static int counter;

  public static int next() { return ++counter;
  }

  private InterfaceJarLib() {
  }
}
