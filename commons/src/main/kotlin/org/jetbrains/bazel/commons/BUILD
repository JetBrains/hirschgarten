load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "commons",
    srcs = glob(
        [
            "**/*.kt",
            "**/*.java",
        ],
        allow_empty = True,
    ),
    visibility = ["//visibility:public"],
    exports = ["//commons/src/main/kotlin/org/jetbrains/bazel/label"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
)
