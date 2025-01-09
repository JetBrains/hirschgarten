load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "managers",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//server/server:__subpackages__"],
    deps = [
        "//server/bazelrunner",
        "//server/commons",
        "//server/executioncontext/workspacecontext",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bep",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/utils",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/utils:fileutils",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bzlmod",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/diagnostics",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/label",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_guava",
        "@maven//:io_grpc_grpc_protobuf",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_iostreams",
        "@server_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_debug",
        "@maven//:org_apache_velocity_velocity_engine_core",
        "@maven//:org_eclipse_lsp4j_org_eclipse_lsp4j_jsonrpc",
    ],
)
