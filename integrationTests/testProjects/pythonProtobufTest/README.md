## Python test repo for JetBrains Bazel plugin (Hirschgarten)

This repo demonstrates some limitations of the bazel plugin currently, mainly around resolving imports for generated Python sources, including from:

1. `py_proto_library`
2. `pycross_wheel_library`

These types of dependencies seem to cause a hard sync failure during Python target processing.
