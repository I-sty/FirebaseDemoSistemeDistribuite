package com.szollosi.firebasedemo;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.szollosi.firebasedemo.data.ReceiverItem;

import java.util.Calendar;
import java.util.Date;

public class FirebaseCommunicationService extends FirebaseMessagingService {

  public static final String KEY_MESSAGE_ID = "key_message_id";

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
    //Log.d(TAG, "From: " + remoteMessage.getFrom());

    // Check if message contains a notification payload.
    RemoteMessage.Notification notification = remoteMessage.getNotification();
    if (notification != null) {
      //Log.d(TAG, "Message Notification Body: " + notification.getBody());
      String messageId = remoteMessage.getMessageId();
      replyToMessage(remoteMessage.getFrom(), messageId);
      Log.i(TAG, "[onMessageReceived]\ngetSentTime(): " + remoteMessage.getSentTime() + "\nKEY_DATE:      " +
          remoteMessage.getData().get(PlaceholderFragment.KEY_EXTRA_DATE) + "\ncurrent:       " +
          Calendar.getInstance().getTimeInMillis());
      handleMessage(messageId,
          new Date(Long.parseLong(remoteMessage.getData().get(PlaceholderFragment.KEY_EXTRA_DATE))), notification
              .getBody());
    } else {
      replyReceivedFromTheReceiver();
    }

    // Also if you intend on generating your own notifications as a result of a received FCM
    // message, here is where that should be initiated. See sendNotification method below.
  }

  private void replyReceivedFromTheReceiver() {
    Log.i(TAG, "[replyReceivedFromTheReceiver]");
  }

  private void replyToMessage(String to, String messageId) {
    Log.i(TAG, "[replyToMessage] Message ID: " + messageId);
    new SendMessageFromReceiver().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, to, messageId);
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
