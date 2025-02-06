load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "debug",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/assets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages",
        "//protocol",
        "//third_party/bazel/src/main/java/com/google/devtools/build/lib/starlarkdebug/proto:starlark_debugging_java_proto",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
