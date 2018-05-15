#!/usr/bin/env bash

api_key=""

curl -X POST -H "Authorization: key=$api_key" -H "Content-Type: application/json" -d '{
  "notification": {
    "title": "FCM Message",
    "body": "This is an FCM Message",
  },
  "to" : ""
}' https://fcm.googleapis.com/fcm/send