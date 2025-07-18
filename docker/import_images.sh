#!/bin/bash

set -e

APP=bydmate

if [[ ! "$1" =~ ^[a-zA-Z0-9._%-]+@([a-zA-Z0-9.-]+)$ ]]; then
    echo "Error: First argument must be a SSH remote address (e.g., user@host)"
    exit 1
fi


ssh $1 "mkdir -p ~/docker/${APP}"

rsync -av --progress \
    ${APP}-images.tar.zst \
    ${APP}.env docker-compose.yml \
    setup_volumes.sh \
    configuration \
    $1:~/docker/${APP}

# rsync -av --progress ${APP}.env ${APP}-data.tar.gz $1

# 3. 远程导入并启动
# ssh $1 <<EOF
#   set -e
#   cd ~/${APP}
#   gunzip -c ${APP}-images.tar.gz | docker load
#   docker volume create ${APP}_data
#   docker run --rm -v ${APP}_data:/data -v \$(pwd):/backup \
#          alpine sh -c "cd /data && tar xzf /backup/${APP}-data.tar.gz"
#   docker compose --env-file ${APP}.env -f docker-compose.yaml up -d
# EOF

ssh $1 <<EOF
  set -ex
  cd ~/docker/${APP}
  pv ${APP}-images.tar.zst | zstd -d -T0 | docker load
  docker compose --env-file ${APP}.env -f docker-compose.yml up -d --no-build
EOF