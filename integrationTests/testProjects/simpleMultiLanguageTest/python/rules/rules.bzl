# Copyright 2024 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

load("@rules_python//python:defs.bzl", RulesPythonPyInfo = "PyInfo")

_GENERATED_CONTENT_FILES = """\
CITIES = ["Auckland", "Regensburg", "Darwin", "Toulouse"]


def print_cities():
 for city in CITIES:
     print(f"- {city} City")
"""

_SCRIPT_GENERATE_DIRECTORY = """\
#!/usr/bin/env bash

set -eu -o pipefail

main() {
    if [[ "$#" -lt 1 ]]; then
        echo "expected a single argument of the base directory"
        exit 1
    fi

    local base_dir

    base_dir="$1"

    mkdir -p "${base_dir}/artificial"

    touch "${base_dir}/artificial/__init__.py"
    touch "${base_dir}/__init__.py"

    cat > "${base_dir}/artificial/plastics.py" << EOF
PLASTICS = ["Bakerlite", "Polyethylene", "Nylon"]


def print_plastics():
    for plastic in PLASTICS:
        print(f"# {plastic} Plastic")
EOF
}

main "$@"
"""

_GENERATED_DIRECTORY_CONTENT = """\
PLASTICS = ["Bakerlite", "Polyethylene", "Nylon"]


def print_plastics():
    for plastic in PLASTICS:
        print(f"# {plastic} Plastic")
"""

def _test_codegen_directory_py_impl(ctx):
    if ctx.attr.generate_with_shell_script:
        output_directory = ctx.actions.declare_directory("materials")
        script_file = ctx.actions.declare_file("make_py.sh")

        ctx.actions.write(output = script_file, content = _SCRIPT_GENERATE_DIRECTORY)

        ctx.actions.run(
            mnemonic = "TestCodeGenDirectoryPy",
            executable = script_file,
            arguments = [ctx.actions.args().add(output_directory.path)],
            outputs = [output_directory],
        )

        outputs = [output_directory]
    else:
        plastics = ctx.actions.declare_file("materials/artificial/plastics.py")
        artificial_init = ctx.actions.declare_file("materials/artificial/__init__.py")
        materials_init = ctx.actions.declare_file("materials/__init__.py")
        outputs = [plastics, artificial_init, materials_init]
        ctx.actions.write(output = plastics, content = _GENERATED_DIRECTORY_CONTENT)
        ctx.actions.write(output = artificial_init, content = "")
        ctx.actions.write(output = materials_init, content = "")

    # This would ideally use some normalized path handling here, but it is done here
    # manually assuming a *NIX style system to reduce the complexity of the example.
    imports_path = "/".join([
        ctx.label.repo_name or ctx.workspace_name,
        "python",
        "generated",
        "materials",
    ])

    return [
        DefaultInfo(
            runfiles = ctx.runfiles(files = outputs),
            files = depset(outputs),
        ),
        RulesPythonPyInfo(
            transitive_sources = depset(outputs),
            imports = depset([imports_path]),
        ),
    ]

def _test_codegen_files_py_impl(ctx):
    cities_output_file = ctx.actions.declare_file("infrastructure/urban/cities.py")
    output_files = [cities_output_file]
    ctx.actions.write(output = cities_output_file, content = _GENERATED_CONTENT_FILES)

    def setup_init_file(module_dir_path):
        # This would ideally use some normalized path handling here, but it is done here
        # manually assuming a *NIX style system to reduce the complexity of the example.
        init_file = ctx.actions.declare_file(module_dir_path + "/__init__.py")
        output_files.append(init_file)
        ctx.actions.write(output = init_file, content = "")

    setup_init_file("infrastructure")
    setup_init_file("infrastructure/urban")

    # This would ideally use some normalized path handling here, but it is done here
    # manually assuming a *NIX style system to reduce the complexity of the example.
    imports_path = "/".join([
        ctx.label.repo_name or ctx.workspace_name,
        "python",
        "generated",
        "infrastructure",
    ])

    return [
        DefaultInfo(
            runfiles = ctx.runfiles(files = output_files),
            files = depset(output_files),
        ),
        RulesPythonPyInfo(
            transitive_sources = depset(output_files),
            imports = depset([imports_path]),
        ),
    ]

test_codegen_directory_py = rule(
    implementation = _test_codegen_directory_py_impl,
    attrs = {
        # The directory-generating script action needs bash, which Windows agents
        # lack; the plain-file mode writes equivalent sources without a directory output.
        "generate_with_shell_script": attr.bool(default = True),
    },
    provides = [DefaultInfo, RulesPythonPyInfo],
    doc = """\
    Produces a Python code-generation library to demonstrate production of a directory of files.
    """,
)

test_codegen_files_py = rule(
    implementation = _test_codegen_files_py_impl,
    provides = [DefaultInfo, RulesPythonPyInfo],
    doc = """\
    Produces a Python code-generation library to demonstrate production of files.
    """,
)
