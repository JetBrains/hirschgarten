load("@rules_java//java:java_test.bzl", "java_test")

java_test(
    name = "addition_test",
    srcs = [
        "AdditionTest.java",
    ],
    test_class = "AdditionTest",
    deps = [
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "multiplication_test",
    srcs = [
        "MultiplicationTest.java",
    ],
    test_class = "MultiplicationTest",
    deps = [
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "subtraction_test",
    srcs = [
        "SubtractionTest.java",
    ],
    tags = ["manual"],
    test_class = "SubtractionTest",
    deps = [
        "@maven//:junit_junit",
    ],
)
