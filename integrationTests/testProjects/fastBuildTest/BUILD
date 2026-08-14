load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_java//java:java_binary.bzl", "java_binary")

java_library(
    name = "calculator",
    srcs = ["Calculator.java"],
)

java_binary(
    name = "main",
    srcs = ["Main.java"],
    deps = [":calculator"],
    main_class = "example.Main",
)

