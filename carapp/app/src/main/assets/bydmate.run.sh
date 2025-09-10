echo $0 $@

#ps -ef | grep $0 | grep -v grep | awk '{print $2}' | xargs kill -9 | true

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

# Set JNI library path (include both system and app lib paths)
JNI_LIB_PATH="/system/lib64"
if [ -d "/data/app-lib/com.toddmo.bydmate.client" ]; then
    JNI_LIB_PATH="$JNI_LIB_PATH:/data/app-lib/com.toddmo.bydmate.client"
fi

APP_LIB_PATH=$(dirname $(pm path com.toddmo.bydmate.client | awk -F':' '{print $NF}'))/lib/arm64/
if [ -d "${APP_LIB_PATH}" ]; then
    JNI_LIB_PATH="$JNI_LIB_PATH:${APP_LIB_PATH}"
fi

# Launch the Server class using app_process
echo exec app_process  -Djava.class.path="$DEX_PATH" -Djava.library.path="$JNI_LIB_PATH" /system/bin \
    $CLASS_NAME $@

exec app_process  -Djava.class.path="$DEX_PATH" -Djava.library.path="$JNI_LIB_PATH" /system/bin \
    $CLASS_NAME $@