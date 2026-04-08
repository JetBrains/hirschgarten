load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_java//java:java_plugin.bzl", "java_plugin")
load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

java_library(
    name = "lib",
    srcs = glob(["src/**/*.java"]),
    visibility = ["//visibility:public"],
)

java_plugin(
    name = "processor",
    processor_class = "com.example.processor.HelperProcessor",
    generates_api = True,
    deps = [":lib"],
    visibility = ["//visibility:public"],
)