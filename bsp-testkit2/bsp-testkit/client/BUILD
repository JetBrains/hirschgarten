load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library")

scala_library(
    name = "client",
    srcs = glob(["*.scala"]),
    visibility = ["//visibility:public"],
    deps = [
        "//bsp-testkit/utils:JsonComparator",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_code_gson_gson",
        "@maven//:org_eclipse_lsp4j_org_eclipse_lsp4j_jsonrpc",
        "@maven//:org_junit_jupiter_junit_jupiter_api",
        "@maven//:org_scala_lang_scala_library",
    ],
)
