#!/usr/bin/env bash
set -euo pipefail

packages=(
  xvfb
  libgl1
  libglx-mesa0
  libgl1-mesa-dri
  libx11-6
  libxext6
  libxrender1
  libxi6
  libxrandr2
  libxcursor1
  libxinerama1
  libxxf86vm1
)

missing=()
for package in "${packages[@]}"; do
  if ! dpkg-query -W -f='${Status}' "$package" 2>/dev/null | grep -q "install ok installed"; then
    missing+=("$package")
  fi
done

if [ "${#missing[@]}" -gt 0 ]; then
  sudo apt-get update
  sudo apt-get install -y --no-install-recommends "${missing[@]}"
fi
