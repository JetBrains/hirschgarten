## Bazel "internal" libraries

Those libraries are self-contained parts of Bazel itself. They are generated using an custom cli tool.
The tool is using special bazel queries to extract all needed jars to run specific part of Bazel. 
Then the tool bundles all required jars together and relocates 3rd-party libraries and bazel classes to version specific package.
Because of that we can use different versions of bazel internals without any class loader magic.
Also Bazel as hermetic jars make it easy to handle different versions of Bazel dependencies.

## Rocksdb native Jars

rocksdb-jni comes as set of platform specific jars. To automatically download and extract all required libraries.
This step is required because rocksdb-jni also ships jars for very niche architectures/libc impls.
So to reduce size of the final jar we need to extract only required jars.

Tools can be found here:
https://git.jetbrains.team/bazel/bazel-internals.git