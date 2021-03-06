package com.philipplauer.unichat;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.philipplauer.unichat.xmpp.RoosterConnectionService;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jxmpp.jid.parts.Localpart;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity{
    private static final String LOGTAG = "RoosterPlus";
    private AutoCompleteTextView mJidView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private BroadcastReceiver mBroadcastReceiver;
    private String serverurl = "saar-force.de";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mJidView = findViewById(R.id.jid);
        mPasswordView = findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        ImageView mJidSignInButton = findViewById(R.id.jid_sign_in_button);
        mJidSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        ImageView mJidRegisterButton = findViewById(R.id.jid_register_button);
        // Registrierungsdialog
        mJidRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog = new Dialog(LoginActivity.this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(false);
                dialog.setContentView(R.layout.register_dialog);
                ImageView rJidReg = dialog.findViewById(R.id.login_register_reg);
                ImageView rJidCancel = dialog.findViewById(R.id.login_register_cancel);
                rJidReg.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EditText username = dialog.findViewById(R.id.dialog_register_jid);
                        EditText password = dialog.findViewById(R.id.dialog_register_password);
                        try {
                            attemptRegister(username.getText().toString(), password.getText().toString());
                            dialog.dismiss();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                rJidCancel.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });
    }
    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(mBroadcastReceiver);
    }
    @Override
    protected void onResume() {
        super.onResume();
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action)
                {
                    case Constants.BroadCastMessages.UI_AUTHENTICATED:
                        Log.d(LOGTAG,"Mainscreen wird geöffnet");
                        showProgress(false);
                        Intent i = new Intent(getApplicationContext(),ChatListActivity.class);
                        startActivity(i);
                        finish();
                        break;
                    case Constants.BroadCastMessages.UI_CONNECTION_ERROR:
                        Log.d(LOGTAG,"Connection Error");
                        showProgress(false);
                        mJidView.setError("Probleme beim Login. Bitte überprüfe deine Daten und versuche es erneut.");
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.BroadCastMessages.UI_AUTHENTICATED);
        filter.addAction(Constants.BroadCastMessages.UI_CONNECTION_ERROR);
        this.registerReceiver(mBroadcastReceiver, filter);
    }
    private void attemptLogin() {
        // Errors zurücksetzen
        mJidView.setError(null);
        mPasswordView.setError(null);
        String jid = mJidView.getText().toString();
        String password = mPasswordView.getText().toString();
        boolean cancel = false;
        View focusView = null;
        // Passwortüberprüfung (hier nur noch Länge, nicht ob auf Server richtig
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }
        // JID überprüfen (per @)
        if (TextUtils.isEmpty(jid)) {
            mJidView.setError(getString(R.string.error_field_required));
            focusView = mJidView;
            cancel = true;
        } else if (!isJidValid(jid)) {
            mJidView.setError(getString(R.string.error_invalid_jid));
            focusView = mJidView;
            cancel = true;
        }
        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);
            saveCredentialsAndLogin();
        }
    }
    private boolean isJidValid(String email) {
        return email.contains("@");
    }
    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }
    // Progressbar zeigen und Login ausblenden
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    private void saveCredentialsAndLogin()
    {
        Log.d(LOGTAG,"saveCredentialsAndLogin() called.");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit()
                .putString("xmpp_jid", mJidView.getText().toString())
                .putString("xmpp_password", mPasswordView.getText().toString())
                .commit();
        Intent i1 = new Intent(this, RoosterConnectionService.class);
        startService(i1);
    }
    private void saveCredentialsAndLoginR(String username, String password)
    {
        Log.d(LOGTAG,"saveCredentialsAndLogin() called.");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit()
                .putString("xmpp_jid", username + "@saar-force.de")
                .putString("xmpp_password", password)
                .commit();
        Intent i1 = new Intent(this, RoosterConnectionService.class);
        startService(i1);
    }

    // Registrierung auf XMPP Server - Verbindung extra ohne Benutzerdaten
    private void attemptRegister(final String username, final String password) throws IOException{
        XMPPTCPConnectionConfiguration conf = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain(serverurl)
                .setHost(serverurl)
                .setResource("saar-force.de")
                .setKeystoreType(null)
                .setSendPresence(true)
                .setDebuggerEnabled(true)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setCompressionEnabled(true).build();
        SmackConfiguration.DEBUG = true;
        XMPPTCPConnection.setUseStreamManagementDefault(true);
        final AbstractXMPPConnection mConnection = new XMPPTCPConnection(conf);
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(true) {
                        try {
                            mConnection.connect();
                            AccountManager accountManager = AccountManager.getInstance(mConnection);
                            accountManager.sensitiveOperationOverInsecureConnection(true);
                            accountManager.createAccount(Localpart.from(username), password);
                            saveCredentialsAndLoginR(username, password);
                        } catch (SmackException | IOException | XMPPException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

}