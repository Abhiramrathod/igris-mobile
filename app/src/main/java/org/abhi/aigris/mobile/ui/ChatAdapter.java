package org.abhi.aigris.mobile.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.abhi.aigris.mobile.R;
import org.abhi.aigris.mobile.databinding.ItemMessageBinding;
import org.abhi.aigris.mobile.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_ASSISTANT = 1;
    private static final int TYPE_LOADING = 2;

    private final List<ChatMessage> messages = new ArrayList<>();
    private boolean loading = false;
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void setLoading(boolean loading) {
        if (this.loading == loading) {
            return;
        }
        this.loading = loading;
        if (loading) {
            notifyItemInserted(messages.size());
        } else {
            int position = messages.size();
            if (position < getItemCount()) {
                notifyItemRemoved(position);
            }
        }
    }

    public void clear() {
        int size = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, size);
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    @Override
    public int getItemViewType(int position) {
        if (loading && position == messages.size()) {
            return TYPE_LOADING;
        }
        return ChatMessage.ROLE_USER.equals(messages.get(position).getRole())
                ? TYPE_USER : TYPE_ASSISTANT;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMessageBinding binding = ItemMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MessageViewHolder(binding, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_LOADING) {
            holder.bindLoading();
        } else {
            holder.bind(messages.get(position), position == messages.size() - 1 && !loading);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size() + (loading ? 1 : 0);
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemMessageBinding binding;
        private final int viewType;
        private TranslateAnimation slideAnimation;
        private AlphaAnimation blinkAnimation;
        private AlphaAnimation pulseAnimation;

        MessageViewHolder(ItemMessageBinding binding, int viewType) {
            super(binding.getRoot());
            this.binding = binding;
            this.viewType = viewType;
        }

        void bind(ChatMessage message, boolean isLast) {
            stopAnimations();

            boolean isUser = ChatMessage.ROLE_USER.equals(message.getRole());
            binding.dot.setBackgroundResource(
                    isUser ? R.drawable.bg_dot_user : R.drawable.bg_dot_assistant);
            binding.line.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);

            binding.txtAuthor.setText(isUser
                    ? R.string.author_user : R.string.author_assistant);
            binding.txtAuthor.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(),
                    isUser ? R.color.mc_text_secondary : R.color.mc_gold));

            binding.txtTime.setText(timeFormat.format(new Date(message.getTimestamp())));
            binding.txtTime.setTextColor(ContextCompat.getColor(
                    binding.getRoot().getContext(), R.color.mc_text_muted));
            binding.txtTime.setVisibility(View.VISIBLE);

            binding.txtBody.setVisibility(View.VISIBLE);
            binding.txtBody.setText(message.getContent());
            binding.txtBody.setBackgroundResource(
                    isUser ? R.drawable.bg_bubble_user : R.drawable.bg_bubble_ai);
            binding.txtBody.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(),
                    message.isError() ? R.color.mc_red
                            : (isUser ? R.color.mc_warm_white : R.color.mc_text)));

            binding.loadingBar.setVisibility(View.GONE);

            binding.getRoot().setAlpha(0f);
            binding.getRoot().setTranslationY(12f);
            binding.getRoot().animate().alpha(1f).translationY(0f)
                    .setDuration(320).setStartDelay(30).start();
        }

        void bindLoading() {
            stopAnimations();

            binding.dot.setBackgroundResource(R.drawable.bg_dot_assistant);
            binding.line.setVisibility(View.VISIBLE);
            binding.txtAuthor.setText(R.string.author_assistant);
            binding.txtAuthor.setTextColor(
                    ContextCompat.getColor(binding.getRoot().getContext(), R.color.mc_gold));

            binding.txtTime.setVisibility(View.VISIBLE);
            binding.txtTime.setText(R.string.processing);
            binding.txtTime.setTextColor(
                    ContextCompat.getColor(binding.getRoot().getContext(), R.color.mc_gold));

            binding.txtBody.setVisibility(View.GONE);
            binding.loadingBar.setVisibility(View.VISIBLE);

            slideAnimation = new TranslateAnimation(
                    TranslateAnimation.RELATIVE_TO_SELF, -1f,
                    TranslateAnimation.RELATIVE_TO_SELF, 3.5f,
                    TranslateAnimation.ABSOLUTE, 0f,
                    TranslateAnimation.ABSOLUTE, 0f);
            slideAnimation.setDuration(1200);
            slideAnimation.setRepeatCount(TranslateAnimation.INFINITE);
            slideAnimation.setRepeatMode(TranslateAnimation.RESTART);
            slideAnimation.setInterpolator(new LinearInterpolator());
            binding.loadingFill.startAnimation(slideAnimation);

            blinkAnimation = new AlphaAnimation(1f, 0.1f);
            blinkAnimation.setDuration(500);
            blinkAnimation.setRepeatCount(AlphaAnimation.INFINITE);
            blinkAnimation.setRepeatMode(AlphaAnimation.REVERSE);
            binding.txtTime.startAnimation(blinkAnimation);

            pulseAnimation = new AlphaAnimation(1f, 0.35f);
            pulseAnimation.setDuration(1000);
            pulseAnimation.setRepeatCount(AlphaAnimation.INFINITE);
            pulseAnimation.setRepeatMode(AlphaAnimation.REVERSE);
            binding.dot.startAnimation(pulseAnimation);
        }

        void stopAnimations() {
            binding.getRoot().animate().cancel();
            binding.getRoot().setAlpha(1f);
            binding.getRoot().setTranslationY(0f);
            if (slideAnimation != null) {
                binding.loadingFill.clearAnimation();
                slideAnimation = null;
            }
            if (blinkAnimation != null) {
                binding.txtTime.clearAnimation();
                blinkAnimation = null;
            }
            if (pulseAnimation != null) {
                binding.dot.clearAnimation();
                pulseAnimation = null;
            }
        }
    }
}
