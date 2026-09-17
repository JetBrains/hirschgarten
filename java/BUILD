# Mock JDK for the IntelliJ test framework, at the runfiles path IdeaTestUtil expects: _main/java/mockJDK-1.7/...
# (the monorepo's jps_test adds @community//java:mockJDK the same way). Files come from http_file rules in MODULE.bazel.
genrule(
    name = "mockJDK_layout",
    testonly = True,
    srcs = [
        "@mock_jdk_17_rt_jar//file",
        "@mock_jdk_17_rt_jar_license//file",
        "@mock_jdk_17_src_zip//file",
    ],
    outs = [
        "mockJDK-1.7/jre/lib/rt.jar",
        "mockJDK-1.7/jre/lib/rt.jar_LICENSE",
        "mockJDK-1.7/src.zip",
    ],
    cmd = " && ".join([
        "cp $(execpath @mock_jdk_17_rt_jar//file) $(execpath mockJDK-1.7/jre/lib/rt.jar)",
        "cp $(execpath @mock_jdk_17_rt_jar_license//file) $(execpath mockJDK-1.7/jre/lib/rt.jar_LICENSE)",
        "cp $(execpath @mock_jdk_17_src_zip//file) $(execpath mockJDK-1.7/src.zip)",
    ]),
)

filegroup(
    name = "mockJDK",
    testonly = True,
    srcs = [":mockJDK_layout"],
    visibility = ["//visibility:public"],
)
