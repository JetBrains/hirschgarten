load("@rules_java//java:java_library.bzl", "java_library")

java_library(
    name = "leaf",
    srcs = select({
        "//settings:mode_a": ["a/Leaf.java"],
        "//settings:mode_b": ["b/Leaf.java"],
    }),
    visibility = ["//visibility:public"],
)
