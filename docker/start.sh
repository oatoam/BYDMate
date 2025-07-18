#!/bin/bash

set -ex

APP=bydmate

docker compose --env-file ${APP}.env -f docker-compose.yml up -d $@