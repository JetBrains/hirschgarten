#!/usr/bin/env sh

# A fake GCC that only answers the probes of the IDE, any compilation fails.

if [ "$1" = "--version" ]; then
  echo "gcc (cc_false_toolchain) 1.0"
  exit 0
fi

preprocess=0
language="c++"
quote=""
angled=""

while [ $# -gt 0 ]; do
  case "$1" in
    -E) preprocess=1 ;;
    -x) shift; language="$1" ;;
    -x*) language="${1#-x}" ;;
    -iquote) shift; quote="$quote $1" ;;
    -iquote*) quote="$quote ${1#-iquote}" ;;
    -I | -isystem) shift; angled="$angled $1" ;;
    -isystem*) angled="$angled ${1#-isystem}" ;;
    -I*) angled="$angled ${1#-I}" ;;
  esac
  shift
done

[ "$preprocess" = 1 ] || exit 1

# the markers the IDE expects around the predefined macros
echo "___CIDR_DEFINITIONS_END"
echo "___CIDR_FEATURES_START"

# without a language standard the IDE does not send the source to its C/C++ engine
if [ "$language" = "c" ]; then
  echo "{ int ____CIDR_command_SET_LANGUAGE_STANDARD, ____CIDR_command_STANDARD_C = (201710L, 0); }"
else
  echo "{ int ____CIDR_command_SET_LANGUAGE_STANDARD, ____CIDR_command_STANDARD_CPP = (201703L, 0); }"
fi

{
  echo '#include "..." search starts here:'
  for dir in $quote; do echo " $dir"; done
  echo '#include <...> search starts here:'
  for dir in $angled; do echo " $dir"; done
  echo 'End of search list.'
} >&2
