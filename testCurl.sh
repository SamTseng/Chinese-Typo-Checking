#!/bin/sh
sent="以目前鼓本估算"
uid="l2"
key="ntnu"
curl -L --header "Content-Type: application/json" --request POST \
     --data "{\"uid\":\"${uid}\",\"key\":\"${key}\", \"sent\":\"${sent}\"}" http://localhost:5050/query
