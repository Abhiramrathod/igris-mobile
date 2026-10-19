package org.abhi.aigris.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.abhi.aigris.mobile.R;
import org.abhi.aigris.mobile.api.LlmClient;
import org.abhi.aigris.mobile.data.PrefsManager;
import org.abhi.aigris.mobile.databinding.ActivityMainBinding;
import org.abhi.aigris.mobile.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PrefsManager prefs;
    private LlmClient client;
    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private String sessionId = UUID.randomUUID().toString();
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
        client = new LlmClient();

        setupTopBar();
        setupChatList();
        setupComposer();
        restoreHistory();
        startCrownRotation();

        if (prefs.getApiKey().isEmpty() && !prefs.isBackendMode() && !setupPromptShown) {
            setupPromptShown = true;
            showSetupDialog();
        }
    }

    private void setupTopBar() {
        binding.txtDate.setText(new SimpleDateFormat("EEE, MMM d", Locale.US)
                .format(new Date()));

        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        binding.btnNewChat.setOnClickListener(v -> confirmNewChat());
    }

    private void setupChatList() {
        adapter = new ChatAdapter();
        binding.recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerMessages.setAdapter(adapter);
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
            sendMessage();
            return true;
        });

        binding.btnSend.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshModels();
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
        adapter.addMessage(userMessage);

        binding.welcome.setVisibility(View.GONE);
        binding.inputMessage.setText("");
        setLoading(true);
        scrollToBottom();

        List<ChatMessage> snapshot = new ArrayList<>(messages);
        client.sendChat(snapshot, prefs, sessionId, new LlmClient.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                appendAssistant(response, false);
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) {
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
        adapter.addMessage(assistantMessage);
        setLoading(false);
        persistHistory();
        scrollToBottom();
    }

    private void setLoading(boolean isLoading) {
        loading = isLoading;
        adapter.setLoading(isLoading);
        binding.inputMessage.setEnabled(!isLoading);
        binding.spinnerModel.setEnabled(!isLoading);
        binding.btnSend.setEnabled(!isLoading
                && binding.inputMessage.getText() != null
                && binding.inputMessage.getText().toString().trim().length() > 0);
    }

    private void restoreHistory() {
        messages.clear();
        messages.addAll(prefs.loadChatHistory());
        adapter.clear();
        for (ChatMessage message : messages) {
            adapter.addMessage(message);
        }
        binding.welcome.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void persistHistory() {
        prefs.saveChatHistory(messages);
    }

    private void confirmNewChat() {
        if (messages.isEmpty()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_new_chat_title)
                .setMessage(R.string.dialog_new_chat_message)
                .setPositiveButton(R.string.dialog_new_chat_positive, (d, w) -> {
                    messages.clear();
                    adapter.clear();
                    prefs.clearChatHistory();
                    binding.welcome.setVisibility(View.VISIBLE);
                    sessionId = UUID.randomUUID().toString();
                })
                .setNegativeButton(R.string.dialog_new_chat_negative, null)
                .show();
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
            if (adapter.getItemCount() > 0) {
                binding.recyclerMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
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
