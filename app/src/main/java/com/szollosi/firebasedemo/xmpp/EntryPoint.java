package com.szollosi.firebasedemo.xmpp;

import android.util.Log;

import com.szollosi.firebasedemo.xmpp.bean.CcsOutMessage;
import com.szollosi.firebasedemo.xmpp.server.CcsClient;
import com.szollosi.firebasedemo.xmpp.util.MessageMapper;
import com.szollosi.firebasedemo.xmpp.util.Util;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Entry Point class for the XMPP Server
 */
public class EntryPoint extends CcsClient {

  private static final String TAG = EntryPoint.class.getName();

  public EntryPoint(String projectId, String apiKey, boolean debuggable, String toRegId) {
    super(projectId, apiKey, debuggable);

    try {
      connect();
    } catch (XMPPException | InterruptedException | KeyManagementException | NoSuchAlgorithmException | SmackException
        | IOException e) {
      Log.e(TAG, "[EntryPoint] Error trying to connect. Error: " + e.getMessage());
      return;
    }

    // Send a sample downstream message to a device
    final String messageId = Util.getUniqueMessageId();
    final Map<String, String> dataPayload = new HashMap<>();
    dataPayload.put(Util.PAYLOAD_ATTRIBUTE_MESSAGE, "This is the simple sample message");
    final CcsOutMessage message = new CcsOutMessage(toRegId, messageId, dataPayload);
    final String jsonRequest = MessageMapper.toJsonString(message);
    sendDownstreamMessage(messageId, jsonRequest);

    try {
      final CountDownLatch latch = new CountDownLatch(1);
      latch.await();
    } catch (InterruptedException e) {
      Log.e(TAG,
          "[EntryPoint] An error occurred while latch was waiting. Error: " + e.getMessage());
    }
  }
}
