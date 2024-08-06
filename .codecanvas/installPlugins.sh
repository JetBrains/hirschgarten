#!/bin/bash

PLUGIN_DIR=$(realpath "$CANVAS_IDE_HOME/../ide-plugins")
mkdir -p $PLUGIN_DIR # avoid errors when run in warmup?

# nightly channel plugins
curl -L -o $PLUGIN_DIR/bsp-plugin.zip "https://plugins.jetbrains.com/plugin/download?rel=true&pluginId=org.jetbrains.bsp&channel=nightly&takeLatestUpdate=true"
curl -L -o $PLUGIN_DIR/bazel-plugin.zip "https://plugins.jetbrains.com/plugin/download?rel=true&pluginId=org.jetbrains.bazel&channel=nightly&takeLatestUpdate=true"
curl -L -o $PLUGIN_DIR/bazel-plugin.zip "https://plugins.jetbrains.com/plugin/download?rel=true&pluginId=org.intellij.scala&channel=nightly&takeLatestUpdate=true"
install_nightly_plugin() {
	local plugin_id=$1
	local plugin_zip="${PLUGIN_DIR}/${plugin_id}.zip"

	curl -L -o $plugin_zip "https://plugins.jetbrains.com/plugin/download?rel=true&pluginId=${plugin_id}&channel=nightly&takeLatestUpdate=true"
	unzip $plugin_zip -d "${PLUGIN_DIR}/"
	rm $plugin_zip
}

install_nightly_plugin "org.jetbrains.bsp"
install_nightly_plugin "org.jetbrains.bazel"
install_nightly_plugin "org.intellij.scala"

# release channel plugins
# installed in CC config
# $CANVAS_IDE_HOME/bin/remote-dev-server.sh installPlugins DevKit
