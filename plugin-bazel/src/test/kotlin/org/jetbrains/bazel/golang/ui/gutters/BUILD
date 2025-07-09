load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "runLineMarkerContributorTest",
    size = "small",
    src = "BazelGoRunLineMarkerContributorTest.kt",
    associates = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/golang/ui/gutters",
    ],
)
