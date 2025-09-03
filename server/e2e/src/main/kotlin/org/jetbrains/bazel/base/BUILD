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
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//server/install/src/main/kotlin/org/jetbrains/bazel/install",
        "@server_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_test",
    ],
)
