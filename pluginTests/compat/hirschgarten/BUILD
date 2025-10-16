load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules/testing:ide_starter.bzl", "IDE_STARTER_DEPS")

kt_jvm_library(
    name = "hirschgarten",
    testonly = True,
    srcs = ["PluginTestsCompat.kt"],
    visibility = ["//visibility:public"],
    deps = [
        "//rules_intellij/testing:lib",
        "//rules_intellij/testing:runfiles",
    ],
)
