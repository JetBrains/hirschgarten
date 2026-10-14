load("//transitions:defs.bzl", "mode_java_library")

# Each app compiles `//lib:lib` in its own configuration, so each gets its own Kotlin toolchain
mode_java_library(
    name = "app_a",
    srcs = ["AppA.java"],
    mode = "a",
    deps = ["//lib:lib"],
)

mode_java_library(
    name = "app_b",
    srcs = ["AppB.java"],
    mode = "b",
    deps = ["//lib:lib"],
)
