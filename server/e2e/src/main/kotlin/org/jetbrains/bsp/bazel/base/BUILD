load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "base",
    srcs = glob(["*.kt"]),
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    runtime_deps = [
        "//server/install",
    ],
    deps = [
        "//protocol",
        "//server/install/src/main/kotlin/org/jetbrains/bsp/bazel/install",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_core",
        "@server_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_test",
        "@testkit//:bazel-testkit-client",
        "@testkit//:testkit-client",
    ],
)
