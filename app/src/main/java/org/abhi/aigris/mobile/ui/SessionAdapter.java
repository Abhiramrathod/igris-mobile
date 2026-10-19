package org.abhi.aigris.mobile.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.abhi.aigris.mobile.R;
import org.abhi.aigris.mobile.databinding.ItemSessionBinding;
import org.abhi.aigris.mobile.model.ChatSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    public interface Listener {
        void onSessionClick(ChatSession session);

        void onSessionLongClick(ChatSession session);
    }

    private final List<ChatSession> sessions = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
    private String activeSessionId;
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setSessions(List<ChatSession> newSessions) {
        sessions.clear();
        sessions.addAll(newSessions);
        notifyDataSetChanged();
    }

    public void setActiveSessionId(String sessionId) {
        this.activeSessionId = sessionId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSessionBinding binding = ItemSessionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SessionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        ChatSession session = sessions.get(position);
        holder.binding.txtTitle.setText(session.getTitle());

        boolean active = session.getId().equals(activeSessionId);
        holder.binding.dotActive.setVisibility(active ? android.view.View.VISIBLE : android.view.View.GONE);
        holder.binding.getRoot().setBackgroundResource(
                active ? R.drawable.bg_session_active : android.R.color.transparent);

        long now = System.currentTimeMillis();
        boolean sameDay = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(now).equals(new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                        .format(session.getUpdatedAt()));
        holder.binding.txtMeta.setText(sameDay
                ? timeFormat.format(new Date(session.getUpdatedAt()))
                : dayFormat.format(new Date(session.getUpdatedAt())));

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null && position < sessions.size()) {
                listener.onSessionClick(sessions.get(holder.getBindingAdapterPosition()));
            }
        });
        holder.binding.getRoot().setOnLongClickListener(v -> {
            if (listener != null && position < sessions.size()) {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onSessionLongClick(sessions.get(pos));
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {

        private final ItemSessionBinding binding;

        SessionViewHolder(ItemSessionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
