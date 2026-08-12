load("@rules_java//java:java_test.bzl", "java_test")

java_test(
    name = "calculator_test",
    srcs = [
        "CalculatorTest.java",
    ],
    test_class = "com.example.CalculatorTest",
    deps = [
        "//src/main/com/example:calculator",
        "//src/main/org/other_package",
        "@maven//:junit_junit",
    ],
)
