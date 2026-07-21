package com.example.greeting;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GreetingModule {
  @Bean("greetingPrefix")
  String greetingPrefix() {
    return "Hello";
  }

  @Bean
  GreetingService greetingService(
      @Qualifier("greetingPrefix") String greetingPrefix) {
    return new GreetingService(greetingPrefix);
  }
}

