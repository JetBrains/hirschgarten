load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library")

scala_library(
    name = "bazel",
    srcs = glob(["*.scala"]),
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:ch_epfl_scala_bsp4j_2_13",
        "//bsp-testkit/client"
    ],
)
