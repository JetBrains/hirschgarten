load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

java_library(
    name = "class1",
    srcs = ["Class1.java"],
    deps = [":class2"],
)

kt_jvm_library(
    name = "class2",
    srcs = ["Class2.kt"],
    deps = [":class3"],
)

java_library(
    name = "class3",
    srcs = ["Class3.java"],
)
