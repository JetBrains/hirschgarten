package app;

import gen.Message;
import gen.Stub;

public final class App {
  public static String use() {
    return Message.value() + ":" + Stub.value();
  }
}
