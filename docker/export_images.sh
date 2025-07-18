#!/bin/bash

set -e

APP=bydmate

rm -rf ${APP}-images.tar.{gz,xz,zst}

docker save $(docker compose config --images) | pv | zstd -T0 > ${APP}-images.tar.zst

# docker run --rm -v ${APP}_data:/data -v $(pwd):/backup \
#        alpine tar czf /backup/${APP}-data.tar.gz -C /data .