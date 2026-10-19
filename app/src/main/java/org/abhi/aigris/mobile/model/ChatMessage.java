package org.abhi.aigris.mobile.model;

public class ChatMessage {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private final String role;
    private final String content;
    private final long timestamp;
    private final boolean error;

    public ChatMessage(String role, String content, long timestamp) {
        this(role, content, timestamp, false);
    }

    public ChatMessage(String role, String content, long timestamp, boolean error) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.error = error;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isError() {
        return error;
    }
}
