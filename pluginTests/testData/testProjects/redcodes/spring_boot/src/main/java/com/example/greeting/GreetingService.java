package com.example.greeting;

public final class GreetingService {
  private final String greetingPrefix;

  GreetingService(String greetingPrefix) {
    this.greetingPrefix = greetingPrefix;
  }

  public String greet(String name) {
    return greetingPrefix + ", " + name + "!";
  }
}

