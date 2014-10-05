package com.dagwaging.rosewidgets.db.widget;

import com.dagwaging.rosewidgets.db.fragments.ConfigurationFragment;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

public class RoseWidgetProvider extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		update(context, appWidgetIds);
	}

	public static void update(Context context, int[] appWidgetIds) {
		Intent update = new Intent(context, UpdateService.class);
		update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		context.startService(update);
	}

	@Override
	public void onDisabled(Context context) {
		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.remove(ConfigurationFragment.PREF_USERNAME)
				.remove(ConfigurationFragment.PREF_PASSWORD).commit();
	}
}
