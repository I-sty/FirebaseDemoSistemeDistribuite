package com.szollosi.firebasedemo.xmpp.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Util class for constants and generic methods
 */

public class Util {

  // For the FCM connection
  public static final String FCM_SERVER = "gcm.l.google.com"; // prod

  public static final int FCM_PORT = 5235; // prod

  public static final String FCM_ELEMENT_NAME = "gcm";

  public static final String FCM_NAMESPACE = "google:mobile:data";

  public static final String FCM_SERVER_AUTH_CONNECTION = "gcm.googleapis.com";

  // For the backend action attribute values
  public static final String BACKEND_ACTION_ECHO = "ECHO";

  public static final String BACKEND_ACTION_MESSAGE = "MESSAGE";

  // For the app common payload message attributes (android - xmpp server)
  public static final String PAYLOAD_ATTRIBUTE_MESSAGE = "message";

  public static final String PAYLOAD_ATTRIBUTE_ACTION = "action";

  public static final String PAYLOAD_ATTRIBUTE_RECIPIENT = "recipient";

  /**
   * Returns a random message id to uniquely identify a message
   */
  public static String getUniqueMessageId() {
    // TODO: replace with your own random message ID
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
    final String formatted = simpleDateFormat.format(new Date());
    final UUID randomUUID = UUID.randomUUID();
    return "m-" + formatted + "-" + randomUUID.toString();
  }

  public static Long getCurrentUnixTime() {
    return getCurrentTimeMillis() / 1000L;
  }

  public static Long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

}
