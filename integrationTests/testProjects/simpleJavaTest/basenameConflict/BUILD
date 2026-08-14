load("@rules_jvm_external//:defs.bzl", "artifact")

java_test(
    name = "basename",
    srcs = ["SimpleTest.java"],
    test_class = "org.example.basenameConflict.SimpleTest",
    deps = [
        "@maven//:junit_junit",
    ],
    visibility = ["//visibility:public"],
)

java_test(
    name = "nested/basename",
    srcs = ["nested/SimpleTest.java"],
    test_class = "org.example.basenameConflict.nested.SimpleTest",
    deps = [
        "@maven//:junit_junit",
    ],
    visibility = ["//visibility:public"],
)