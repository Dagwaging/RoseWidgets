package com.dagwaging.rosewidgets.db.activities;

import com.dagwaging.rosewidgets.db.fragments.ConfigurationFragment;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;

public class ConfigurationActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setResult(RESULT_CANCELED);

		DialogFragment dialog = new ConfigurationFragment();
		dialog.setArguments(getIntent().getExtras());
		dialog.show(getFragmentManager(), "dialog");
	}
}
