package base;

import dep.DepLib;

public final class Base {
  public static String name() {
    return "base(" + DepLib.tag() + ")";
  }
}
