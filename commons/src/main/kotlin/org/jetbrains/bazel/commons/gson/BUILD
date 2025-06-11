load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "gson",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    exports = [
        "@maven//:com_google_code_gson_gson",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
        "@maven//:com_google_code_gson_gson",
    ],
)
