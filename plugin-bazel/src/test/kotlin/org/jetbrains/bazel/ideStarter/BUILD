load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules/testing:ide_starter.bzl", "IDE_STARTER_DEPS")
load("//rules/testing:junit5.bzl", "JUNIT5_DEPS")

kt_jvm_library(
    name = "baseTest",
    srcs = ["IdeStarterBaseProjectTest.kt"],
    visibility = ["//visibility:public"],
    deps = IDE_STARTER_DEPS + JUNIT5_DEPS,
)
