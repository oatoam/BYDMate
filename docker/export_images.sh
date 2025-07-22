#!/bin/bash

set -e

APP=bydmate

rm -rf ${APP}-images.tar.{gz,xz,zst}
for arg in $@; do
    if [ "$arg" == "fast" ]; then
        FAST_EXPORT=true
    fi
done

IMAGELIST=$(docker compose config --images)

if [ -n "$FAST_EXPORT" ]; then
    echo "Fast export mode enabled"
    IMAGELIST="${APP}-data_analyzer ${APP}-frontend ${APP}-mqtt_subscriber"
fi

docker save $IMAGELIST | pv | zstd -T0 > ${APP}-images.tar.zst

# docker run --rm -v ${APP}_data:/data -v $(pwd):/backup \
#        alpine tar czf /backup/${APP}-data.tar.gz -C /data .