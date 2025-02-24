load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "languages",
    srcs = glob([
        "**/*.kt",
        "**/*.java",
    ]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/assets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview/base",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview/highlighting",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview/lexer",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/task",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
