#!/usr/bin/env python3
"""
gen_sdk_test_deps — generate pluginTests/sdk_test_deps.bzl: the SDK plugin and jars that
the mirror's //pluginTests needs on its classpath.

usage:
    ./gen_sdk_test_deps.py            # after a dependency change or an SDK bump
"""

import io
import json
import posixpath
import re
import subprocess
import sys
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path
from typing import Optional

EXCLUDED_JARS = (
  # Otherwise SpringBootProjectTest doesn't pass
  "plugins/Spring/lib/modules/intellij.spring.graph.jar",
)

PLUGIN_DIR = Path(__file__).resolve().parents[2]
REPO_ROOT = PLUGIN_DIR.parents[1]
MODULE_BAZEL = PLUGIN_DIR / "~MODULE.bazel"
OUT_FILE = PLUGIN_DIR / "pluginTests" / "sdk_test_deps.bzl"
CACHE_DIR = Path.home() / ".cache" / "hirschgarten"

SEED_QUERY = ("kind('jvm_library|jvm_import|ij_plugin_module|java_library', "
              "deps(//plugins/bazel/pluginTests:pluginTests_test_lib))")

IGNORED_PREFIXES = ("intellij.bazel", "intellij.libraries.bazel", "intellij.monorepo.devkit")

# The monorepo and the SDK name one plugin descriptor module differently.
SDK_RENAMES = {"intellij.kotlin.plugin": "kotlin.plugin"}

SEED_SUFFIXES = ("", "_$legacy_jps_module", "_$legacy_jps_library")

# A module key -> the paths of the jars it lives in.
ModuleJars = dict[str, list[str]]


def read_sdk_pin() -> tuple[str, str]:
  text = MODULE_BAZEL.read_text()
  base = re.search(r'^SDK_262_BASE_VERSION = "(.+)"', text, re.M).group(1)
  build = re.search(r'^SDK_262_INTELLIJ_VERSION = SDK_262_BASE_VERSION \+ "(.+)"', text, re.M).group(1)
  repository = re.search(r'^SDK_262_INTELLIJ_REPOSITORY = "(.+)"', text, re.M).group(1)
  template = re.search(r'^IC_262_URL = "(.+)" %', text, re.M).group(1)

  version = base + build
  return template % (repository, version, version), version


def download_sdk(url: str, version: str) -> Path:
  """Download the SDK zip once, and delete the zip of every other version. One zip is about 1.5 GB."""
  archive = CACHE_DIR / f"sdk-{version}.zip"
  if not archive.exists():
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    print(f"# downloading {url}", flush=True)
    part = archive.with_name(archive.name + ".part")
    urllib.request.urlretrieve(url, part)
    part.rename(archive)
  for stale in CACHE_DIR.glob("sdk-*.zip"):
    if stale != archive:
      print(f"# removing the stale cache {stale.name}")
      stale.unlink()
  return archive


def seed_names() -> set[str]:
  """The JPS module names in the transitive closure of //pluginTests."""
  cmd = ["bazel", "query", SEED_QUERY, "--output=xml", "--noshow_progress"]
  print(f"$ {' '.join(cmd)}", flush=True)
  output = subprocess.run(cmd, cwd=REPO_ROOT, capture_output=True, check=True).stdout
  names = set()
  for rule in ET.fromstring(output).findall("rule"):
    module_name = next((attribute.get("value") for attribute in rule.findall("string")
                        if attribute.get("name") == "module_name"), None)
    if module_name is None:
      continue  # no JPS name: one of our own targets, or the test library of another module
    name = SDK_RENAMES.get(module_name, module_name)
    if not name.startswith(IGNORED_PREFIXES):
      names.add(name)
  return names


def module_key(name: str, namespace: Optional[str]) -> str:
  """The entry name rule of module-descriptors.jar: <name>.xml, or <name>_<namespace>.xml for other namespaces."""
  return name if namespace in (None, "jetbrains") else f"{name}_{namespace}"


