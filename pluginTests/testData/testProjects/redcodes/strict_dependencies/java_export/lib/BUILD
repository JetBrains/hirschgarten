load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_jvm_external//:defs.bzl", "java_export")

java_export(
    name = "exported",
    srcs = ["Exported.java"],
    maven_coordinates = "com.example:exported:1.0.0",
    visibility = ["//visibility:public"],
    deps = [":internal_a"],
)

java_library(
    name = "internal_a",
    srcs = ["InternalA.java"],
    visibility = ["//visibility:public"],
)

java_export(
    name = "aggregator",
    maven_coordinates = "com.example:aggregator:1.0.0",
    visibility = ["//visibility:public"],
    exports = [":internal_b"],
)

java_library(
    name = "internal_b",
    srcs = ["InternalB.java"],
    visibility = ["//visibility:public"],
)

java_library(
    name = "strict_consumer",
    srcs = ["StrictConsumer.java"],
    visibility = ["//visibility:public"],
    deps = [":strict_dep"],
)

java_library(
    name = "strict_dep",
    srcs = ["StrictDep.java"],
    visibility = ["//visibility:public"],
)
