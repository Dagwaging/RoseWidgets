package com.dagwaging.rosewidgets.fragments;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.dagwaging.rosewidgets.R;
import com.dagwaging.rosewidgets.widget.RoseWidgetProvider;
import com.dagwaging.rosewidgets.widget.UpdateService;

public class ConfigurationFragment extends DialogFragment implements
		OnClickListener, android.view.View.OnClickListener, TextWatcher, OnEditorActionListener {
	protected static final String TAG = "com.dagwaging.rosewidgets.fragments.ConfigurationFragment";

	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	private EditText usernameField;

	private EditText passwordField;

	private BroadcastReceiver receiver;

	private ProgressDialog progressDialog;

	private Button okay;

	private Timer progressDialogTimer;

	public static String PREF_USERNAME = "username";

	public static String PREF_PASSWORD = "password";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();

		if (args != null) {
			appWidgetId = args.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		if (!PreferenceManager.getDefaultSharedPreferences(getActivity())
				.getString(PREF_USERNAME, "").isEmpty()) {
			finishWidget();
		}

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if (progressDialogTimer != null) {
					progressDialogTimer.cancel();
				}

				if (progressDialog != null) {
					progressDialog.dismiss();
				}

				if (UpdateService.ACTION_LOAD_SUCCESS.equals(action)) {
					finishWidget();
				} else if (UpdateService.ACTION_LOAD_FAILURE.equals(action)) {
					String message = intent
							.getStringExtra(UpdateService.EXTRA_LOAD_ERROR);

					Builder builder = new Builder(context);
					builder.setTitle(R.string.title_error);
					builder.setMessage(message == null ? "Unknown error"
							: message);
					builder.setPositiveButton(android.R.string.ok, null);
					builder.show();
				}
			}
		};

		IntentFilter filter = new IntentFilter();
		filter.addAction(UpdateService.ACTION_LOAD_SUCCESS);
		filter.addAction(UpdateService.ACTION_LOAD_FAILURE);

		getActivity().registerReceiver(receiver, filter);
	}

	@SuppressLint("InflateParams")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		View view = getActivity().getLayoutInflater().inflate(
				R.layout.configuration, null);

		usernameField = (EditText) view.findViewById(R.id.username);
		passwordField = (EditText) view.findViewById(R.id.password);

		usernameField.addTextChangedListener(this);
		passwordField.addTextChangedListener(this);
		
		passwordField.setOnEditorActionListener(this);

		Builder builder = new Builder(getActivity());
		builder.setTitle(R.string.app_name);
		builder.setView(view);
		builder.setNegativeButton(android.R.string.cancel, this);
		builder.setPositiveButton(android.R.string.ok, null);

		return builder.create();
	}

	@Override
	public void onStart() {
		super.onStart();

		final AlertDialog dialog = (AlertDialog) getDialog();

		if (dialog != null) {
			okay = dialog.getButton(Dialog.BUTTON_POSITIVE);
			okay.setOnClickListener(this);
			okay.setEnabled(false);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		getActivity().unregisterReceiver(receiver);
	}

	private void save() {
		String username = usernameField.getText().toString();
		String password = passwordField.getText().toString();

		PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
				.putString(PREF_USERNAME, username)
				.putString(PREF_PASSWORD, password).commit();

		progressDialogTimer = new Timer();
		progressDialogTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						progressDialog = new ProgressDialog(getActivity());
						progressDialog.setIndeterminate(true);
						progressDialog.setMessage(getString(R.string.authing));
						progressDialog.show();
					}
				});
			}
		}, 500);

		RoseWidgetProvider.update(getActivity(), new int[] { appWidgetId });
	}

	private void finishWidget() {
		Intent result = new Intent();
		result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

		getActivity().setResult(Activity.RESULT_OK, result);
		getActivity().finish();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_NEGATIVE) {
			onCancel(dialog);
		}
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		getActivity().finish();
	}

	@Override
	public void onClick(View v) {
		if (v == okay) {
			save();
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	@Override
	public void afterTextChanged(Editable s) {
		okay.setEnabled(usernameField.getText().length() > 0
				&& passwordField.getText().length() > 0);
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if(actionId == EditorInfo.IME_ACTION_DONE) {
			save();
			
			return true;
		}
		
		return false;
	}
}
