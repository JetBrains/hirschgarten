load("@rules_intellij//intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "v242",
    srcs = select_for_plugin_api({
        "intellij-2024.2": glob([
            "org/jetbrains/plugins/**",
        ]),
        "intellij-2024.3": ["Dummy.kt"],
    }),
    visibility = ["//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/android/sdkcompat:__pkg__"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/android",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/android",
    ],
)
