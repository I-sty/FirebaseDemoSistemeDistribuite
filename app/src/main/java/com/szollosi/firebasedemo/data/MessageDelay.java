package com.szollosi.firebasedemo.data;

public class MessageDelay {
  public long received;

  private String id;

  private long sent;

  public MessageDelay(long sent, String id) {
    this.sent = sent;
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public long getReceived() {
    return received;
  }

  public long getSent() {
    return sent;
  }

  public void setReceived(long received) {
    this.received = received;
  }
}
