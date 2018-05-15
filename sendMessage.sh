#!/usr/bin/env bash

api_key="AAAAJsr7MLQ:APA91bFQ46FJL_B1lAeIrb4key4NnFoz9N-rVxS66VwQ74w5pxOcvXB7mLz33dJ9IcLIQzFpBAhgCJ-r2pUK6RIrsokfZJ_pb-fEz1QvlhNP6tv5x_7qRQZ_AHtxVzj7elN3l47-zSXE"

curl -X POST -H "Authorization: key=$api_key" -H "Content-Type: application/json" -d '{
  "notification": {
    "title": "FCM Message",
    "body": "This is an FCM Message",
  },
  "to" : "fOAC8EF2IgY:APA91bGCmq5n13gDoyfFVSvcX6DgdUEt8nbNgoff_5VT3KoveWVURsn1OBmKZU7cyeTLoUzwnL-e1xM-g3q3sHmVkmCAnoi0fyafScdJzeaWG5C0WvsEV0FRpsE7Gb_uJ3kgZiyc2s6D"
}' https://fcm.googleapis.com/fcm/send