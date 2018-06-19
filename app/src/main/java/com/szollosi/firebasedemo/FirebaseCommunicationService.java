package com.szollosi.firebasedemo;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.szollosi.firebasedemo.data.ReceiverItem;

import java.util.Date;

public class FirebaseCommunicationService extends FirebaseMessagingService {
  private static final String TAG = FirebaseCommunicationService.class.getName();

  @Override
  public void onDeletedMessages() {
    Log.i(TAG, "[onDeletedMessages]");
  }

  private void handleMessage(String id, Date time, String message) {
    ReceiverItem receiverItem = new ReceiverItem(id, time, message);
    Intent intent = new Intent(PlaceholderFragment.ACTION_MESSAGE_RECEIVED);
    intent.putExtra(PlaceholderFragment.KEY_EXTRA_MESSAGE, receiverItem);
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {
    Log.e(TAG, "[onMessageReceived] " + remoteMessage);

    // TODO(developer): Handle FCM messages here.
    // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
    Log.d(TAG, "From: " + remoteMessage.getFrom());


    // Check if message contains a notification payload.
    RemoteMessage.Notification notification = remoteMessage.getNotification();
    if (notification != null) {
      Log.d(TAG, "Message Notification Body: " + notification.getBody());
      handleMessage(remoteMessage.getMessageId(), new Date(remoteMessage.getSentTime()),
          notification.getBody());
    }

    // Also if you intend on generating your own notifications as a result of a received FCM
    // message, here is where that should be initiated. See sendNotification method below.
  }

  @Override
  public void onMessageSent(String s) {
    super.onMessageSent(s);
    Log.e(TAG, "onMessageSent: message sent");
  }

  @Override
  public void onSendError(String s, Exception e) {
    super.onSendError(s, e);
    Log.e(TAG, "onSendError: error");
  }
}
