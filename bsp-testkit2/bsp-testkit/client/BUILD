load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "client",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//bsp-testkit2/bsp-testkit/utils:JsonComparator",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/gson",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/server/connection",
        "//protocol",
        "@maven//:org_junit_jupiter_junit_jupiter_api",
        "@server_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@server_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_test",
    ],
)
