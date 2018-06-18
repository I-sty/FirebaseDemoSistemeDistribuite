package com.szollosi.firebasedemo.xmpp.server;

import android.text.TextUtils;
import android.util.Log;

import com.szollosi.firebasedemo.xmpp.bean.CcsInMessage;
import com.szollosi.firebasedemo.xmpp.bean.CcsOutMessage;
import com.szollosi.firebasedemo.xmpp.bean.Message;
import com.szollosi.firebasedemo.xmpp.util.BackOffStrategy;
import com.szollosi.firebasedemo.xmpp.util.MessageMapper;
import com.szollosi.firebasedemo.xmpp.util.Util;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.sm.predicates.ForEveryStanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

/**
 * Class that connects to FCM Cloud Connection Server and handles stanzas (ACK, NACK, upstream,
 * downstream). Sample Smack implementation of a client for FCM Cloud Connection Server. Most of it
 * has been taken more or less verbatim from Google's documentation: <a href=
 * "https://firebase.google.com/docs/cloud-messaging/xmpp-server-ref">https://firebase.google.com/docs/cloud-messaging/xmpp-server-ref</a>
 *
 * @author Charz++
 */
public class CcsClient
    implements StanzaListener, ReconnectionListener, ConnectionListener, PingFailedListener {

  private static final String TAG = CcsClient.class.getName();

  // downstream messages to sync with acks and nacks
  private final Map<String, Message> syncMessages = new ConcurrentHashMap<>();

  // messages from backoff failures
  private final Map<String, Message> pendingMessages = new ConcurrentHashMap<>();

  private XMPPTCPConnection xmppConn;

  private String apiKey;

  private boolean debuggable;

  private String username;

  private Boolean isConnectionDraining = false;

  /**
   * Public constructor for the CCS Client
   *
   * @param projectId
   * @param apiKey
   * @param debuggable
   */

  public CcsClient(String projectId, String apiKey, boolean debuggable) {
    // Add FCM Packet Extension Provider
    ProviderManager.addExtensionProvider(Util.FCM_ELEMENT_NAME, Util.FCM_NAMESPACE,
        new ExtensionElementProvider<FcmPacketExtension>() {

          @Override
          public FcmPacketExtension parse(XmlPullParser parser, int initialDepth)
              throws XmlPullParserException, IOException {
            final String json = parser.nextText();
            return new FcmPacketExtension(json);
          }
        });
    this.apiKey = apiKey;
    this.debuggable = debuggable;
    this.username = projectId + "@" + Util.FCM_SERVER_AUTH_CONNECTION;
  }

  /**
   * Connects to FCM Cloud Connection Server using the supplied credentials
   */
  public void connect() throws XMPPException, SmackException, IOException, InterruptedException,
      NoSuchAlgorithmException, KeyManagementException {
    Log.i(TAG, "Initiating connection ...");

    isConnectionDraining = false; // Set connection draining to false when there is a new connection

    // create connection configuration
    XMPPTCPConnection.setUseStreamManagementResumptionDefault(true);
    XMPPTCPConnection.setUseStreamManagementDefault(true);

    final SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, null, new SecureRandom());

    final XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
    Log.i(TAG, "Connecting to the server ...");
    config.setXmppDomain(Util.FCM_SERVER_AUTH_CONNECTION);
    config.setHost(Util.FCM_SERVER);
    config.setPort(Util.FCM_PORT);
    config.setSendPresence(false);
    config.setSecurityMode(SecurityMode.ifpossible);
    config.setDebuggerEnabled(
        debuggable); // launch a window with info about packets sent and received
    config.setCompressionEnabled(true);
    config.setSocketFactory(sslContext.getSocketFactory());
    config.setCustomSSLContext(sslContext);

    xmppConn = new XMPPTCPConnection(config.build()); // Create the connection

    xmppConn.connect(); // Connect

    // Enable automatic reconnection and add the listener (if not, remove the the listener, the
    // interface and the override methods)
    ReconnectionManager.getInstanceFor(xmppConn).enableAutomaticReconnection();
    ReconnectionManager.getInstanceFor(xmppConn).addReconnectionListener(this);

    // Disable Roster at login (in XMPP the contact list is called a "roster")
    Roster.getInstanceFor(xmppConn).setRosterLoadedAtLogin(false);

    // Security checks
    SASLAuthentication.unBlacklistSASLMechanism(
        "PLAIN"); // FCM CCS requires a SASL PLAIN authentication mechanism
    SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5");
    Log.i(TAG, "SASL PLAIN authentication enabled ? " +
        SASLAuthentication.isSaslMechanismRegistered("PLAIN"));
    Log.i(TAG, "Is compression enabled ? " + xmppConn.isUsingCompression());
    Log.i(TAG, "Is the connection secure ? " + xmppConn.isSecureConnection());

    // Handle connection errors
    xmppConn.addConnectionListener(this);

    // Handle incoming packets and reject messages that are not from FCM CCS
    xmppConn.addAsyncStanzaListener(this,
        stanza -> stanza.hasExtension(Util.FCM_ELEMENT_NAME, Util.FCM_NAMESPACE));

    // Log all outgoing packets
    xmppConn.addStanzaInterceptor(stanza -> Log.i(TAG, "Sent: " + stanza.toXML()),
        ForEveryStanza.INSTANCE);

    // Set the ping interval
    final PingManager pingManager = PingManager.getInstanceFor(xmppConn);
    pingManager.setPingInterval(100);
    pingManager.registerPingFailedListener(this);

    xmppConn.login(username, apiKey);
    Log.i(TAG, "User logged in: {}" + username);
  }

  /**
   * Sends all the queued pending messages
   */
  private void sendQueuedPendingMessages(Map<String, Message> pendingMessagesToResend) {
    Log.i(TAG, "Sending queued pending messages through the new connection.");
    Log.i(TAG, "Pending messages size: " + pendingMessages.size());
    final Map<String, Message> filtered = pendingMessagesToResend.entrySet().stream()
        .filter(entry -> entry.getValue() != null).sorted(compareTimestampsAscending())
        .collect(Collectors
            .toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    Log.i(TAG, "Filtered pending messages size: " + filtered.size());
    filtered.forEach((messageId, pendingMessage) -> {
      pendingMessages.remove(messageId);
      sendDownstreamMessage(messageId, pendingMessage.getJsonRequest());
    });
  }

  /**
   * Sends all the queued sync messages that occurred before 5 seconds (1000 ms) ago. With this we try
   * to send those lost messages that we have not received ack nor nack.
   */
  private void sendQueuedSyncMessages(Map<String, Message> syncMessagesToResend) {
    Log.i(TAG, "Sending queued sync messages ...");
    Log.i(TAG, "Sync messages size: " + syncMessages.size());
    final Map<String, Message> filtered =
        syncMessagesToResend.entrySet().stream().filter(isOldSyncMessageQueued())
            .sorted(compareTimestampsAscending())
            .collect(Collectors
                .toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    Log.i(TAG, "Filtered sync messages size: " + filtered.size());
    filtered.forEach((messageId, syncMessage) -> {
      removeMessageFromSyncMessages(messageId);
      sendDownstreamMessage(messageId, syncMessage.getJsonRequest());
    });
  }

  private Predicate<Entry<String, Message>> isOldSyncMessageQueued() {
    return entry -> (entry.getValue() != null)
        && (entry.getValue().getTimestamp() < Util.getCurrentTimeMillis() - 5000);
  }

  private Comparator<Entry<String, Message>> compareTimestampsAscending() {
    return Comparator.comparing(e -> e.getValue().getTimestamp());
  }

  /**
   * Handle incoming messages
   */
  @Override
  public void processStanza(Stanza packet) {
    Log.i(TAG, "Processing packet in thread " + Thread.currentThread().getName() + " - " +
        Thread.currentThread().getId());
    Log.i(TAG, "Received: {}" + packet.toXML());
    final FcmPacketExtension fcmPacket =
        (FcmPacketExtension) packet.getExtension(Util.FCM_NAMESPACE);
    final String json = fcmPacket.getJson();
    Map<String, Object> jsonMap = MessageMapper.toMapFromJsonString(json);
    if (jsonMap == null) {
      Log.i(TAG, "Error parsing Packet JSON to JSON String: {}" + json);
      return;
    }
    final Object messageTypeObj = jsonMap.get("message_type");

    if (messageTypeObj == null) {
      // Normal upstream message from a device client
      CcsInMessage inMessage = MessageMapper.ccsInMessageFrom(jsonMap);
      handleUpstreamMessage(inMessage);
      return;
    }

    final String messageType = messageTypeObj.toString();
    switch (messageType) {
      case "ack":
        handleAckReceipt(jsonMap);
        break;
      case "nack":
        handleNackReceipt(jsonMap);
        break;
      case "receipt":
        // TODO: handle the delivery receipt when a device confirms that it received a particular message.
        break;
      case "control":
        handleControlMessage(jsonMap);
        break;
      default:
        Log.i(TAG, "Received unknown FCM message type: {}" + messageType);
    }

  }

  /**
   * Handles an upstream message from a device client through FCM
   */
  private void handleUpstreamMessage(CcsInMessage inMessage) {
    // The custom 'action' payload attribute defines what the message action is about.
    final String action =
        inMessage.getDataPayload().get(Util.PAYLOAD_ATTRIBUTE_ACTION);
    if (TextUtils.isEmpty(action)) {
      throw new IllegalStateException("Action must not be null! Options: 'ECHO', 'MESSAGE'");
    }

    // 1. send ACK to FCM
    final String ackJsonRequest =
        MessageMapper.createJsonAck(inMessage.getFrom(), inMessage.getMessageId());
    sendAck(ackJsonRequest);

    // 2. process and send message
    if (action.equals(Util.BACKEND_ACTION_ECHO)) { // send a message to the sender (user itself)
      final String messageId = Util.getUniqueMessageId();
      final String to = inMessage.getFrom();

      final CcsOutMessage outMessage = new CcsOutMessage(to, messageId, inMessage.getDataPayload());
      final String jsonRequest = MessageMapper.toJsonString(outMessage);
      sendDownstreamMessage(messageId, jsonRequest);
    } else if (action.equals(Util.BACKEND_ACTION_MESSAGE)) { // send a message to the recipient
      this.handlePacketRecieved(inMessage);
    }
  }

  /**
   * Handles an ACK message from FCM
   */
  private void handleAckReceipt(Map<String, Object> jsonMap) {
    removeMessageFromSyncMessages(jsonMap);
  }

  /**
   * Handles a NACK message from FCM
   */
  private void handleNackReceipt(Map<String, Object> jsonMap) {
    removeMessageFromSyncMessages(jsonMap);

    String errorCode = (String) jsonMap.get("error");
    if (TextUtils.isEmpty(errorCode)) {
      Log.e(TAG, "Received null FCM Error Code.");
      return;
    }
    switch (errorCode) {
      case "INVALID_JSON":
      case "BAD_REGISTRATION":
      case "DEVICE_UNREGISTERED":
      case "BAD_ACK":
      case "TOPICS_MESSAGE_RATE_EXCEEDED":
      case "DEVICE_MESSAGE_RATE_EXCEEDED":
        Log.i(TAG,
            "Device error: {} -> " + jsonMap.get("error") + ", " +
                jsonMap.get("error_description"));
        break;
      case "SERVICE_UNAVAILABLE":
      case "INTERNAL_SERVER_ERROR":
        Log.i(TAG,
            "Server error: " + jsonMap.get("error") + " -> " + jsonMap.get("error_description"));
        break;
      case "CONNECTION_DRAINING":
        Log.i(TAG, "Connection draining from Nack ...");
        handleConnectionDraining();
        break;
      default:
        Log.i(TAG, "Received unknown FCM Error Code: {}" + errorCode);
        break;
    }
  }

  /**
   * Handles a Control message from FCM
   */
  private void handleControlMessage(Map<String, Object> jsonMap) {
    final String controlType = (String) jsonMap.get("control_type");

    if (controlType.equals("CONNECTION_DRAINING")) {
      handleConnectionDraining();
    } else {
      Log.i(TAG, "Received unknown FCM Control message: {}" + controlType);
    }
  }

  private void handleConnectionDraining() {
    Log.i(TAG, "FCM Connection is draining!");
    isConnectionDraining = true;
  }

  private void removeMessageFromSyncMessages(Map<String, Object> jsonMap) {
    final String messageIdObj = (String) jsonMap.get("message_id");
    if (messageIdObj != null) {
      removeMessageFromSyncMessages(messageIdObj);
    }
  }

  private void putMessageToSyncMessages(String messageId, String jsonRequest) {
    syncMessages.put(messageId, Message.from(jsonRequest));
  }

  public void removeMessageFromSyncMessages(String messageId) {
    syncMessages.remove(messageId);
  }

  private void onUserAuthentication() {
    isConnectionDraining = false;
    sendQueuedMessages();
  }

  /**
   * ===============================================================================================
   *
   * API Helper methods:
   *
   * These are methods that implementers can use, call, or override. Help give the implementer more
   * control/ customization.
   *
   * ===============================================================================================
   */

  /**
   * Note: This method is only called if {@link ReconnectionManager#isAutomaticReconnectEnabled()}
   * returns true
   */
  @Override
  public void reconnectionFailed(Exception e) {
    Log.i(TAG, "Reconnection failed! Error: {}" + e.getMessage());
  }

  /**
   * Note: This method is only called if {@link ReconnectionManager#isAutomaticReconnectEnabled()}
   * returns true
   */
  @Override
  public void reconnectingIn(int seconds) {
    Log.i(TAG, "Reconnecting in {} ..." + seconds);
  }

  /**
   * This method will be removed in Smack 4.3. Use {@link #connected(XMPPConnection)} or
   * {@link #authenticated(XMPPConnection, boolean)} instead.
   */
  @Deprecated
  @Override
  public void reconnectionSuccessful() {
    Log.i(TAG, "Reconnection successful.");
  }

  @Override
  public void connectionClosedOnError(Exception e) {
    Log.i(TAG, "Connection closed on error.");
  }

  @Override
  public void connectionClosed() {
    Log.i(TAG,
        "Connection closed. The current connectionDraining flag is: {}" + isConnectionDraining);
    if (isConnectionDraining) {
      reconnect();
    }
  }

  @Override
  public void authenticated(XMPPConnection arg0, boolean arg1) {
    Log.i(TAG, "User authenticated.");
    // This is the last step after a connection or reconnection
    onUserAuthentication();
  }

  @Override
  public void connected(XMPPConnection arg0) {
    Log.i(TAG, "Connection established.");
  }

  @Override
  public void pingFailed() {
    Log.i(TAG, "The ping failed, restarting the ping interval again ...");
    final PingManager pingManager = PingManager.getInstanceFor(xmppConn);
    pingManager.setPingInterval(100);
  }

  /**
   * Called when a custom packet has been received by the server. By default this method just resends
   * the packet.
   */
  public void handlePacketRecieved(CcsInMessage inMessage) {
    final String messageId = Util.getUniqueMessageId();
    // TODO: it should be the user id to be retrieved from the data base
    final String to = inMessage.getDataPayload().get(Util.PAYLOAD_ATTRIBUTE_RECIPIENT);

    // TODO: handle the data payload sent to the client device. Here, I just resend the incoming one.
    final CcsOutMessage outMessage = new CcsOutMessage(to, messageId, inMessage.getDataPayload());
    final String jsonRequest = MessageMapper.toJsonString(outMessage);
    sendDownstreamMessage(messageId, jsonRequest);
  }

  /**
   * Sends a downstream message to FCM
   */
  public void sendDownstreamMessage(String messageId, String jsonRequest) {
    Log.i(TAG, "Sending downstream message.");
    putMessageToSyncMessages(messageId, jsonRequest);
    if (!isConnectionDraining) {
      sendDownstreamMessageInternal(messageId, jsonRequest);
    }
  }

  /**
   * Sends a downstream message to FCM with back off strategy
   */
  private void sendDownstreamMessageInternal(String messageId, String jsonRequest) {
    final Stanza request = new FcmPacketExtension(jsonRequest).toPacket();
    final BackOffStrategy backoff = new BackOffStrategy();
    while (backoff.shouldRetry()) {
      try {
        xmppConn.sendStanza(request);
        backoff.doNotRetry();
      } catch (NotConnectedException | InterruptedException e) {
        Log.e(TAG,
            "The packet could not be sent due to a connection problem. Backing off the packet: " +
                request.toXML());
        try {
          backoff.errorOccured2();
        } catch (Exception e2) { // all the attempts failed
          removeMessageFromSyncMessages(messageId);
          pendingMessages.put(messageId, Message.from(jsonRequest));
        }
      }
    }
  }

  /**
   * Sends an ACK to FCM with back off strategy
   *
   * @param jsonRequest
   */
  public void sendAck(String jsonRequest) {
    Log.i(TAG, "Sending ack.");
    final Stanza packet = new FcmPacketExtension(jsonRequest).toPacket();
    final BackOffStrategy backoff = new BackOffStrategy();
    while (backoff.shouldRetry()) {
      try {
        xmppConn.sendStanza(packet);
        backoff.doNotRetry();
      } catch (NotConnectedException | InterruptedException e) {
        Log.i(TAG,
            "The packet could not be sent due to a connection problem. Backing off the packet: " +
                packet.toXML());
        backoff.errorOccured();
      }
    }
  }

  /**
   * Sends a message to multiple recipients (list). Kind of like the old HTTP message with the list of
   * regIds in the "registration_ids" field.
   */
  public void sendBroadcast(CcsOutMessage outMessage, List<String> recipients) {
    final Map<String, Object> map = MessageMapper.mapFrom(outMessage);
    for (String toRegId : recipients) {
      final String messageId = Util.getUniqueMessageId();
      map.put("message_id", messageId);
      map.put("to", toRegId);
      final String jsonRequest = MessageMapper.toJsonString(map);
      sendDownstreamMessage(messageId, jsonRequest);
    }
  }

  public synchronized void reconnect() {
    Log.i(TAG, "Initiating reconnection ...");
    final BackOffStrategy backoff = new BackOffStrategy(5, 1000);
    while (backoff.shouldRetry()) {
      try {
        connect();
        sendQueuedMessages();
        backoff.doNotRetry();
      } catch (XMPPException | SmackException | IOException | InterruptedException | KeyManagementException
          | NoSuchAlgorithmException e) {
        Log.i(TAG,
            "The notifier server could not reconnect after the connection draining message.");
        backoff.errorOccured();
      }
    }
  }

  private void sendQueuedMessages() {
    // copy the snapshots of the two lists and then try to resend them
    final Map<String, Message> pendingMessagesToResend = new HashMap<>(pendingMessages);
    final Map<String, Message> syncMessagesToResend = new HashMap<>(syncMessages);
    sendQueuedPendingMessages(pendingMessagesToResend);
    sendQueuedSyncMessages(syncMessagesToResend);
  }

  /*** BEGIN: Methods for the Manager ***/

  private boolean isConnected() {
    return xmppConn != null && xmppConn.isConnected();
  }

  private boolean isAuthenticated() {
    return xmppConn != null && xmppConn.isAuthenticated();
  }

  public boolean isAlive() {
    Log.i(TAG, "Connection parameters -> isConnected: " + isConnected() + ", isAuthenticated: " +
        isAuthenticated());
    return isConnected() && isAuthenticated();
  }

  public void disconnectAll() {
    Log.i(TAG, "Disconnecting all ...");
    if (xmppConn.isConnected()) {
      Log.i(TAG, "Detaching all the listeners for the connection.");
      PingManager.getInstanceFor(xmppConn).unregisterPingFailedListener(this);
      ReconnectionManager.getInstanceFor(xmppConn).removeReconnectionListener(this);
      xmppConn.removeAsyncStanzaListener(this);
      xmppConn.removeConnectionListener(this);
      xmppConn.removeStanzaInterceptor(this);
      xmppConn.removeAllRequestAckPredicates();
      xmppConn.removeAllStanzaAcknowledgedListeners();
      xmppConn.removeAllStanzaIdAcknowledgedListeners();
      xmppConn.removeStanzaSendingListener(this);
      xmppConn.removeStanzaAcknowledgedListener(this);
      xmppConn.removeAllRequestAckPredicates();
      Log.i(TAG, "Disconnecting the xmpp server from FCM.");
      xmppConn.disconnect();
    }
  }

  public void disconnectGracefully() {
    Log.i(TAG, "Disconnecting ...");
    if (xmppConn.isConnected()) {
      Log.i(TAG, "Disconnecting the xmpp server from FCM");
      xmppConn
          .disconnect(); // this method call the onClosed listener because it have not been detached
    }
  }

  /*** END: Methods for the Manager ***/

}
