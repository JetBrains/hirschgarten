load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_import", "kt_jvm_library")

kt_jvm_library(
    name = "A",
    srcs = [
        "A1.java",
        "A2.kt",
    ],
    deps = [
        "//:B",
    ],
)

java_library(
    name = "B",
    srcs = [
        "B.java",
    ],
    deps = [
        "//:C",
    ],
)

java_library(
    name = "C",
    srcs = [
        "C.java",
    ],
)
