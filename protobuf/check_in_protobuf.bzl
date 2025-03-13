load("@aspect_bazel_lib//lib:output_files.bzl", "output_files")
load("@aspect_bazel_lib//lib:run_binary.bzl", "run_binary")
load("@aspect_bazel_lib//lib:write_source_files.bzl", "write_source_files")

def check_in_protobuf(name, jars, tool = ":extract_bin"):
    """
    Creates a set of output_files(...) rules for each srcjar in `jars`,
    then runs your shell script on them, then copies the results.

    Each element of `jars` is a dict with:
      {
        "label": "//some:target",
        "path": "my/path/to/foo-speed-src.jar",
      }

    The shell script `tool` presumably unzips or processes the jar and places outputs in "out".

    Usage:
      check_in_protobuf(
          name = "check_in_protobuf",
          tool = ":extract_bin",  # Your shell script
          jars = [
              {
                  "label": "//server:some_proto",
                  "path": "server/whatever/some_proto-speed-src.jar",
              },
              {
                  "label": "@googleapis//google/devtools/build/v1:build_java_proto",
                  "path": "../googleapis+/google/devtools/build/v1/build_proto-speed-src.jar",
              },
              ...
          ],
      )
    """

    # We'll collect the names of all 'output_files' rules we create
    output_labels = []

    # We'll collect the "$(location :xxx)" expansions for the run_binary args
    location_args = []

    for i, jar_info in enumerate(jars):
        label = jar_info["label"]
        path = jar_info["path"]

        # Generate a unique rule name using the index
        rule_name = "jar_{}".format(i)

        # 1) Create an output_files(...) rule for this jar
        output_files(
            name = rule_name,
            paths = [path],
            target = label,
        )
        output_labels.append(":" + rule_name)
        location_args.append("$(location :" + rule_name + ")")

    # 2) Create a run_binary(...) to invoke your shell script with all jars
    run_binary_name = name + "_extract"
    run_binary(
        name = run_binary_name,
        tool = tool,  # e.g. ":extract_bin"
        srcs = output_labels,  # All jar outputs as inputs
        args = ["$@"] + location_args,
        out_dirs = ["out"],  # Adjust if your script outputs elsewhere
    )

    # 3) Use write_source_files(...) to copy from the run_binary output
    write_source_files(
        name = name,
        files = {
            "src/main/gen": ":" + run_binary_name,
        },
    )
