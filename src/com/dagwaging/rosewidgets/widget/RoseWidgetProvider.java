package com.dagwaging.rosewidgets.widget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import com.dagwaging.rosewidgets.R;

public class RoseWidgetProvider extends AppWidgetProvider {
	public static final String MIN_WIDTH = "min_width";

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
		PreferenceManager.getDefaultSharedPreferences(context).edit().clear()
				.commit();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onAppWidgetOptionsChanged(Context context,
			AppWidgetManager appWidgetManager, int appWidgetId,
			Bundle newOptions) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			return;
		}

		int minWidth = newOptions
				.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
		// int minHeight = newOptions
		// .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.putInt(MIN_WIDTH, minWidth).commit();

		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.widget);

		adjustWidgetSize(views, minWidth);

		appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
	}

	public static void adjustWidgetSize(RemoteViews views, int minWidth) {
		boolean showReceivedLabel = minWidth > 72;
		boolean showBandwidth = minWidth > 216;
		boolean showSent = minWidth > 288;

		views.setViewVisibility(R.id.bandwidth, showBandwidth ? View.VISIBLE
				: View.GONE);
		views.setViewVisibility(R.id.summaryPolicyMegabytesReceived,
				showReceivedLabel ? View.VISIBLE : View.GONE);
		views.setViewVisibility(R.id.summaryPolicyReceivedIcon,
				showReceivedLabel ? View.GONE : View.VISIBLE);
		views.setViewVisibility(R.id.summaryPolicySent, showSent ? View.VISIBLE
				: View.GONE);
	}
}
