package com.dagwaging.rosewidgets.db.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.dagwaging.rosewidgets.R;
import com.dagwaging.rosewidgets.db.widget.RoseWidgetProvider;

public class ConfigurationFragment extends com.dagwaging.rosewidgets.fragments.ConfigurationFragment {
	public static final String PREF_USERNAME = "campusdishUsername";

	public static final String PREF_PASSWORD = "campusdishPassword";

	@SuppressLint("InflateParams")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		View view = getActivity().getLayoutInflater().inflate(
				R.layout.configuration_db, null);

		usernameField = (EditText) view.findViewById(R.id.username);
		passwordField = (EditText) view.findViewById(R.id.password);

		usernameField.addTextChangedListener(this);
		passwordField.addTextChangedListener(this);
		
		passwordField.setOnEditorActionListener(this);

		Builder builder = new Builder(getActivity());
		builder.setTitle(R.string.widget_name_db);
		builder.setView(view);
		builder.setNegativeButton(android.R.string.cancel, this);
		builder.setPositiveButton(android.R.string.ok, null);

		return builder.create();
	}

	@Override
	protected void onSave(int appWidgetId) {
		RoseWidgetProvider.update(getActivity(), new int[] { appWidgetId });	
	}

	@Override
	protected String getPrefUsername() {
		return PREF_USERNAME;
	}

	@Override
	protected String getPrefPassword() {
		return PREF_PASSWORD;
	}

}
