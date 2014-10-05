package com.dagwaging.rosewidgets.netreg.activities;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;

import com.dagwaging.rosewidgets.netreg.fragments.ConfigurationFragment;

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
