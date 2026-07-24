#!/bin/sh
# Gradle wrapper
if [ -f /usr/lib/jvm/java-17-openjdk-amd64/bin/java ]; then
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
elif [ -f /usr/lib/jvm/java-11-openjdk-amd64/bin/java ]; then
    JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
fi
export JAVA_HOME
if [ ! -f /usr/local/bin/gradle ]; then
    if command -v java >/dev/null 2>&1; then
        if [ ! -f gradle-wrapper.jar ]; then
            echo "Gradle wrapper not found. Install Gradle or download wrapper."
            exit 1
        fi
        java -jar gradle-wrapper.jar "$@"
    else
        echo "Java not found"
        exit 1
    fi
else
    gradle "$@"
fi
