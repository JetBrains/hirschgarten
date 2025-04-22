load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "entities",
    srcs = ["//plugin-bazel/src/main/gen:generated_wsm_entities"] + glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/annotations",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/gson",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/utils",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
