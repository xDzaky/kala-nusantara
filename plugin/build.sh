#!/bin/bash
# Build script KalaCore
export PATH="$HOME/tools/gradle-8.12/bin:$PATH"
JAVA_HOME=/usr/lib/jvm/java-21-openjdk
cd "$(dirname "$0")/kala-core"
JAVA_HOME=/usr/lib/jvm/java-21-openjdk gradle shadowJar --no-daemon "$@"
if [ $? -eq 0 ]; then
    echo "✅ Build sukses! Copying to server/plugins..."
    cp build/libs/kala-core-0.1.0-SNAPSHOT.jar ../server/plugins/KalaCore-0.1.0.jar
    echo "📦 KalaCore-0.1.0.jar siap di server/plugins/"
else
    echo "❌ Build gagal!"
fi
