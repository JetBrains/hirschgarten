package com.example;

import com.example.gen.Gen1;
import com.example.gen.Gen2;

public class App {
    public String use() {
      return Gen1.value() + Gen2.value();
    }
}
