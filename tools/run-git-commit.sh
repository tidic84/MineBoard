#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
git add CHANGELOG.md README.md build.gradle core docs fabric-1.21.1 fabric-26.2/build.gradle minecraft-26.2 neoforge-1.21.1/build.gradle neoforge-26.2/build.gradle tools
git commit -F tools/commit-message.txt
