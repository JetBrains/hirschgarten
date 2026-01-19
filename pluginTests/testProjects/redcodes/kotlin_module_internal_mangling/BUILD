load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "A",
    module_name = "a_module",
    srcs = ["A.kt"],
)

kt_jvm_library(
    name = "B",
    srcs = ["B.java"],
    deps = ["//:A"]
)