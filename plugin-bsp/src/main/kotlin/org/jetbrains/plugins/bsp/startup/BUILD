load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "startup",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bsp:__subpackages__"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/extensionPoints",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/notifications",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/widgets",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/widgets/fileTargets",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/utils",
    ],
)
