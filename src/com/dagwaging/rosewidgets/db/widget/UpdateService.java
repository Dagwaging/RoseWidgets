package com.dagwaging.rosewidgets.db.widget;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.dagwaging.rosewidgets.R;
import com.dagwaging.rosewidgets.db.data.Balance;
import com.dagwaging.rosewidgets.db.fragments.ConfigurationFragment;

public class UpdateService extends IntentService {
	public static String TAG = "com.dagwaging.rosewidgets.db.widget.UpdateService";

	public static String ACTION_LOAD_SUCCESS = "com.dagwaging.rosewidgets.ACTION_LOAD_SUCCESS";

	public static String ACTION_LOAD_FAILURE = "com.dagwaging.rosewidgets.ACTION_LOAD_FAILURE";

	public static final String PREF_NOTIFIED = "notified";

	public static final String EXTRA_LOAD_ERROR = "com.dagwaging.rosewidgets.EXTRA_LOAD_ERROR";

	private static final String CAMPUSDISH_LOGIN_URL = "https://www.campusdish.com/en-US/CSMW/RoseHulman/login.aspx";

	private static final String CAMPUSDISH_MAIN_URL = "http://www.campusdish.com/en-US/CSMW/RoseHulman/";

	public UpdateService() {
		super("UpdateService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();

		if (extras == null) {
			return;
		}

		int[] appWidgetIds = extras
				.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);

		if (appWidgetIds == null) {
			return;
		}

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		String username = prefs.getString(ConfigurationFragment.PREF_USERNAME,
				null);
		String password = prefs.getString(ConfigurationFragment.PREF_PASSWORD,
				null);

		if (username == null || password == null) {
			return;
		}

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

		reset(appWidgetManager, appWidgetIds);

		Log.d(TAG, "Loading...");

		Intent finished = new Intent();

		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

		if (networkInfo == null || !networkInfo.isConnected()) {
			update(appWidgetManager, appWidgetIds,
					getString(R.string.no_network));

			finished.setAction(ACTION_LOAD_FAILURE);
			finished.putExtra(EXTRA_LOAD_ERROR, getString(R.string.no_network));
		}

		try {
			List<Balance> data = getBalanceData(username, password);

			if (data == null) {
				throw new IOException("Bad response");
			}

			boolean notified = prefs.getBoolean(PREF_NOTIFIED, false);

			Calendar date = new GregorianCalendar();

			if (date.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.WEDNESDAY
					&& date.get(GregorianCalendar.HOUR_OF_DAY) >= 8) {
				if (!notified) {
					String db = null;

					for (Balance balance : data) {
						if ("Declining Balance:".equals(balance.label)) {
							db = balance.value;
							break;
						}
					}

					if (db != null) {
						Notification notification = new Notification.Builder(
								this)
								.setContentTitle("You have " + db + " to spend")
								.setDefaults(Notification.DEFAULT_ALL)
								.setSmallIcon(R.drawable.ic_alert_cash)
								.getNotification();

						NotificationManager notificationManager = (NotificationManager) getSystemService(Activity.NOTIFICATION_SERVICE);
						notificationManager.notify(0, notification);

						prefs.edit().putBoolean(PREF_NOTIFIED, true).apply();
					}
				}
			} else if (notified) {
				prefs.edit().putBoolean(PREF_NOTIFIED, false).apply();
			}

			update(appWidgetManager, appWidgetIds, data);

			finished.setAction(ACTION_LOAD_SUCCESS);
		} catch (IOException e) {
			e.printStackTrace();

			String message = e.getMessage();

			update(appWidgetManager, appWidgetIds, message);

			finished.setAction(ACTION_LOAD_FAILURE);
			finished.putExtra(EXTRA_LOAD_ERROR, message);
		}

		Log.d(TAG, "Load complete");

		sendBroadcast(finished);
	}

