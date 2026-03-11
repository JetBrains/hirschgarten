load("@rules_java//java:java_import.bzl", "java_import")
load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

java_import(
    name = "kotlin-stdlib",
    jars = [":kotlin-stdlib.jar"],
)

define_kt_toolchain(
    name = "kotlin_toolchain",
    jvm_runtime = [":kotlin-stdlib"],
    jvm_stdlibs = [":kotlin-stdlib"],
)
