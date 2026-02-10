#!/bin/bash

echo -e "\nResetting dev environment"
echo -e "-------------------------\n"

HOST=$1
PORT=$2

# Reset dev database
echo "Resetting database..."
mysql -h "$HOST" -P "$PORT" -uroot -p < dev_reset.sql

echo "Removing generated data..."
rm /projectdata/mp/dev/persistent/config/* -rf
rm /projectdata/mp/dev/persistent/data/thumbs/* -rf

echo -e "Done.\n"
