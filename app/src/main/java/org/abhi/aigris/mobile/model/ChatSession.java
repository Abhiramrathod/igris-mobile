package org.abhi.aigris.mobile.model;

import java.util.ArrayList;
import java.util.List;

public class ChatSession {

    private String id;
    private String title;
    private long createdAt;
    private long updatedAt;
    private List<ChatMessage> messages = new ArrayList<>();

    public ChatSession() {
    }

    public ChatSession(String id, long createdAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }

    public void autoTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return;
        }
        for (ChatMessage message : messages) {
            if (ChatMessage.ROLE_USER.equals(message.getRole())) {
                String text = message.getContent().replaceAll("\\s+", " ").trim();
                title = text.length() > 42 ? text.substring(0, 42) + "…" : text;
                return;
            }
        }
        title = "New command";
    }
}
