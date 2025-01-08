#!/bin/bash

PLUGIN_DIR=$(realpath "$CANVAS_IDE_HOME/../ide-plugins")
mkdir -p $PLUGIN_DIR # avoid errors when run in warmup?

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
