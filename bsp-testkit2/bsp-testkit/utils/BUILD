load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library")

java_library(
    name = "FlatMapUtils",
    srcs = ["FlatMapUtils.java"],
)

scala_library(
    name = "JsonComparator",
    srcs = ["JsonComparator.scala"],
    visibility = ["//visibility:public"],
    deps = [
        "FlatMapUtils",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_guava",
        "@maven//:org_junit_jupiter_junit_jupiter_api",
    ],
)
