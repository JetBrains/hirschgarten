load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

kt_jvm_library(
    name = "v251",
    srcs = select_for_plugin_api({
        "intellij-2025.1": glob([
            "org/jetbrains/bazel/sdkcompat/**",
        ]),
        "intellij-2025.2": ["Dummy.kt"],
    }),
    visibility = ["//sdkcompat:__pkg__"],
    exports = [
        "//sdkcompat/v251/org/jetbrains/bazel/sdkcompat/workspacemodel/entities",
    ],
    deps = [
        "//rules_intellij/intellij_platform_sdk:java",
        "//rules_intellij/intellij_platform_sdk:kotlin",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//rules_intellij/third_party/android",
    ],
)
