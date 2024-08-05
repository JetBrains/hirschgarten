#!/bin/bash
# Download the plugin
# TODO use this instead for Marketplace release versions?: https://www.jetbrains.com/help/idea/work-inside-remote-project.html#plugins

PLUGIN_DIR=$(realpath "$CANVAS_IDE_HOME/../ide-plugins")
mkdir -p $PLUGIN_DIR # avoid errors when run in warmup?

# BSP and Bazel plugin nightly channel
curl -L -o $PLUGIN_DIR/bsp-plugin.zip "https://plugins.jetbrains.com/plugin/download?rel=true&pluginId=org.jetbrains.bsp&channel=nightly&takeLatestUpdate=true"
curl -L -o $PLUGIN_DIR/bazel-plugin.zip "https://plugins.jetbrains.com/plugin/download?rel=true&pluginId=org.jetbrains.bazel&channel=nightly&takeLatestUpdate=true"

# Unzip the plugin to the plugins directory of the IDE
unzip $PLUGIN_DIR/bsp-plugin.zip -d $PLUGIN_DIR/
unzip $PLUGIN_DIR/bazel-plugin.zip -d $PLUGIN_DIR/

# Remove the .zip file
rm $PLUGIN_DIR/bsp-plugin.zip
rm $PLUGIN_DIR/bazel-plugin.zip
