package org.abhi.aigris.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.abhi.aigris.mobile.R;
import org.abhi.aigris.mobile.api.LlmClient;
import org.abhi.aigris.mobile.data.PrefsManager;
import org.abhi.aigris.mobile.data.SessionStore;
import org.abhi.aigris.mobile.databinding.ActivityMainBinding;
import org.abhi.aigris.mobile.model.ChatMessage;
import org.abhi.aigris.mobile.model.ChatSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PrefsManager prefs;
    private SessionStore sessionStore;
    private LlmClient client;
    private ChatAdapter chatAdapter;
    private SessionAdapter sessionAdapter;
    private ChatSession activeSession;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final List<String> modelList = new ArrayList<>();
    private ArrayAdapter<String> modelAdapter;
    private boolean loading = false;
    private boolean setupPromptShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = new PrefsManager(this);
        sessionStore = new SessionStore(this);
        client = new LlmClient();

        activeSession = sessionStore.getActiveSession();
        messages.addAll(activeSession.getMessages());

        setupTopBar();
        setupChatList();
        setupDrawer();
        setupComposer();
        bindActiveSession();
        startCrownRotation();

        if (prefs.getApiKey().isEmpty() && !prefs.isBackendMode() && !setupPromptShown) {
            setupPromptShown = true;
            showSetupDialog();
        }
    }

    private void setupTopBar() {
        binding.txtDate.setText(new SimpleDateFormat("EEE, MMM d", Locale.US)
                .format(new Date()));

        binding.btnHistory.setOnClickListener(v ->
                binding.drawer.openDrawer(GravityCompat.START));

        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        binding.btnNewChat.setOnClickListener(v -> newSession());
    }

    private void setupChatList() {
        chatAdapter = new ChatAdapter();
        binding.recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerMessages.setAdapter(chatAdapter);
    }

    private void setupDrawer() {
        binding.drawerPanel.recyclerSessions.setLayoutManager(new LinearLayoutManager(this));

        sessionAdapter = new SessionAdapter();
        binding.drawerPanel.recyclerSessions.setAdapter(sessionAdapter);

        sessionAdapter.setListener(new SessionAdapter.Listener() {
            @Override
            public void onSessionClick(ChatSession session) {
                if (loading || session.getId().equals(activeSession.getId())) {
                    binding.drawer.closeDrawer(GravityCompat.START);
                    return;
                }
                sessionStore.setActive(session.getId());
                switchToSession(session);
                binding.drawer.closeDrawer(GravityCompat.START);
            }

            @Override
            public void onSessionLongClick(ChatSession session) {
                confirmDeleteSession(session);
            }
        });

        binding.drawerPanel.btnNewCommand.setOnClickListener(v -> newSession());

        refreshSessions();
    }

    private void setupComposer() {
        modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modelList);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerModel.setAdapter(modelAdapter);

        binding.spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < modelList.size()) {
                    prefs.setModel(modelList.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        binding.inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnSend.setEnabled(!loading && s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.inputMessage.setOnEditorActionListener((v, actionId, event) -> {
            boolean fromSendKey = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND || fromSendKey) {
                sendMessage();
                return true;
            }
            return false;
        });

        binding.btnSend.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshModels();
        refreshSessions();
    }

    @Override
    public void onBackPressed() {
        if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
            binding.drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void refreshModels() {
        Set<String> models = new LinkedHashSet<>();
        models.add(prefs.getModel());
        modelList.clear();
        modelList.addAll(models);
        modelAdapter.notifyDataSetChanged();

        client.fetchModels(prefs, new LlmClient.ModelsCallback() {
            @Override
            public void onModels(List<String> fetched) {
                if (isFinishing() || isDestroyed() || fetched.isEmpty()) {
                    return;
                }
                modelList.clear();
                modelList.addAll(fetched);
                if (!modelList.contains(prefs.getModel())) {
                    modelList.add(0, prefs.getModel());
                }
                modelAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    private void switchToSession(ChatSession session) {
        activeSession = session;
        messages.clear();
        messages.addAll(session.getMessages());
        chatAdapter.clear();
        for (ChatMessage message : messages) {
            chatAdapter.addMessage(message);
        }
        binding.welcome.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        sessionAdapter.setActiveSessionId(session.getId());
        refreshSessions();
        scrollToBottom();
    }

    private void bindActiveSession() {
        sessionAdapter.setActiveSessionId(activeSession.getId());
        chatAdapter.clear();
        for (ChatMessage message : messages) {
            chatAdapter.addMessage(message);
        }
        binding.welcome.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        refreshSessions();
    }

    private void newSession() {
        if (loading) {
            return;
        }
        ChatSession session = sessionStore.createSession();
        switchToSession(session);
        binding.drawer.closeDrawer(GravityCompat.START);
    }

    private void confirmDeleteSession(ChatSession session) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_session_title)
                .setMessage(R.string.dialog_delete_session_message)
                .setPositiveButton(R.string.dialog_delete_session_positive, (d, w) -> {
                    boolean wasActive = session.getId().equals(activeSession.getId());
                    sessionStore.delete(session.getId());
                    if (wasActive) {
                        switchToSession(sessionStore.getActiveSession());
                    } else {
                        refreshSessions();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void refreshSessions() {
        sessionAdapter.setSessions(sessionStore.getSessionsSorted());
        sessionAdapter.setActiveSessionId(activeSession.getId());
    }

    private void sendMessage() {
        if (loading) {
            return;
        }
        String text = binding.inputMessage.getText() != null
                ? binding.inputMessage.getText().toString().trim() : "";
        if (text.isEmpty()) {
            return;
        }
        if (prefs.getApiKey().isEmpty() && !prefs.isBackendMode()) {
            showSetupDialog();
            return;
        }

        ChatMessage userMessage = new ChatMessage(
                ChatMessage.ROLE_USER, text, System.currentTimeMillis());
        messages.add(userMessage);
        chatAdapter.addMessage(userMessage);
        sessionStore.save(activeSession);

        binding.welcome.setVisibility(View.GONE);
        binding.inputMessage.setText("");
        setLoading(true);
        scrollToBottom();

        final String sourceSessionId = activeSession.getId();
        List<ChatMessage> snapshot = new ArrayList<>(messages);
        client.sendChat(snapshot, prefs, sourceSessionId,
                new LlmClient.ResponseCallback() {
                    @Override
                    public void onSuccess(String response) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (!sourceSessionId.equals(activeSession.getId())) {
                            setLoading(false);
                            return;
                        }
                        appendAssistant(response, false);
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (!sourceSessionId.equals(activeSession.getId())) {
                            setLoading(false);
                            return;
                        }
                        appendAssistant("Error: " + message, true);
                    }
                });
    }

    private void appendAssistant(String content, boolean error) {
        ChatMessage assistantMessage = new ChatMessage(
                ChatMessage.ROLE_ASSISTANT, content, System.currentTimeMillis(), error);
        messages.add(assistantMessage);
        chatAdapter.addMessage(assistantMessage);
        sessionStore.save(activeSession);
        setLoading(false);
        scrollToBottom();
    }

    private void setLoading(boolean isLoading) {
        loading = isLoading;
        chatAdapter.setLoading(isLoading);
        binding.inputMessage.setEnabled(!isLoading);
        binding.spinnerModel.setEnabled(!isLoading);
        binding.btnSend.setEnabled(!isLoading
                && binding.inputMessage.getText() != null
                && binding.inputMessage.getText().toString().trim().length() > 0);
    }

    private void showSetupDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_setup_title)
                .setMessage(R.string.dialog_setup_message)
                .setPositiveButton(R.string.dialog_setup_positive,
                        (d, w) -> startActivity(new Intent(this, SettingsActivity.class)))
                .setNegativeButton(R.string.dialog_setup_negative, null)
                .show();
    }

    private void scrollToBottom() {
        binding.recyclerMessages.post(() -> {
            if (chatAdapter.getItemCount() > 0) {
                binding.recyclerMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            }
        });
    }

    private void startCrownRotation() {
        RotateAnimation animation = new RotateAnimation(
                0f, 360f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(40000);
        animation.setRepeatCount(RotateAnimation.INFINITE);
        animation.setInterpolator(new android.view.animation.LinearInterpolator());
        binding.imgBgCrown.startAnimation(animation);
    }
}
