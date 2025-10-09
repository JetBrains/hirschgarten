package org.jetbrains.bazel.test.runner;

//import com.github.bazel_contrib.contrib_rules_jvm.junit5.JUnit5Runner;
import org.jetbrains.bazel.test.Test;
import org.jetbrains.bazel.test.TestData;
import org.junit.platform.console.ConsoleLauncher;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

public class TestRunner {
  private static final String TEST_CLASSES_FILE_NAME = "test_metadata.proto";

  private String testClassName;

  public static void main(String[] args) throws IOException {
    new TestRunner().launch(args);
  }

  private void launch(String[] args) throws IOException {
    this.setupEnv();
    final var list = new ArrayList<String>();
    Collections.addAll(list, args);
    list.add("--select-class=" + this.testClassName);
    list.add("--fail-if-no-tests");
    //list.add("-e");
    //list.add("junit-platform-suite");
    ConsoleLauncher.main(list.toArray(String[]::new));
  }

  private void setupEnv() throws IOException {
    final var loader = this.getClass().getClassLoader();
    try (final var steam = loader.getResourceAsStream(TEST_CLASSES_FILE_NAME)) {
      if (steam == null) {
        throw this.createError();
      }

      final var testData = TestData.parseFrom(steam);
      final var tests = testData.getTestsList();
      if (tests.isEmpty()) {
        throw this.createError();
      }

      for (var test : tests) {
        final Class<?> klass = this.loadClassOrNull(loader, test.getClassName());
        if (klass != null) {
          this.setupWithTest(test, klass);
          break;
        }
      }
    }
  }

  private void setupWithTest(Test test, Class<?> klass) {
    this.testClassName = test.getClassName();
    //System.setProperty("bazel.test_suite", test.getClassName());
  }

  private Class<?> loadClassOrNull(ClassLoader loader, String name) {
    try {
      return Class.forName(name, false, loader);
    }
    catch (ClassNotFoundException e) {
      try {
        return Class.forName(name);
      }
      catch (ClassNotFoundException ignored) {

      }
    }
    return null;
  }

  private RuntimeException createError() {
    return new RuntimeException("No tests found, annotate your test class with " +
                                  "`@BazelTest` to make it visible to the runner.");
  }
}