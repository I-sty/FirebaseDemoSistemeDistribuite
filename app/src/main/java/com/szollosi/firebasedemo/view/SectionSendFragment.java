package com.szollosi.firebasedemo.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.szollosi.firebasedemo.R;
import com.szollosi.firebasedemo.xmpp.util.Util;

import java.util.Random;

import static com.szollosi.firebasedemo.xmpp.util.Util.BACKEND_ACTION_ECHO;

public class SectionSendFragment extends Fragment {

  private static final String API_KEY =
      "AAAAJsr7MLQ:APA91bH_HourueyFn1Tuxzj4p3FEt_2eN7rsRm5VczNkfvQP-aeede8LqXSzC-0ibWlZzkQ1Q5ijuzLcRpY9rRqnBo_" +
          "-IezhF7aJhvu-7we0d9MP_QdRXXSYUfaqhfW9YjK4s8OxKoMn";

  private static final String PROJECT_ID = "fir-demo-e59f8";

  private static final String SENDER_ID = "166614216884";

  private static final String TAG = SectionSendFragment.class.getName();

  private FloatingActionButton actionButton;

  private TextInputEditText apiKeyEditText;

  private ImageView copyIcon;

  private TextInputEditText messageEditText;

  private TextView myTokenTextView;

  private View.OnClickListener onClickCopyIcon = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
      ClipData clip = ClipData.newPlainText(myTokenTextView.getText(), myTokenTextView.getText());
      if (clipboard != null) {
        clipboard.setPrimaryClip(clip);
      } else {
        Log.w(TAG, "[onClickCopyIcon] ClipboardManager is null!");
      }
    }
  };

  private AppCompatSpinner periodSpinner;

  private TextInputEditText projectIDEditText;

  private TextInputEditText receiverTokenEditText;

  private Switch repeatTaskSwitch;

  private RelativeLayout root;

  private View.OnClickListener actionButtonClick = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      String apiKey = apiKeyEditText.getText().toString();
      if (TextUtils.isEmpty(apiKey)) {
        showError(apiKeyEditText);
        return;
      }
      String projectIDText = projectIDEditText.getText().toString();
      if (TextUtils.isEmpty(projectIDText)) {
        showError(projectIDEditText);
        return;
      }
      String receiverToken = receiverTokenEditText.getText().toString();
      if (TextUtils.isEmpty(receiverToken)) {
        showError(receiverTokenEditText);
        return;
      }
      //new EntryPoint(projectIDText, apiKey, true, receiverToken);
      try {
        FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
        firebaseMessaging.send(new RemoteMessage.Builder(SENDER_ID + "@" + Util.FCM_SERVER_AUTH_CONNECTION)
            .setMessageId(Integer.toString(new Random().nextInt(9999))).addData("message", "message-isti")
            .addData("action", BACKEND_ACTION_ECHO).setTtl(0).build());
      } catch (Exception e) {
        Log.e(TAG, "[onClick] " + e);
      }
    }
  };

  private AppCompatSpinner valueSpinner;

  private CompoundButton.OnCheckedChangeListener repeatTaskSwitchChangeListener =
      new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          periodSpinner.setEnabled(isChecked);
          valueSpinner.setEnabled(isChecked);
        }
      };

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    root = (RelativeLayout) inflater.inflate(R.layout.fragment_sender, container, false);
    loadViews(root);
    return root;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    actionButtonClick = null;
    repeatTaskSwitchChangeListener = null;
    onClickCopyIcon = null;
  }

  private void loadViews(RelativeLayout root) {
    periodSpinner = root.findViewById(R.id.appCompatSpinner);
    periodSpinner.setEnabled(false);
    valueSpinner = root.findViewById(R.id.appCompatSpinner1);
    valueSpinner.setEnabled(false);
    myTokenTextView = root.findViewById(R.id.textView2);
    String token = FirebaseInstanceId.getInstance().getToken();
    Log.e(TAG, "[loadViews] Firebase token: " + token);
    myTokenTextView.setText(token);
    apiKeyEditText = root.findViewById(R.id.apiKeyEditText);
    apiKeyEditText.setText(API_KEY);
    projectIDEditText = root.findViewById(R.id.projectIdEditText);
    projectIDEditText.setText(PROJECT_ID);
    receiverTokenEditText = root.findViewById(R.id.receiverTokenEditText);
    receiverTokenEditText.setText(
        "f9VNXN49QZA:APA91bGTyBI6HL0t0K2Kgi8vv1HAPu0CayccYK5tpGrt31WoCottWRTx3PzToIQ-96BdLP-OKrbcrUHz992BMQfyq7eo" +
            "-DBJ85Fdk6C8wu-hfJDMgEcT7Ji5AEHXNRNaaLt8juB3I6v96DHEUuNdqp9-gL9_csABWA");
    messageEditText = root.findViewById(R.id.messageEditText);
    actionButton = root.findViewById(R.id.floatingActionButton);
    actionButton.setOnClickListener(actionButtonClick);
    copyIcon = root.findViewById(R.id.imageView);
    copyIcon.setOnClickListener(onClickCopyIcon);
    repeatTaskSwitch = root.findViewById(R.id.switch1);
    repeatTaskSwitch.setOnCheckedChangeListener(repeatTaskSwitchChangeListener);
  }

  private void showError(TextInputEditText editText) {
    editText.setError(editText.getContext().getString(R.string.error_can_not_empty));
    Snackbar.make(root, editText.getContext().getString(R.string.warning_fill_fields), Snackbar.LENGTH_SHORT).show();
  }
}
