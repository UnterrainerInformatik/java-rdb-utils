#!/usr/bin/env bash
. ./.gitlab-env

mkdir -p /app/data/rdbutils/mysql-data

docker-compose pull
docker-compose up -d --force-recreate --remove-orphans &
