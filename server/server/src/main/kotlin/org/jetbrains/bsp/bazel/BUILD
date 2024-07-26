load("@bazel_sonatype//:defs.bzl", "sonatype_java_export")

sonatype_java_export(
    name = "bsp",
    maven_coordinates = "org.jetbrains.bsp:bazel-bsp:3.2.0",
    maven_profile = "org.jetbrains",
    pom_template = "//server/server/src/main/resources:pom.xml",
    resources = ["//server/log4j_config"],
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//server/commons",
        "//server/install:install-lib",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server",
    ],
)

java_library(
    name = "bsp_export",
    visibility = ["//visibility:public"],
    exports = [
        "//server/commons",
        "//server/install:install-lib",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server",
    ],
)
