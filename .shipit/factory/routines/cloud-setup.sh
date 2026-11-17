#!/bin/bash
# Setup script for the "beansprout" cloud environment. Paste it into the environment's setup script box.
# It runs before Claude starts. The cloud image ships JDK 21, and Beansprout needs JDK 25.
#
# Kit part: everything here. Project part: JDK_VERSION.
# Network: Limited, with api.adoptium.net, github.com and release-assets.githubusercontent.com allowed.
# The Temurin download redirects to GitHub's release asset host.
set -euo pipefail

JDK_VERSION=25
DEST=/opt/jdk-$JDK_VERSION

if [ ! -x "$DEST/bin/java" ]; then
  ARCH=$(uname -m)
  case "$ARCH" in
    x86_64) ARCH=x64 ;;
    aarch64|arm64) ARCH=aarch64 ;;
  esac
  URL="https://api.adoptium.net/v3/binary/latest/$JDK_VERSION/ga/linux/$ARCH/jdk/hotspot/normal/eclipse"
  mkdir -p "$DEST"
  curl -fsSL "$URL" | tar -xz -C "$DEST" --strip-components=1
fi

# Make JDK 25 the default for every shell the session starts.
ln -sf "$DEST/bin/java" /usr/local/bin/java
ln -sf "$DEST/bin/javac" /usr/local/bin/javac
cat > /etc/profile.d/jdk.sh <<EOF
export JAVA_HOME=$DEST
export PATH=$DEST/bin:\$PATH
EOF
grep -q "JAVA_HOME=$DEST" "$HOME/.bashrc" 2>/dev/null || cat /etc/profile.d/jdk.sh >> "$HOME/.bashrc"

"$DEST/bin/java" -version
