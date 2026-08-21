public final class DefaultPackageApp {
  public static String use() {
    return JavaLibDefaultPackage.value() +
           KotlinLibDefaultPackage.INSTANCE.value();
  }
}
