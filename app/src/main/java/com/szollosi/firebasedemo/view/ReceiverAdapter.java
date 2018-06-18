package com.szollosi.firebasedemo.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.szollosi.firebasedemo.PlaceholderFragment;
import com.szollosi.firebasedemo.R;
import com.szollosi.firebasedemo.data.ReceiverItem;
import com.szollosi.firebasedemo.databinding.ReceiverItemBinding;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ReceiverAdapter extends RecyclerView.Adapter<ReceiverAdapter.ViewHolder> {

  @NonNull
  private ArrayList<ReceiverItem> items = new ArrayList<>();

  private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(final Context context, final Intent intent) {
      ReceiverItem receiverItem = intent.getParcelableExtra(PlaceholderFragment.KEY_EXTRA_MESSAGE);
      if (receiverItem != null) {
        items.add(receiverItem);
      }
      notifyItemInserted(items.size() - 1);
    }
  };

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
    ReceiverItemBinding binding = DataBindingUtil.inflate(
        LayoutInflater.from(parent.getContext()),
        R.layout.receiver_item, parent, false);
    return new ViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
    ReceiverItem receiverItem = items.get(position);
    holder.binding.setReceiverItem(receiverItem);
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  public BroadcastReceiver getBroadcastReceiver() {
    return broadcastReceiver;
  }

  public void destroy() {
    this.broadcastReceiver = null;
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    private final ReceiverItemBinding binding;

    @BindView(R.id.message_id_value)
    public TextView messageId;

    @BindView(R.id.message_received_value)
    public TextView messageReceived;

    @BindView(R.id.message_content_value)
    public TextView messageContent;

    @BindView(R.id.message_send_date_value)
    public TextView messageSent;

    @BindView(R.id.message_date_diff_value)
    public TextView messageDateDiff;

    ViewHolder(ReceiverItemBinding binding) {
      super(binding.getRoot());
      ButterKnife.bind(binding.getRoot());
      this.binding = binding;
    }
  }
}
