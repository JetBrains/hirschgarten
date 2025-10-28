load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sync",
    srcs = glob(["*.kt"]),
    associates = ["//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/projectStructure/legacy"],
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/golang/resolve",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/libraries:sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target/sync/projectStructure",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//rules_intellij/third_party/go",
    ],
)
