load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "golang",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/impl",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/run",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/startup",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/utils",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/go",
    ],
)
