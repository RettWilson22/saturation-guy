#!/bin/sh
# Builds the mod with the Java 25 that ships inside the Minecraft launcher.
# Output: build/libs/saturation-guy-<version>.jar
cd "$(dirname "$0")"
export JAVA_HOME="$HOME/Library/Application Support/minecraft/runtime/java-runtime-epsilon/mac-os-arm64/java-runtime-epsilon/jre.bundle/Contents/Home"
exec ./gradlew build "$@"
