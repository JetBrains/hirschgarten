import hello.HelloOuterClass.Hello;

public final class App {
  public static Hello greet() {
    return Hello.newBuilder().setName("world").build();
  }
}
