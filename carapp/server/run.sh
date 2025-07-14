#!/bin/sh

echo $0 $@

DEX_PATH=$1
echo DEX_PATH = $DEX_PATH
CLASS_NAME=$2
echo CLASS_NAME = $CLASS_NAME
shift
shift

if [ -z "$DEX_PATH" ]; then
    echo "DEX_PATH is not set."
    exit 1
fi

if [ -z "$CLASS_NAME" ]; then
    echo "CLASS_NAME is not set."
    exit 1
fi

# Set the path to your APK's dex file

# Set CLASSPATH to include our DEX
export CLASSPATH="$DEX_PATH"

# Launch the Server class using app_process
exec app_process -Djava.class.path="$DEX_PATH" -Djava.library.path=/system/lib64 /system/bin \
    $CLASS_NAME $@