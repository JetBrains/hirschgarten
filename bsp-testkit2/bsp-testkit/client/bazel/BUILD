load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library")

scala_library(
    name = "bazel",
    srcs = glob(["*.scala"]),
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:com_github_zpp_This_is_fine_build_server_protocol_bsp4j",
        "//bsp-testkit/client"
    ],
)
