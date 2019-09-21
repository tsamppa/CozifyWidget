package com.cozify.cozifywidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

public class CozifyWidgetSetupActivity extends AppCompatActivity {
    static final String TAG = "CozifyWidgetConfigure";
    static final String PREFS_NAME
            = "com.cozify.android.apis.appwidget.CozifyWidgetProvider";
    static final String PREF_PREFIX_KEY = "prefix_";

    private CozifyApiReal cozifyAPI = new CozifyApiReal();

    TextView textViewStatus;
    EditText editTextEmail;
    TextInputLayout textInputLayoutEmail;
    EditText editTextPw;
    TextInputLayout textInputLayoutPw;
    Button buttonLogin;

    String emailAddress;
    String cloudtoken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget);
        editTextEmail = findViewById(R.id.email_edit);
        editTextEmail.setText(emailAddress);
        textViewStatus = findViewById(R.id.login_status);
        buttonLogin = findViewById(R.id.login_button);
        textInputLayoutEmail = findViewById(R.id.email_layout);

        // Login automatically with saved credentials or show the Login screen
        if (!checkAccess()) {
            enableEmailLogin();
        }

        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int str_len = s.length();
                if (str_len > 0) {
                    textInputLayoutEmail.setErrorEnabled(false);
                } else {
                    textInputLayoutEmail.setErrorEnabled(true);
                    textInputLayoutEmail.setError("Please enter email");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        // Bind the action for the save button.
        buttonLogin.setOnClickListener(mOnClickListenerLogin);

        // Find the Password  EditText
        editTextPw = findViewById(R.id.appwidget_pw);
        textInputLayoutPw = findViewById(R.id.password);
        textInputLayoutPw.setEnabled(false);
        editTextPw.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int str_len = s.length();
                if (str_len == 6) {
                    textInputLayoutPw.setErrorEnabled(false);
                    confirmPassword(editTextPw.getText().toString(), emailAddress);
                } else {
                    textInputLayoutPw.setErrorEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void showEmailFields() {
        textInputLayoutEmail.setVisibility(View.VISIBLE);
        textInputLayoutEmail.requestFocus();
        buttonLogin.setVisibility(View.VISIBLE);
    }

    private void showPasswordField() {
        textInputLayoutPw.setVisibility(View.VISIBLE);
        textInputLayoutPw.requestFocus();
    }


    private void listHubs() {
        cozifyAPI.listHubs(new CozifyApiReal.StringCallback() {
            @Override
            public void result(boolean success, String message, String result) {
                if (success) {
                    textViewStatus.setText("Hubs found in LAN: "+result);
                } else {
                    textViewStatus.setText(message);
                }
            }
        });
    }

    private void confirmPassword(String pw, String email_address) {
        cozifyAPI.confirmPassword(pw, email_address, new CozifyApiReal.StringCallback() {
            @Override
            public void result(boolean success, String message, String result) {
                if (success) {
                    cloudtoken = result;
                    setAuthHeader();
                    final Context context = CozifyWidgetSetupActivity.this;
                    PersistentStorage.getInstance().saveCloudToken(context, cloudtoken);
                    getHubKeys();
                } else {
                    textViewStatus.setText(message);
                }
            }
        });
    }

    private void setAuthHeader() {
        cozifyAPI.setCloudToken(cloudtoken);
    }

    private void updateAllWidgets() {
        Intent intent = new Intent(this, CozifyAppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), CozifyAppWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void getHubKeys() {
        textViewStatus.setText("Checking connection..");
        buttonLogin.setEnabled(false);
        cozifyAPI.getHubKeys(new CozifyApiReal.JsonCallback() {
            @Override
            public void result(boolean success, String message, JSONObject jsonResult) {
                buttonLogin.setEnabled(true);
                if (success) {
                    final Context context = CozifyWidgetSetupActivity.this;
                    Toast.makeText(context, "Connected to Cozify. You can now create widgets.", Toast.LENGTH_SHORT).show();
                    updateAllWidgets();
                    Intent resultValue = new Intent();
                    setResult(RESULT_OK, resultValue);
                    finish();
                } else {
                    enableEmailLogin();
                }
            }
        });
    }

    private void enableEmailLogin() {
        listHubs();
        showEmailFields();
    }

    View.OnClickListener mOnClickListenerLogin = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = CozifyWidgetSetupActivity.this;
            // When the button is clicked, save the string in our prefs and return that they
            // clicked OK.
            final String email_address = editTextEmail.getText().toString();
            if (email_address.length() > 0) {
                textInputLayoutEmail.setErrorEnabled(false);
                // Login with email
                emailAddress = email_address;
                cozifyAPI.requestLogin(emailAddress, new CozifyApiReal.StringCallback() {
                    @Override
                    public void result(boolean success, String message, String result) {
                        if (success) {
                            showPasswordField();
                            textInputLayoutEmail.setErrorEnabled(false);
                            textInputLayoutPw.setEnabled(true);
                            editTextPw.setSelection(0);
                            textViewStatus.setText("Check your email for the temporary password.");
                        } else {
                            textViewStatus.setText(message + ": " + result);
                        }
                    }
                });
                PersistentStorage.getInstance().saveEmail(context, email_address);
            } else {
                textInputLayoutEmail.setErrorEnabled(true);
                textInputLayoutEmail.setError("Please enter login email");
                textViewStatus.setText("Please enter login email");
            }
        }
    };

    private boolean checkAccess() {
        final Context context = CozifyWidgetSetupActivity.this;
        emailAddress = PersistentStorage.getInstance().loadEmail(context);
        if (emailAddress == null || emailAddress.length() < 5) {
            return false;
        }
        editTextEmail.setText(emailAddress);
        cloudtoken = PersistentStorage.getInstance().loadCloudToken(context);
        if (cloudtoken == null || cloudtoken.length() < 5) {
            return false;
        }
        setAuthHeader();
        getHubKeys();
        return true;
    }

}
