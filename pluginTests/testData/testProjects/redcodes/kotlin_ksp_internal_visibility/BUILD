load("@rules_kotlin//kotlin:core.bzl", "kt_ksp_plugin")
load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_ksp_plugin(
    name = "table_processor",
    processor_class = "com.example.processor.TableProcessorProvider",
    deps = ["//helper:processor"],
)

# Bazel compiles Consumer.kt and the KSP output as one Kotlin module, so `internal` crosses freely
# between them. The KSP source jar has to become a source root of this module for the IDE to agree.
kt_jvm_library(
    name = "consumer",
    srcs = ["Consumer.kt"],
    plugins = [":table_processor"],
    deps = ["//helper:annotations"],
)
