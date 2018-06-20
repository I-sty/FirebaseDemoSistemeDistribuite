package com.szollosi.firebasedemo;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.szollosi.firebasedemo.view.SectionSendFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SendMessageFromReceiver extends AsyncTask<String, Void, String> {
  private static final String FCM_MESSAGE_URL = "https://fcm.googleapis.com/fcm/send";

  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private static final String TAG = SendMessageFromReceiver.class.getName();

  private OkHttpClient mClient = new OkHttpClient();

  /**
   * Override this method to perform a computation on a background thread. The
   * specified parameters are the parameters passed to {@link #execute}
   * by the caller of this task.
   * <p>
   * This method can call {@link #publishProgress} to publish updates
   * on the UI thread.
   *
   * @param strings The parameters of the task.
   *
   * @return A result, defined by the subclass of this task.
   *
   * @see #onPreExecute()
   * @see #onPostExecute
   * @see #publishProgress
   */
  @Override
  protected String doInBackground(String... strings) {
    String to = strings[0];
    String messageId = strings[1];
    try {
      JSONObject root = new JSONObject();

      JSONObject data = new JSONObject();
      data.put(FirebaseCommunicationService.KEY_MESSAGE_ID, messageId);

      root.put("data", data);
      root.put("to", to);

      String result = postToFCM(root.toString());
      Log.d(TAG, "Result: " + result);
      return result;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

  @Override
  protected void onPostExecute(String result) {
    try {
      JSONObject resultJson = new JSONObject(result);
      int success, failure;
      success = resultJson.getInt("success");
      failure = resultJson.getInt("failure");
      Log.e(TAG, "[onPostExecute] Message Success: " + success + "\nMessage Failed: " + failure);
    } catch (JSONException e) {
      e.printStackTrace();
      Log.e(TAG, "[onPostExecute] Message Failed");
    }
  }

  @Nullable
  private String postToFCM(String bodyString) throws IOException {
    RequestBody body = RequestBody.create(JSON, bodyString);
    Request request = new Request.Builder().url(FCM_MESSAGE_URL).post(body)
        .addHeader("Authorization", "key=" + SectionSendFragment.API_KEY).build();
    Response response = mClient.newCall(request).execute();
    ResponseBody responseBody = response.body();
    if (responseBody != null) {
      return responseBody.string();
    }
    return null;
  }
}
