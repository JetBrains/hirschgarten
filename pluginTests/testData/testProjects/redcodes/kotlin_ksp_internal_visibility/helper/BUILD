load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "annotations",
    srcs = ["src/com/example/processor/GenerateTable.kt"],
    visibility = ["//visibility:public"],
)

kt_jvm_library(
    name = "processor",
    srcs = [
        "src/com/example/processor/TableProcessor.kt",
        "src/com/example/processor/TableProcessorProvider.kt",
    ],
    # KSP2 discovers SymbolProcessorProvider implementations with a ServiceLoader.
    resource_strip_prefix = "helper/resources",
    resources = glob(["resources/**"]),
    visibility = ["//visibility:public"],
    deps = [
        ":annotations",
        "@rules_kotlin//kotlin/compiler:symbol-processing-api",
    ],
)
