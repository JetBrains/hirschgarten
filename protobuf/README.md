# Why is this module needed?

We use a double-build system. Outside of IJ monorepo this plugin is built with Bazel, but
within it JPS is used. Bazel generates protobuf Java libraries automatically, but JPS
is not able to do that. So we need to check in the source code of the libraries generated
by Bazel.

To generate the libraries, run the following command:

```bash
bazel run //protobuf:check_in_protobuf
```

# PORT THIS TO SINGLE SCRIPT