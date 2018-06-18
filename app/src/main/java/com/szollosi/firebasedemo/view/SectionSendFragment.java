package com.szollosi.firebasedemo.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.AsyncTask;
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
import com.szollosi.firebasedemo.R;
import com.szollosi.firebasedemo.xmpp.EntryPoint;

public class SectionSendFragment extends Fragment {

  private static final String TAG = SectionSendFragment.class.getName();

  private static final String PROJECT_ID = "fir-demo-e59f8";

  private static final String API_KEY =
      "AAAAJsr7MLQ:APA91bH_HourueyFn1Tuxzj4p3FEt_2eN7rsRm5VczNkfvQP-aeede8LqXSzC-0ibWlZzkQ1Q5ijuzLcRpY9rRqnBo_-IezhF7aJhvu-7we0d9MP_QdRXXSYUfaqhfW9YjK4s8OxKoMn";

  private FloatingActionButton actionButton;

  private AppCompatSpinner periodSpinner;

  private AppCompatSpinner valueSpinner;

  private TextView myTokenTextView;

  private ImageView copyIcon;

  private RelativeLayout root;

  private Switch repeatTaskSwitch;

  private CompoundButton.OnCheckedChangeListener repeatTaskSwitchChangeListener =
      new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          periodSpinner.setEnabled(isChecked);
          valueSpinner.setEnabled(isChecked);
        }
      };

  private View.OnClickListener onClickCopyIcon = new View.OnClickListener() {

    @Override public void onClick(View v) {
      ClipboardManager clipboard =
          (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
      ClipData clip = ClipData.newPlainText(myTokenTextView.getText(), myTokenTextView.getText());
      if (clipboard != null) {
        clipboard.setPrimaryClip(clip);
      } else {
        Log.w(TAG, "[onClickCopyIcon] ClipboardManager is null!");
      }
    }
  };

  private TextInputEditText projectIDEditText;

  private TextInputEditText apiKeyEditText;

  private TextInputEditText receiverTokenEditText;

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
      AsyncTask.execute(() -> new EntryPoint(projectIDText, apiKey, true, receiverToken));
    }
  };

  private TextInputEditText messageEditText;

  private void showError(TextInputEditText editText) {
    editText.setError(getString(R.string.error_can_not_empty));
    Snackbar.make(root, getString(R.string.warning_fill_fields), Snackbar.LENGTH_SHORT).show();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    actionButtonClick = null;
    repeatTaskSwitchChangeListener = null;
    onClickCopyIcon = null;
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    root = (RelativeLayout) inflater.inflate(R.layout.fragment_sender, container, false);
    loadViews(root);
    return root;
  }

  private void loadViews(RelativeLayout root) {
    periodSpinner = root.findViewById(R.id.appCompatSpinner);
    periodSpinner.setEnabled(false);
    valueSpinner = root.findViewById(R.id.appCompatSpinner1);
    valueSpinner.setEnabled(false);
    myTokenTextView = root.findViewById(R.id.textView2);
    myTokenTextView.setText(FirebaseInstanceId.getInstance().getToken());
    apiKeyEditText = root.findViewById(R.id.apiKeyEditText);
    apiKeyEditText.setText(API_KEY);
    projectIDEditText = root.findViewById(R.id.projectIdEditText);
    projectIDEditText.setText(PROJECT_ID);
    receiverTokenEditText = root.findViewById(R.id.receiverTokenEditText);
    messageEditText = root.findViewById(R.id.messageEditText);
    actionButton = root.findViewById(R.id.floatingActionButton);
    actionButton.setOnClickListener(actionButtonClick);
    copyIcon = root.findViewById(R.id.imageView);
    copyIcon.setOnClickListener(onClickCopyIcon);
    repeatTaskSwitch = root.findViewById(R.id.switch1);
    repeatTaskSwitch.setOnCheckedChangeListener(repeatTaskSwitchChangeListener);
  }
}
