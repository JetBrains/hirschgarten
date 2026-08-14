package com.example;

import java.util.Arrays;

class Main {
    public static void main(String[] args) {
        System.out.println("2 + 2 = " + Calculator.add(2, 2));
        System.out.println("MODULE.bazel from envs: " + System.getenv("MODULE.bazel"));
        System.out.println("Args: " + Arrays.toString(args));
        System.out.println("PARENT_ENV_HOME=" + System.getenv("HOME"));
        System.out.println("PARENT_ENV_USERPROFILE=" + System.getenv("USERPROFILE"));
    }
}
