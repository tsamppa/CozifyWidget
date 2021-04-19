package com.cozify.cozifywidget;

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

    private CozifyApiReal cozifyAPI = new CozifyApiReal(this);

    TextView textViewStatus;
    EditText editTextEmail;
    TextInputLayout textInputLayoutEmail;
    EditText editTextPw;
    TextInputLayout textInputLayoutPw;
    Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget);
        editTextEmail = findViewById(R.id.email_edit);
        if (cozifyAPI.getEmail() != null) {
            editTextEmail.setText(cozifyAPI.getEmail());
        }
        textViewStatus = findViewById(R.id.login_status);
        buttonLogin = findViewById(R.id.login_button);
        textInputLayoutEmail = findViewById(R.id.email_layout);

        // Find the Password  EditText
        editTextPw = findViewById(R.id.appwidget_pw);
        textInputLayoutPw = findViewById(R.id.password);
        textInputLayoutPw.setEnabled(false);

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

        editTextPw.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int str_len = s.length();
                if (str_len == 6) {
                    textInputLayoutPw.setErrorEnabled(false);
                    confirmPassword(editTextPw.getText().toString());
                } else {
                    textInputLayoutPw.setErrorEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void promptForEmail() {
        textInputLayoutEmail.setEnabled(true);
        textInputLayoutEmail.requestFocus();
        textInputLayoutEmail.setErrorEnabled(true);
        textInputLayoutEmail.setError("Please enter login email");
        textViewStatus.setText("Please enter login email");
        buttonLogin.setEnabled(true);
    }

    private void promptForPassword() {
        textInputLayoutEmail.setEnabled(false);
        editTextPw.setVisibility(View.VISIBLE);
        textInputLayoutPw.setVisibility(View.VISIBLE);
        textInputLayoutPw.setEnabled(true);
        textInputLayoutPw.requestFocus();
        editTextPw.setSelection(0);
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

    private void confirmPassword(String pw) {
        cozifyAPI.confirmPassword(pw, cozifyAPI.getEmail(), new CozifyApiReal.StringCallback() {
            @Override
            public void result(boolean success, String message, String result) {
                if (success) {
                    cozifyAPI.setCloudToken(result);
                    getHubKeys();
                } else {
                    textViewStatus.setText(message);
                    promptForEmail();
                }
            }
        });
    }

    private void getHubKeys() {
        textViewStatus.setText("Checking connection..");
        buttonLogin.setEnabled(false);
        textInputLayoutEmail.setEnabled(false);
        cozifyAPI.getHubKeys(new CozifyApiReal.JsonCallback() {
            @Override
            public void result(boolean success, String message, JSONObject jsonResult) {
                if (success) {
                    final Context context = CozifyWidgetSetupActivity.this;
                    Toast.makeText(context, "Connected to Cozify. You can now create widgets.", Toast.LENGTH_SHORT).show();
                    Intent myIntent = new Intent(CozifyWidgetSetupActivity.this,
                            CozifyAppWidgetConnectedActivity.class);
                    startActivity(myIntent);
                    finish();
                } else {
                    textViewStatus.setText(message);
                    buttonLogin.setEnabled(true);
                    enableEmailLogin();
                }
            }
        });
    }

    private void enableEmailLogin() {
        listHubs();
        promptForEmail();
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
                cozifyAPI.setEmail(email_address);
                cozifyAPI.requestLogin(email_address, new CozifyApiReal.StringCallback() {
                    @Override
                    public void result(boolean success, String message, String result) {
                        if (success) {
                            promptForPassword();
                            textViewStatus.setText("Check your email for the temporary password.");
                        } else {
                            promptForEmail();
                            textViewStatus.setText(message + ": " + result);
                        }
                    }
                });
            } else {
                promptForEmail();
            }
        }
    };

    private boolean checkAccess() {
        cozifyAPI.loadCloudSettings();
        if (cozifyAPI.getEmail() == null) {
            return false;
        }
        editTextEmail.setText(cozifyAPI.getEmail());
        if (cozifyAPI.getCloudToken() == null) {
            return false;
        }
        getHubKeys();
        return true;
    }

}
