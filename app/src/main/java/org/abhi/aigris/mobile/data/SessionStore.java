package org.abhi.aigris.mobile.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.abhi.aigris.mobile.model.ChatMessage;
import org.abhi.aigris.mobile.model.ChatSession;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Local session storage, ChatGPT-style: every conversation is a saved session
 * that survives app restarts and can be reopened from the sidebar.
 */
public class SessionStore {

    private static final String PREFS_NAME = "igris_sessions";
    private static final String KEY_SESSIONS = "sessions";
    private static final String KEY_ACTIVE_ID = "active_session_id";

    private static final String LEGACY_PREFS_NAME = "igris_prefs";
    private static final String LEGACY_CHAT_HISTORY = "chat_history";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final List<ChatSession> sessions = new ArrayList<>();

    public SessionStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sessions.addAll(load());
        migrateLegacyHistory(context);

        if (sessions.isEmpty()) {
            sessions.add(new ChatSession(UUID.randomUUID().toString(), System.currentTimeMillis()));
            persistSessions();
        }
        if (find(getActiveId()) == null) {
            setActiveId(sessions.get(0).getId());
        }
    }

    public ChatSession getActiveSession() {
        ChatSession session = find(getActiveId());
        return session != null ? session : sessions.get(0);
    }

    public void setActive(String sessionId) {
        if (find(sessionId) != null) {
            setActiveId(sessionId);
        }
    }

    public List<ChatSession> getSessionsSorted() {
        List<ChatSession> sorted = new ArrayList<>(sessions);
        Collections.sort(sorted, Comparator.comparingLong(ChatSession::getUpdatedAt).reversed());
        return sorted;
    }

    public ChatSession createSession() {
        ChatSession session = new ChatSession(UUID.randomUUID().toString(), System.currentTimeMillis());
        sessions.add(session);
        setActiveId(session.getId());
        persistSessions();
        return session;
    }

    public void save(ChatSession session) {
        session.setUpdatedAt(System.currentTimeMillis());
        session.autoTitle();
        if (find(session.getId()) == null) {
            sessions.add(session);
        }
        persistSessions();
    }

    public void delete(String sessionId) {
        sessions.removeIf(s -> s.getId().equals(sessionId));
        if (sessions.isEmpty()) {
            sessions.add(new ChatSession(UUID.randomUUID().toString(), System.currentTimeMillis()));
        }
        if (find(getActiveId()) == null) {
            setActiveId(sessions.get(0).getId());
        }
        persistSessions();
    }

    private ChatSession find(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        for (ChatSession session : sessions) {
            if (session.getId().equals(sessionId)) {
                return session;
            }
        }
        return null;
    }

    private String getActiveId() {
        return prefs.getString(KEY_ACTIVE_ID, null);
    }

    private void setActiveId(String sessionId) {
        prefs.edit().putString(KEY_ACTIVE_ID, sessionId).apply();
    }

    private List<ChatSession> load() {
        String json = prefs.getString(KEY_SESSIONS, "");
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<List<ChatSession>>() {}.getType();
            List<ChatSession> loaded = gson.fromJson(json, type);
            return loaded != null ? loaded : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void persistSessions() {
        prefs.edit().putString(KEY_SESSIONS, gson.toJson(sessions)).apply();
    }

    /** Imports the pre-sessions single-chat history so nothing is lost on upgrade. */
    private void migrateLegacyHistory(Context context) {
        SharedPreferences legacy = context.getApplicationContext()
                .getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE);
        String json = legacy.getString(LEGACY_CHAT_HISTORY, "");
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            Type type = new TypeToken<List<ChatMessage>>() {}.getType();
            List<ChatMessage> oldMessages = gson.fromJson(json, type);
            if (oldMessages != null && !oldMessages.isEmpty()) {
                ChatSession session = new ChatSession(
                        UUID.randomUUID().toString(), System.currentTimeMillis());
                session.setMessages(oldMessages);
                session.autoTitle();
                sessions.add(0, session);
                setActiveId(session.getId());
                persistSessions();
            }
        } catch (Exception ignored) {
        }
        legacy.edit().remove(LEGACY_CHAT_HISTORY).apply();
    }
}
