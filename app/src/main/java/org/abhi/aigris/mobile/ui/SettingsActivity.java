package org.abhi.aigris.mobile.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.abhi.aigris.mobile.R;
import org.abhi.aigris.mobile.data.PrefsManager;
import org.abhi.aigris.mobile.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private PrefsManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = new PrefsManager(this);

        setupModeSpinner();
        loadValues();

        binding.btnSave.setOnClickListener(v -> save());
    }

    private void setupModeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.connection_modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMode.setAdapter(adapter);

        binding.spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean backend = position == 1;
                String hint = getString(backend
                        ? R.string.hint_base_url_backend : R.string.hint_base_url_direct);
                binding.inputBaseUrl.setHint(hint);

                String current = binding.inputBaseUrl.getText() != null
                        ? binding.inputBaseUrl.getText().toString().trim() : "";
                String otherDefault = backend
                        ? PrefsManager.DEFAULT_URL_DIRECT : PrefsManager.DEFAULT_URL_BACKEND;
                if (current.isEmpty() || current.equals(otherDefault)) {
                    binding.inputBaseUrl.setText(backend
                            ? PrefsManager.DEFAULT_URL_BACKEND
                            : PrefsManager.DEFAULT_URL_DIRECT);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadValues() {
        binding.spinnerMode.setSelection(prefs.isBackendMode() ? 1 : 0);

        String baseUrl = prefs.getBaseUrl();
        if (baseUrl.isEmpty()) {
            baseUrl = prefs.isBackendMode()
                    ? PrefsManager.DEFAULT_URL_BACKEND : PrefsManager.DEFAULT_URL_DIRECT;
        }
        binding.inputBaseUrl.setText(baseUrl);
        binding.inputApiKey.setText(prefs.getApiKey());
        binding.inputModel.setText(prefs.getModel());
        binding.inputTemperature.setText(String.valueOf(prefs.getTemperature()));
        binding.inputMaxTokens.setText(String.valueOf(prefs.getMaxTokens()));
        binding.inputSystemPrompt.setText(prefs.getSystemPrompt());
    }

    private void save() {
        prefs.setMode(binding.spinnerMode.getSelectedItemPosition() == 1
                ? PrefsManager.MODE_BACKEND : PrefsManager.MODE_DIRECT);

        String baseUrl = textOf(binding.inputBaseUrl.getText());
        prefs.setBaseUrl(baseUrl);

        prefs.setApiKey(textOf(binding.inputApiKey.getText()));

        String model = textOf(binding.inputModel.getText());
        if (!model.isEmpty()) {
            prefs.setModel(model);
        }

        String temperature = textOf(binding.inputTemperature.getText());
        try {
            if (!temperature.isEmpty()) {
                float value = Float.parseFloat(temperature);
                if (value < 0f || value > 2f) {
                    throw new NumberFormatException();
                }
                prefs.setTemperature(temperature);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.toast_error_number, Toast.LENGTH_SHORT).show();
        }

        String maxTokens = textOf(binding.inputMaxTokens.getText());
        try {
            if (!maxTokens.isEmpty()) {
                int value = Integer.parseInt(maxTokens);
                if (value < 1) {
                    throw new NumberFormatException();
                }
                prefs.setMaxTokens(maxTokens);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.toast_error_number, Toast.LENGTH_SHORT).show();
        }

        prefs.setSystemPrompt(textOf(binding.inputSystemPrompt.getText()));

        Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private static String textOf(CharSequence text) {
        return text != null ? text.toString().trim() : "";
    }
}
