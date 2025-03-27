load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "entities",
    srcs = ["//plugin-bazel/src/main/gen:generated_wsm_entities"] + glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/gson",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/annotations",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/utils",
        "//protocol",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
