#!/bin/bash

git pull origin main
mvn clean
mvn install -DskipTests=true
