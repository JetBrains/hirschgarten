load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_import", "kt_jvm_library")

kt_jvm_import(
    name = "annotations_import",
    jars = ["@annotations_http//file"],
    visibility = ["//visibility:public"],
)

kt_jvm_library(
    name = "a-lib",
    srcs = glob(["src/main/kotlin/com/**/*.kt"]),
    resources = glob([
        "src/main/resources/**/*",
        "src/main/java/**/*",
        "src/main/kotlin/messages/**/*",
        "src/main/kotlin/com/example/*.properties",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        ":annotations_import",
        ":foo-res",
        ":standalone-res",
    ],
)

kt_jvm_library(
    name = "foo-res",
    resource_strip_prefix = "module/src/main/foo-res",
    resources = glob(["src/main/foo-res/**/*"]),
)

kt_jvm_library(
    name = "standalone-res",
    resources = ["standalone/StandaloneBundle.properties"],
)
