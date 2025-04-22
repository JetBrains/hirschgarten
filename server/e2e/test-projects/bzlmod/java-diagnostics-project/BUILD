load("@rules_java//java:java_library.bzl", "java_library")

java_library(
    name = "library",
    srcs = ["Library.java"],
)

java_library(
    name = "deprecated_warning",
    srcs = ["DeprecatedWarning.java"],
    deps = [":library"],
)

java_library(
    name = "no_such_method_error",
    srcs = ["NoSuchMethodError.java"],
)

java_library(
    name = "warning_and_error",
    srcs = ["WarningAndError.java"],
    deps = [":library"],
)
