load("@bazel_skylib//rules:diff_test.bzl", "diff_test")

def resolver_test(name):
    """Creates a resolver test with common boilerplate.

    Args:
        name: Base name for the test targets (infers testdata_glob and expected_output)
    """
    testdata_glob = "testData/" + name + "/**/*.textproto"
    expected_output = "testData/" + name + "/expected_output.txt"

    # Create filegroup for test data
    native.filegroup(
        name = name + "_testdata",
        srcs = native.glob([testdata_glob]),
    )

    # Generate output by running resolver_sanity_runner
    native.genrule(
        name = name + "_output",
        srcs = [":" + name + "_testdata"],
        outs = [name + "_output.txt"],
        cmd = "$(location :resolver_sanity_runner) $(locations :" + name + "_testdata) > $@",
        tools = [":resolver_sanity_runner"],
    )

    # Diff test comparing actual vs expected output
    diff_test(
        name = name + "_diff_test",
        file1 = ":" + name + "_output",
        file2 = expected_output,
    )
