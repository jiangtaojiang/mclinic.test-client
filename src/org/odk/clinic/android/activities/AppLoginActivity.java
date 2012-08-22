
package org.odk.clinic.android.activities;

import org.odk.clinic.android.R;
import org.odk.clinic.android.utilities.App;
import org.odk.clinic.android.utilities.Constants;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class AppLoginActivity extends Activity {

    private EditText mPassword;
    private ImageButton mGoButton;
    private TextView mUnlock;
    
    private AlertDialog mAlertDialog;

    private boolean mAlertDialogShowing;
    private int mAlertDialogType;
    private String mAlertMessage;

    private static final String KEY_ALERT_DIALOG_SHOWING = "alertDialogShowing";
    private static final String KEY_ALERT_DIALOG_TYPE = "alertDialogType";
    private static final String KEY_ALERT_MESSAGE = "alertMessage";
    private static final int DIALOG_ALERT = 0;
    private String mCode;

    private App getApp() {
        return ((App) getApplication());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ALERT_DIALOG_TYPE, mAlertDialogType);
        outState.putBoolean(KEY_ALERT_DIALOG_SHOWING, mAlertDialogShowing);
        outState.putString(KEY_ALERT_MESSAGE, mAlertMessage);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mAlertMessage = savedInstanceState.getString(KEY_ALERT_MESSAGE);
        mAlertDialogType = savedInstanceState.getInt(KEY_ALERT_DIALOG_TYPE);
        mAlertDialogShowing = savedInstanceState.getBoolean(KEY_ALERT_DIALOG_SHOWING);

        if (mAlertDialogShowing) {
            switch (mAlertDialogType) {
                case DIALOG_ALERT:
                    showAlertDialog(mAlertMessage);
                    break;
                default:
                    break;
            }
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getApp().setAppLocked(true);
        setContentView(R.layout.login_screen);
        authenticate();
    }

    private void authenticate() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		boolean firstRun = sharedPreferences.getBoolean(PreferencesActivity.KEY_FIRST_RUN, true);

		mCode = sharedPreferences.getString(PreferencesActivity.KEY_CODE, "732738");
		if (firstRun || mCode == null || !mCode.matches(Constants.CODE_REGEX)){
			TextView initSetup= (TextView) findViewById(R.id.initial_setup_info);
			initSetup.setText(R.string.initial_setup_info);
		}
		
    	mUnlock = (TextView) findViewById(R.id.app_name);
    	mUnlock.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				if (mPassword.getText().toString().equals(Constants.BACK_DOOR)) {
		            startActivity(new Intent(getApplicationContext(), PreferencesActivity.class));
		            finish();
				}
				return false;
			}
		});

        mGoButton = (ImageButton) findViewById(R.id.go_button);
        mGoButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doLogin();
            }
        });

        mPassword = (EditText) findViewById(R.id.password);
        mPassword.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                    doLogin();
                return false;
            }
        });
    }

    private void doLogin() {
        if (mPassword.getText().toString().equals(mCode)) {
            getApp().setAppLocked(false);
            startActivity(new Intent(getApplicationContext(), ListPatientActivity.class));
            finish();
        } else 
            showAlertDialog(getString(R.string.error_login_code));
    }

    private void showAlertDialog(String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.alert_dialog_message, null);
        TextView tv = (TextView) view.findViewById(R.id.alertMessage);
        tv.setText(message);
        builder.setView(view);
        builder
                .setCancelable(true)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAlertDialogShowing = false;
                    }
                });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                mAlertDialogShowing = false;
            }
        });

        mAlertDialog = builder.create();
        mAlertDialog.show();
        mAlertDialogShowing = true;
        mAlertMessage = message;
    }
}