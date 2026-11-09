#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
sha256sum --check docs/reference/audio-sha256.txt
sha256sum --check docs/reference/icons-sha256.txt
if [[ $# -gt 0 ]]; then
  apk=$1
  while read -r expected path; do
    actual=$(unzip -p "$apk" "assets/sounds/$(basename "$path")" | sha256sum | cut -d' ' -f1)
    [[ "$expected" == "$actual" ]] || { echo "APK audio mismatch: $path" >&2; exit 1; }
  done < docs/reference/audio-sha256.txt
  echo 'All 14 packaged audio files match upstream bytes.'
fi
