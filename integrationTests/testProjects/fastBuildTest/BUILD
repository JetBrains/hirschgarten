load("@rules_java//java:java_binary.bzl", "java_binary")
load("@rules_java//java:java_library.bzl", "java_library")

java_library(
    name = "calculator",
    srcs = ["Calculator.java"],
)

java_binary(
    name = "main",
    srcs = ["Main.java"],
    main_class = "example.Main",
    deps = [":calculator"],
)
