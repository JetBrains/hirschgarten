load("@rules_java//java:java_library.bzl", "java_library")

java_library(
    name = "annotations",
    srcs = ["NotNull.java"],
)

java_library(
    name = "usage",
    srcs = ["Usage.java"],
    deps = ["@maven//:org_jetbrains_annotations"],
)
