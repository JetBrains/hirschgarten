package com.example.greeting;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public final class SpringWiringTest {
  private SpringWiringTest() {}

  public static void main(String[] args) {
    try (var context = new AnnotationConfigApplicationContext(GreetingModule.class)) {
      String actual = context.getBean(GreetingService.class).greet("Bazel");
      if (!"Hello, Bazel!".equals(actual)) {
        throw new AssertionError("Unexpected greeting: " + actual);
      }
    }
  }
}

