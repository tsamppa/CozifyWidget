package com.example.cozifywidget;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;

public class CozifyWidgetSetupActivity extends AppCompatActivity {
    static final String TAG = "CozifyWidgetConfigure";
    static final String PREFS_NAME
            = "com.example.android.apis.appwidget.CozifyWidgetProvider";
    static final String PREF_PREFIX_KEY = "prefix_";

    TextView textViewStatus;
    EditText editTextEmail;
    TextInputLayout textInputLayoutEmail;
    EditText editTextPw;
    TextInputLayout textInputLayoutPw;
    Button buttonCreate;

    String emailAddress;
    String cloudtoken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget);
        final Context context = CozifyWidgetSetupActivity.this;

        editTextEmail = findViewById(R.id.email_edit);
        editTextEmail.setText(emailAddress);
        textViewStatus = findViewById(R.id.login_status);
        buttonCreate = findViewById(R.id.login_button);
        textInputLayoutEmail = findViewById(R.id.email_layout);

        // Login automatically with saved credentials
        checkAccess();

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
        buttonCreate.setOnClickListener(mOnClickListenerLogin);

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
        listHubs();
    }

    private void listHubs() {
        CozifyAPI.getInstance().listHubs(new CozifyAPI.StringCallback() {
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
        CozifyAPI.getInstance().confirmPassword(pw, email_address, new CozifyAPI.StringCallback() {
            @Override
            public void result(boolean success, String message, String result) {
                if (success) {
                    cloudtoken = result;
                    setAuthHeader();
                    final Context context = CozifyWidgetSetupActivity.this;
                    PersistentStorage.getInstance().saveCloudToken(context, cloudtoken);
                    getHubKeys();
                    textViewStatus.setText(message + ": token " + cloudtoken);
                } else {
                    textViewStatus.setText(message);
                }
            }
        });
    }

    private void setAuthHeader() {
        CozifyAPI.getInstance().setCloudToken(cloudtoken);
    }

    private void getHubKeys() {
        textViewStatus.setText("Checking connection..");
        buttonCreate.setEnabled(false);
        CozifyAPI.getInstance().getHubKeys(new CozifyAPI.JsonCallback() {
            @Override
            public void result(boolean success, String message, JSONObject jsonResult) {
                buttonCreate.setEnabled(true);
                if (success) {
                    textViewStatus.setText(jsonResult.toString());
                    Intent resultValue = new Intent();
                    setResult(RESULT_OK, resultValue);
                    finish();
                } else {
                    textViewStatus.setText("Please check internet connection and try again");
                }
            }
        });
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
                CozifyAPI.getInstance().requestLogin(emailAddress, new CozifyAPI.StringCallback() {
                    @Override
                    public void result(boolean success, String message, String result) {
                        if (success) {
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

    private void checkAccess() {
        final Context context = CozifyWidgetSetupActivity.this;
        emailAddress = PersistentStorage.getInstance().loadEmail(context, 0);
        if (emailAddress == null || emailAddress.length() < 5) return;
        cloudtoken = PersistentStorage.getInstance().loadCloudToken(context);
        if (cloudtoken == null || cloudtoken.length() < 5) return;
        setAuthHeader();
        getHubKeys();
    }
    public void controlTarget(View w) {
        return;
    }
}
