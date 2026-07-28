#!/usr/bin/env sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
server_dir="$project_dir/run-26.1"
java_command="java"

if [ "$(uname -s)" = "Darwin" ] && [ -x /usr/libexec/java_home ]; then
    java_home=$(/usr/libexec/java_home -v 25)
    java_command="$java_home/bin/java"
fi

if [ ! -f "$server_dir/paper.jar" ]; then
    echo "Paper 26.1.2 fehlt: $server_dir/paper.jar"
    echo "Führe zuerst die 26.1-Testserver-Einrichtung aus."
    exit 1
fi

cd "$server_dir"
exec "$java_command" -Xms2G -Xmx2G -jar paper.jar --nogui
