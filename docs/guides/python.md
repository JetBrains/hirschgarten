# Enabling Python support


In order for the new Bazel plugin to recognise your Python targets, the support needs to be enabled.
Having opened your project using the plugin, follow these steps:

1. Make sure the [Python plugin](https://plugins.jetbrains.com/plugin/631-python) is installed and enabled.
2. Make sure `bsp.python.support` option is enabled (it's on by default)
   1. Open the search menu, either by double-pressing Shift or using Navigate > Search Everywhere.
   2. Search for `Registry` and open it.
   3. Locate the flag by the `bsp.python.support` key and enable it.
3. Resync the project.
