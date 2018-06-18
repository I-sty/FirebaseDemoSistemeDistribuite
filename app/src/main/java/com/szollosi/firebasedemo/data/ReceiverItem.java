package com.szollosi.firebasedemo.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiverItem implements Parcelable {

  public static final Parcelable.Creator<ReceiverItem> CREATOR =
      new Parcelable.Creator<ReceiverItem>() {

        @Override public ReceiverItem createFromParcel(Parcel source) {
          return new ReceiverItem(source);
        }

        @Override public ReceiverItem[] newArray(int size) {
          return new ReceiverItem[size];
        }
      };

  private final SimpleDateFormat simpleDateFormat =
      new SimpleDateFormat("hh:mm:ss:SSS dd/M/yyyy", Locale.getDefault());

  private final Date sent;

  private final Date received;

  private final Date difference;

  private final String message;

  private String id;

  public ReceiverItem(String id, Date sent, String message) {
    this.id = id;
    this.sent = sent;
    this.received = new Date();
    this.difference = new Date(received.getTime() - sent.getTime());
    this.message = message;
  }

  private ReceiverItem(Parcel in) {
    long tmpSent = in.readLong();
    this.sent = tmpSent == -1 ? null : new Date(tmpSent);
    long tmpReceived = in.readLong();
    this.received = tmpReceived == -1 ? null : new Date(tmpReceived);
    long tmpDifference = in.readLong();
    this.difference = tmpDifference == -1 ? null : new Date(tmpDifference);
    this.message = in.readString();
    this.id = in.readString();
  }

  public String getSent() {
    return simpleDateFormat.format(this.sent);
  }

  public String getReceived() {
    return simpleDateFormat.format(this.received);
  }

  public String getDifference() {
    return String.valueOf(difference.getTime());
  }

  public String getMessage() {
    return message;
  }

  public String getId() {
    return id;
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeSerializable(this.simpleDateFormat);
    dest.writeLong(this.sent != null ? this.sent.getTime() : -1);
    dest.writeLong(this.received != null ? this.received.getTime() : -1);
    dest.writeLong(this.difference != null ? this.difference.getTime() : -1);
    dest.writeString(this.message);
    dest.writeString(this.id);
  }
}
