load("@rules_java//java:defs.bzl", "java_test")

package(
    default_visibility = [":visibility"],
)

package_group(
    name = "visibility",
    packages = [
        "//shared",
    ],
)

java_test(
    name = "VcsStateTest",
    size = "small",
    srcs = ["VcsStateTest.java"],
    deps = [
        "//commons/src/main/kotlin/com/google/idea/blaze/common/vcs",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "@com_google_guava_guava//jar",
    ],
)
