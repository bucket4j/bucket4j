#!/usr/bin/env bash
# Determines which Maven modules need to be built for a pull request, based on
# which files changed. Falls back to a full build whenever a change could
# affect every module (root pom.xml, bucket4j-core, bucket4j-parent, the
# workflow itself, or the wrapper/build config).
set -euo pipefail

BASE_SHA="$1"
HEAD_SHA="$2"

ALWAYS_FULL_BUILD_REGEX='^(pom\.xml|bucket4j-core/|bucket4j-parent/|\.github/workflows/|mvnw|mvnw\.cmd|\.mvn/)'

# Directories that contain a module pom.xml, deepest path first, so prefix
# matching below picks the most specific module for a changed file.
mapfile -t MODULE_DIRS < <(
  find . -mindepth 2 -name pom.xml -not -path '*/target/*' -exec dirname {} \; \
    | sed 's#^\./##' \
    | awk '{ print length, $0 }' | sort -rn | cut -d' ' -f2-
)

CHANGED_FILES=$(git diff --name-only "$BASE_SHA" "$HEAD_SHA")

FULL_BUILD=false
declare -A MODULES_SET

while IFS= read -r file; do
  [ -z "$file" ] && continue

  if [[ "$file" =~ $ALWAYS_FULL_BUILD_REGEX ]]; then
    FULL_BUILD=true
    continue
  fi

  for dir in "${MODULE_DIRS[@]}"; do
    if [[ "$file" == "$dir"/* ]]; then
      MODULES_SET["$dir"]=1
      break
    fi
  done
done <<< "$CHANGED_FILES"

if [ "$FULL_BUILD" = false ]; then
  # A changed module might be an aggregator (e.g. bucket4j-redis) whose own
  # pom.xml was edited - pull its child modules in too, since they're not
  # linked to it via a Maven <dependency> and "-amd" wouldn't find them.
  for module in "${!MODULES_SET[@]}"; do
    for dir in "${MODULE_DIRS[@]}"; do
      if [[ "$dir" == "$module"/* ]]; then
        MODULES_SET["$dir"]=1
      fi
    done
  done
fi

if [ "$FULL_BUILD" = true ] || [ ${#MODULES_SET[@]} -eq 0 ]; then
  echo "full_build=true" >> "$GITHUB_OUTPUT"
  echo "modules=" >> "$GITHUB_OUTPUT"
else
  MODULES=$(IFS=,; echo "${!MODULES_SET[*]}")
  echo "full_build=false" >> "$GITHUB_OUTPUT"
  echo "modules=$MODULES" >> "$GITHUB_OUTPUT"
fi
