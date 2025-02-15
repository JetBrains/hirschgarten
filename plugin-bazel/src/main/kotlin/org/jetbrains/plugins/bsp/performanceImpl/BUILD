load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "performanceImpl",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/performance",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/projectAware",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/status",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/target",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/widgets",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/performance",
    ],
)
