#!/usr/bin/env sh

# A fake GCC that only answers the probes of the IDE, any compilation fails.

if [ "$1" = "--version" ]; then
  echo "gcc (cc_false_toolchain) 1.0"
  exit 0
fi

preprocess=0
quote=""
angled=""

while [ $# -gt 0 ]; do
  case "$1" in
    -E) preprocess=1 ;;
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

{
  echo '#include "..." search starts here:'
  for dir in $quote; do echo " $dir"; done
  echo '#include <...> search starts here:'
  for dir in $angled; do echo " $dir"; done
  echo 'End of search list.'
} >&2
