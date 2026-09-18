load("@rules_java//java:java_library.bzl", "java_library")

java_library(
    name = "lib",
    srcs = ["Lib.java"],
)

# Bazel skips this target when a wildcard selects it, and fails when a label requests it.
java_library(
    name = "incompatible",
    srcs = ["Incompatible.java"],
    target_compatible_with = ["@platforms//:incompatible"],
)