def read_module_jars(descriptors_jar: zipfile.ZipFile) -> ModuleJars:
  """Read the open modules/module-descriptors.jar into module key -> jar paths. Skips the plugin descriptors."""
  module_jars: ModuleJars = {}
  for entry in descriptors_jar.namelist():
    if not entry.endswith(".xml") or entry.startswith("plugins/"):
      continue
    root = ET.fromstring(descriptors_jar.read(entry))
    key = module_key(root.get("name"), root.get("namespace"))
    module_jars[key] = [posixpath.normpath(posixpath.join("modules", resource.get("path")))
                        for resource in root.iter("resource-root")]
  return module_jars


def resolve_jars(module_jars: ModuleJars, seeds: set[str]) -> tuple[set[str], set[str]]:
  """The jars of the seeds that the SDK ships, and the seeds it has no module for."""
  jars, unknown = set(), set()
  for name in seeds:
    key = next((name + suffix for suffix in SEED_SUFFIXES if name + suffix in module_jars), None)
    if key is None:
      unknown.add(name)
    else:
      jars.update(module_jars[key])
  return jars, unknown


def close_over_plugin_dirs(jars: set[str], sdk_entries: set[str]) -> set[str]:
  dirs = {jar.split("/")[1] for jar in jars if jar.startswith("plugins/")}

  matched = set()
  for entry in sdk_entries:
    parts = entry.split("/")
    if not entry.endswith(".jar") or parts[0] != "plugins" or parts[1] not in dirs:
      continue
    is_lib_jar = len(parts) == 4 and parts[2] == "lib"
    is_module_jar = len(parts) == 5 and parts[2:4] == ["lib", "modules"]
    if is_lib_jar or is_module_jar:
      matched.add(entry)

  return matched


def write_output(version: str, jars: list[str], ignored_jars: list[str], absent_modules: list[str]) -> None:
  jar_lines = "".join(f'"{jar}",\n' for jar in jars)

  OUT_FILE.write_text(f'''"""The SDK jars that //pluginTests needs on its classpath.

Generated by tools/infra_scripts/gen_sdk_test_deps.py for {version}. Do not edit.
Closed per plugin directory: once one jar of a plugins/<dir> is in, every lib/*.jar and lib/modules/*.jar of that dir is in.
"""

# The jars of the plugins/ tree of the SDK zip
SDK_TEST_JARS = [
{"".join(f'"{jar}",\n' for jar in jars)}]

# The JAR files excluded from SDK_TEST_JARS. No build file reads this list
EXCLUDED_JARS = [
{"".join(f'"{ignored_jar}",\n' for ignored_jar in ignored_jars)}]

# The SDK zip has no jar for these names. No build file reads this list.
NOT_IN_SDK = [
{"".join(f'"{name}",\n' for name in absent_modules)}]
''', encoding="utf-8")


def main() -> None:
  url, version = read_sdk_pin()
  seeds = seed_names()

  with zipfile.ZipFile(download_sdk(url, version)) as sdk:
    build_number = json.loads(sdk.read("product-info.json"))["buildNumber"]
    if build_number != version:
      sys.exit(f"{url} holds the build {build_number}, but ~MODULE.bazel pins {version}")
    sdk_entries = set(sdk.namelist())
    with zipfile.ZipFile(io.BytesIO(sdk.read("modules/module-descriptors.jar"))) as descriptors:
      module_jars = read_module_jars(descriptors)
  jars, unknown = resolve_jars(module_jars, seeds)
  jars = close_over_plugin_dirs(jars, sdk_entries)

  present, excluded = [], []
  for jar in jars:
    if jar not in EXCLUDED_JARS:
      present.append(jar)
    else:
      excluded.append(jar)
  write_output(version, sorted(present), sorted(excluded), sorted(unknown))
  print(f"# sdk {version}: {len(seeds)} seeds -> {len(present)} jars, {len(excluded)} excluded jars, {len(unknown)} names not in the SDK")
  print(f"# written to {OUT_FILE.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
  main()
