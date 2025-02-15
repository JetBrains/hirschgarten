load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "server",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = [
        #         "//plugin-bazel/bspcli:__subpackages__",
        #         "//plugin-bazel/install:__subpackages__",
        #         "//plugin-bazel/server:__subpackages__",
        "//visibility:public",
    ],
    deps = [
        "//plugin-bazel/bazelrunner",
        "//plugin-bazel/commons",
        "//plugin-bazel/executioncontext/workspacecontext",
        "//plugin-bazel/logger",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bep",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/info",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/managers",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/utils",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bzlmod",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/diagnostics",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/firstPhase",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_core",
        "@maven//:org_eclipse_lsp4j_org_eclipse_lsp4j_jsonrpc",
    ],
)