	private void reset(AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		RemoteViews views = new RemoteViews(getPackageName(),
				R.layout.widget_db);

		views.setViewVisibility(R.id.loading, View.VISIBLE);
		views.setTextViewText(R.id.loading, getText(R.string.loading));

		views.removeAllViews(R.id.balances);

		appWidgetManager.updateAppWidget(appWidgetIds, views);
	}

	private void update(AppWidgetManager appWidgetManager, int[] appWidgetIds,
			List<Balance> data) {
		RemoteViews views = new RemoteViews(getPackageName(),
				R.layout.widget_db);

		Intent update = new Intent(this, UpdateService.class);
		update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

		PendingIntent pendingIntent = PendingIntent.getService(this, 0, update,
				PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.widget, pendingIntent);

		views.setViewVisibility(R.id.loading, View.GONE);

		for (Balance balance : data) {
			RemoteViews balanceView = new RemoteViews(getPackageName(),
					R.layout.widget_db_balance);

			balanceView.setTextViewText(R.id.balanceLabel, balance.label);
			balanceView.setTextViewText(R.id.balanceValue, balance.value);

			views.addView(R.id.balances, balanceView);
		}

		appWidgetManager.updateAppWidget(appWidgetIds, views);
	}

	private void update(AppWidgetManager appWidgetManager, int[] appWidgetIds,
			String error) {
		RemoteViews views = new RemoteViews(getPackageName(),
				R.layout.widget_db);

		Intent update = new Intent(this, UpdateService.class);
		update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

		PendingIntent pendingIntent = PendingIntent.getService(this, 0, update,
				PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.widget, pendingIntent);

		if (error == null) {
			error = "Unknown error";
		}

		views.setTextViewText(R.id.loading, error);

		appWidgetManager.updateAppWidget(appWidgetIds, views);
	}

	public static List<Balance> getBalanceData(final String username,
			final String password) throws IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();

		HttpResponse response = httpClient.execute(new HttpGet(
				CAMPUSDISH_LOGIN_URL));

		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			throw new IOException(response.getStatusLine().getReasonPhrase());
		}

		HttpEntity entity = response.getEntity();

		if (entity == null) {
			return null;
		}

		String viewState = parseViewState(entity.getContent(),
				CAMPUSDISH_LOGIN_URL);

		List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
		parameters.add(new BasicNameValuePair("__VIEWSTATE", viewState));
		parameters.add(new BasicNameValuePair("loginFormCtrl:Email", username));
		parameters.add(new BasicNameValuePair("loginFormCtrl:Password",
				password));
		parameters.add(new BasicNameValuePair("loginFormCtrl:LoginCommand.x",
				"0"));
		parameters.add(new BasicNameValuePair("loginFormCtrl:LoginCommand.y",
				"0"));

		HttpPost request = new HttpPost(CAMPUSDISH_LOGIN_URL);
		request.setEntity(new UrlEncodedFormEntity(parameters));
		request.setHeader("Referer", CAMPUSDISH_LOGIN_URL);

		response = httpClient.execute(request);

		entity = response.getEntity();

		if (entity != null) {
			return parseDbData(entity.getContent(), CAMPUSDISH_MAIN_URL);
		}

		return null;
	}

	private static List<Balance> parseDbData(InputStream stream, String url)
			throws IOException {
		Document document = Jsoup.parse(stream, "UTF-8", url);
		Elements data = document.getElementsByClass("EPSAccount");

		if (data.size() == 0) {
			return null;
		}

		List<Balance> balances = new ArrayList<Balance>();

		for (int i = 1; i < data.size(); i += 2) {
			Element label = data.get(i);
			Element value = data.get(i + 1);

			if (label == null || value == null) {
				break;
			}

			Balance balance = new Balance();
			balance.label = label.text();
			balance.value = value.text();

			balances.add(balance);
		}

		return balances;
	}

	private static String parseViewState(InputStream stream, String url)
			throws IOException {
		Document document = Jsoup.parse(stream, "UTF-8", url);
		Element viewState = document.getElementsByAttributeValue("name",
				"__VIEWSTATE").get(0);

		if (viewState == null)
			return "";

		return viewState.attr("value");
	}
}
