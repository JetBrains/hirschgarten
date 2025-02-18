load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "workspacemodel",
    srcs = glob([
        "main/gen/**/*.kt",
        "main/kotlin/**/*.kt",
    ]),
    resources = glob([
        "main/resources/workspace-model-entities-folder-marker",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/annotations",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/utils",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_code_gson_gson",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
