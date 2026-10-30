package com.example.project;

import com.example.project.<error descr="Cannot resolve symbol 'Heavy'">Heavy</error>;

public class Main {
    public static void main(String[] args) {
        System.out.println(<error descr="Cannot resolve symbol 'Heavy'">Heavy</error>.notWorthInspecting("Hello World"));
    }
}
