package com.szollosi.firebasedemo;

import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.szollosi.firebasedemo.view.ReceiverAdapter;
import com.szollosi.firebasedemo.view.SectionSendFragment;

public class PlaceholderFragment extends Fragment {

  public static final String ACTION_MESSAGE_RECEIVED = "action.message.received";

  public static final String KEY_EXTRA_MESSAGE = "key-message";

  public static final String KEY_EXTRA_DATE = "key-date";

  private static final String TAG = PlaceholderFragment.class.getName();

  /**
   * The fragment argument representing the section number for this
   * fragment.
   */
  private static final String ARG_SECTION_NUMBER = "section_number";

  private static final int SECTION_RECEIVER = 1;

  private static final int SECTION_SENDER = 2;

  private RecyclerView receiverRecyclerView;

  private ReceiverAdapter receiverAdapter;

  private SectionSendFragment sendFragment;

  /**
   * Returns a new instance of this fragment for the given section
   * number.
   */
  public static PlaceholderFragment newInstance(int sectionNumber) {
    PlaceholderFragment fragment = new PlaceholderFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_SECTION_NUMBER, sectionNumber);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    this.receiverRecyclerView = null;
    this.receiverAdapter.destroy();
    LocalBroadcastManager.getInstance(this.getContext().getApplicationContext())
        .unregisterReceiver(receiverAdapter.getBroadcastReceiver());
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    Bundle args = getArguments();
    receiverAdapter = new ReceiverAdapter();
    LocalBroadcastManager.getInstance(this.getContext().getApplicationContext())
        .registerReceiver(receiverAdapter.getBroadcastReceiver(),
            new IntentFilter(ACTION_MESSAGE_RECEIVED));
    if (args == null) {
      return null;
    }

    int sectionNumber = 0;
    if (getArguments() != null) {
      sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
    }
    switch (sectionNumber) {
      case SECTION_RECEIVER: {
        View rootView = inflater.inflate(R.layout.fragment_receiver, container, false);
        receiverRecyclerView = rootView.findViewById(R.id.fragment_receiver_recycler_view);
        receiverRecyclerView.setHasFixedSize(true);
        receiverRecyclerView.setItemAnimator(new DefaultItemAnimator());
        receiverRecyclerView.setAdapter(receiverAdapter);
        return rootView;
      }
      case SECTION_SENDER: {
        sendFragment = new SectionSendFragment();
        return sendFragment.onCreateView(inflater, container, null);
      }
      default: {
        Log.w(TAG, "[onCreateView] Invalid section number: " + sectionNumber);
        return null;
      }
    }
  }
}
