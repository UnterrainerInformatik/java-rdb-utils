#!/usr/bin/env bash

mkdir -p /app/data/elite-server/mysql-data

docker-compose pull
docker-compose up -d --force-recreate --remove-orphans &
