load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "extensionPoints",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/annotations",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//rules_intellij/third_party/android",
        "//rules_intellij/third_party/code_with_me",
        "//rules_intellij/third_party/devkit",
        "//rules_intellij/third_party/performance",
        "//rules_intellij/third_party/python",
        "//sdkcompat",
        "@maven//:com_google_code_gson_gson",
    ],
)
