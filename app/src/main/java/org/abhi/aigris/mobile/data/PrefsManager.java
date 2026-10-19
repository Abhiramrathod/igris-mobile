package org.abhi.aigris.mobile.data;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {

    public static final String MODE_DIRECT = "direct";
    public static final String MODE_BACKEND = "backend";

    public static final String DEFAULT_URL_DIRECT = "https://integrate.api.nvidia.com/v1";
    public static final String DEFAULT_URL_BACKEND = "http://10.0.2.2:8080";
    public static final String DEFAULT_MODEL = "mistralai/mistral-small-4-119b-2603";
    public static final String DEFAULT_SYSTEM_PROMPT =
            "You are IGRIS, the royal AI command system of the Shadow Monarch. Be precise, powerful, and helpful.";

    private static final String PREFS_NAME = "igris_prefs";
    private static final String KEY_MODE = "mode";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL = "model";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_MAX_TOKENS = "max_tokens";
    private static final String KEY_SYSTEM_PROMPT = "system_prompt";

    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getMode() {
        return prefs.getString(KEY_MODE, MODE_DIRECT);
    }

    public void setMode(String mode) {
        prefs.edit().putString(KEY_MODE, mode).apply();
    }

    public boolean isBackendMode() {
        return MODE_BACKEND.equals(getMode());
    }

    public String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, "");
    }

    public void setBaseUrl(String baseUrl) {
        prefs.edit().putString(KEY_BASE_URL, baseUrl).apply();
    }

    public String getEffectiveBaseUrl() {
        String url = getBaseUrl().trim();
        if (url.isEmpty()) {
            return isBackendMode() ? DEFAULT_URL_BACKEND : DEFAULT_URL_DIRECT;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "").trim();
    }

    public void setApiKey(String apiKey) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public String getModel() {
        String model = prefs.getString(KEY_MODEL, "").trim();
        return model.isEmpty() ? DEFAULT_MODEL : model;
    }

    public void setModel(String model) {
        prefs.edit().putString(KEY_MODEL, model).apply();
    }

    public float getTemperature() {
        try {
            return Float.parseFloat(prefs.getString(KEY_TEMPERATURE, "1.0"));
        } catch (NumberFormatException e) {
            return 1.0f;
        }
    }

    public void setTemperature(String temperature) {
        prefs.edit().putString(KEY_TEMPERATURE, temperature).apply();
    }

    public int getMaxTokens() {
        try {
            return Integer.parseInt(prefs.getString(KEY_MAX_TOKENS, "8192"));
        } catch (NumberFormatException e) {
            return 8192;
        }
    }

    public void setMaxTokens(String maxTokens) {
        prefs.edit().putString(KEY_MAX_TOKENS, maxTokens).apply();
    }

    public String getSystemPrompt() {
        String prompt = prefs.getString(KEY_SYSTEM_PROMPT, "").trim();
        return prompt.isEmpty() ? DEFAULT_SYSTEM_PROMPT : prompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        prefs.edit().putString(KEY_SYSTEM_PROMPT, systemPrompt).apply();
    }
}
