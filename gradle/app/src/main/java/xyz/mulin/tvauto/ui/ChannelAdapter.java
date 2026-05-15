package xyz.mulin.tvauto.ui;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import xyz.mulin.tvauto.R;
import xyz.mulin.tvauto.model.Channel;

public final class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.Holder> {
    public interface Listener {
        void onChannelClicked(int position);

        void onRequestSettingsFocus();
    }

    private final Listener listener;
    private final List<Channel> channels = new ArrayList<>();
    private int currentChannelIndex;
    private int itemHeightPx = ViewGroup.LayoutParams.WRAP_CONTENT;

    public ChannelAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitChannels(List<Channel> items, int currentChannelIndex) {
        channels.clear();
        channels.addAll(items);
        this.currentChannelIndex = currentChannelIndex;
        notifyDataSetChanged();
    }

    public void setCurrentChannelIndex(int currentChannelIndex) {
        this.currentChannelIndex = currentChannelIndex;
        notifyDataSetChanged();
    }

    public void setItemHeightPx(int itemHeightPx) {
        if (this.itemHeightPx == itemHeightPx) return;
        this.itemHeightPx = itemHeightPx;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        applyItemHeight(holder);
        Channel channel = channels.get(position);
        holder.tvNum.setText(String.valueOf(position + 1));
        holder.tvName.setText(channel.getName());
        holder.indicator.setVisibility(position == currentChannelIndex ? View.VISIBLE : View.GONE);
        updateItemStyle(holder, false);

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onChannelClicked(adapterPosition);
            }
        });
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> updateItemStyle(holder, hasFocus));
        holder.itemView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return false;

            boolean isFirst = adapterPosition == 0;
            boolean isLast = adapterPosition == getItemCount() - 1;
            boolean up = keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_W;
            boolean down = keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_S;
            if ((isFirst && up) || (isLast && down)) {
                listener.onRequestSettingsFocus();
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    private void updateItemStyle(Holder holder, boolean hasFocus) {
        if (hasFocus) {
            holder.tvName.setTextColor(Color.BLACK);
            holder.tvNum.setTextColor(Color.DKGRAY);
            holder.itemView.animate().scaleX(1.02f).scaleY(1.02f).setDuration(150).start();
            return;
        }

        int position = holder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION) return;
        boolean isPlaying = position == currentChannelIndex;
        holder.tvName.setTextColor(isPlaying ? Color.parseColor("#0079FB") : Color.WHITE);
        holder.tvNum.setTextColor(Color.parseColor("#88FFFFFF"));
        holder.itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
    }

    private void applyItemHeight(Holder holder) {
        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        if (layoutParams != null && layoutParams.height != itemHeightPx) {
            layoutParams.height = itemHeightPx;
            holder.itemView.setLayoutParams(layoutParams);
        }

        if (itemHeightPx == ViewGroup.LayoutParams.WRAP_CONTENT) return;

        int contentHeightPx = dp(holder.itemView, 22);
        int verticalPaddingPx = (itemHeightPx - contentHeightPx) / 2;
        verticalPaddingPx = Math.max(dp(holder.itemView, 6), Math.min(dp(holder.itemView, 16), verticalPaddingPx));
        holder.itemView.setPadding(
                holder.itemView.getPaddingLeft(),
                verticalPaddingPx,
                holder.itemView.getPaddingRight(),
                verticalPaddingPx
        );
    }

    private int dp(View view, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                view.getResources().getDisplayMetrics()
        );
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView tvNum;
        final TextView tvName;
        final View indicator;

        Holder(View itemView) {
            super(itemView);
            tvNum = itemView.findViewById(R.id.tvNum);
            tvName = itemView.findViewById(R.id.tvName);
            indicator = itemView.findViewById(R.id.indicator);
        }
    }
}
