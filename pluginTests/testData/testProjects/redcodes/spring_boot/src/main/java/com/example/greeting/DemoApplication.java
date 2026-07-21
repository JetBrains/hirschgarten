package com.example.greeting;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public final class DemoApplication {
  private DemoApplication() {}

  public static void main(String[] args) {
    try (var context = new AnnotationConfigApplicationContext(GreetingModule.class)) {
      System.out.println(context.getBean(GreetingService.class).greet("Bazel"));
    }
  }
}

