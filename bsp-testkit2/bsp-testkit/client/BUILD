load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library")

kt_jvm_library(
    name = "client",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//bsp-testkit2/bsp-testkit/utils:JsonComparator",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/server/connection",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_code_gson_gson",
        "@maven//:org_eclipse_lsp4j_org_eclipse_lsp4j_jsonrpc",
        "@maven//:org_junit_jupiter_junit_jupiter_api",
        "@server_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@server_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_test",
    ],
)
