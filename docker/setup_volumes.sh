#!/bin/bash

APP="bydmate"

if test ! -e $PWD/data/influx; then
    echo "create volume influx_data"
    mkdir -p $PWD/data/influx
fi

# docker volume create --driver local \
#     --opt type=none \
#     --opt o=bind \
#     --opt device=$PWD/data/influx \
#     ${APP}_influx_data

if test ! -e $PWD/data/postgres; then
    echo "create volume postgres_data"
    mkdir -p $PWD/data/postgres
fi

# docker volume create --driver local \
#     --opt type=none \
#     --opt o=bind \
#     --opt device=$PWD/data/postgres \
#     ${APP}_postgres_data

if test ! -e $PWD/configuration; then
    echo "create volume configuration"
    mdkir -p $PWD/configuration
fi

# docker volume create --driver local \
#     --opt type=none \
#     --opt o=bind \
#     --opt device=$PWD/configuration \
#     ${APP}_configuration

