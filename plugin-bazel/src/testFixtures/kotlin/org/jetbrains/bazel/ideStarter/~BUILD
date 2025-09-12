load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules/testing:ide_starter.bzl", "IDE_STARTER_DEPS")
load("//rules/testing:junit5.bzl", "JUNIT5_DEPS")

kt_jvm_library(
    name = "baseTest",
    testonly = 1,
    srcs = ["IdeStarterBaseProjectTest.kt"],
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/testing",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/resourceUtil",
    ] + IDE_STARTER_DEPS + JUNIT5_DEPS,
)
