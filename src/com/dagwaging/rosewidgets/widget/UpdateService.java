package com.dagwaging.rosewidgets.widget;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.dagwaging.rosewidgets.R;
import com.dagwaging.rosewidgets.data.NetworkUsage;
import com.dagwaging.rosewidgets.data.NetworkUsageDetails;
import com.dagwaging.rosewidgets.data.NetworkUsageSummary;
import com.dagwaging.rosewidgets.fragments.ConfigurationFragment;
import com.dagwaging.rosewidgets.ntlm.NTLMSchemeFactory;

public class UpdateService extends IntentService {
	public static String TAG = "com.dagwaging.rosewidgets.widget.UpdateService";

	public static String ACTION_LOAD_SUCCESS = "com.dagwaging.rosewidgets.ACTION_LOAD_SUCCESS";

	public static String ACTION_LOAD_FAILURE = "com.dagwaging.rosewidgets.ACTION_LOAD_FAILURE";

	public static final String EXTRA_LOAD_ERROR = "com.dagwaging.rosewidgets.EXTRA_LOAD_ERROR";

	private static final String NETREG_URL = "http://netreg.rose-hulman.edu/tools/networkUsage.pl";

	// private static final int LOWER_THROTTLE_LIMIT = 8192;

	private static final int UPPER_THROTTLE_LIMIT = 9216;

	private static final Pattern megabytesPattern = Pattern.compile("[0-9,.]*");

	private static final String UNRESTRICTED = "Unrestricted";

	private static final String RESTRICTED_1024 = "1024k";

	private static final String RESTRICTED_256 = "256k";

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
			NetworkUsage data = getNetRegData(username, password);

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
		RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);

		int minWidth = PreferenceManager.getDefaultSharedPreferences(this)
				.getInt(RoseWidgetProvider.MIN_WIDTH, 0);

		RoseWidgetProvider.adjustWidgetSize(views, minWidth);

		views.setTextViewText(R.id.bandwidthClass, getString(R.string.loading));
		views.setImageViewResource(R.id.bandwidthClassIcon,
				R.drawable.ic_action_navigation_refresh);

		views.setTextViewText(R.id.summaryPolicyMegabytesReceived,
				getString(R.string.loading));
		views.setProgressBar(R.id.summaryPolicyMegabytesReceivedProgress, 0, 0,
				true);

		views.setTextViewText(R.id.summaryPolicyMegabytesSent,
				getString(R.string.loading));
		views.setProgressBar(R.id.summaryPolicyMegabytesSentProgress, 0, 0,
				true);

		appWidgetManager.updateAppWidget(appWidgetIds, views);
	}

	private void update(AppWidgetManager appWidgetManager, int[] appWidgetIds,
			NetworkUsage data) {
		RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);

		int minWidth = PreferenceManager.getDefaultSharedPreferences(this)
				.getInt(RoseWidgetProvider.MIN_WIDTH, 0);

		RoseWidgetProvider.adjustWidgetSize(views, minWidth);

		Intent update = new Intent(this, UpdateService.class);
		update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

		PendingIntent pendingIntent = PendingIntent.getService(this, 0, update,
				PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.widget, pendingIntent);

		views.setTextViewText(R.id.bandwidthClass, data.summary.bandwidthClass);

		int icon = R.drawable.ic_action_navigation_refresh;

		if (UNRESTRICTED.equals(data.summary.bandwidthClass)) {
			icon = R.drawable.ic_action_navigation_accept;
		} else if (RESTRICTED_1024.equals(data.summary.bandwidthClass)) {
			icon = R.drawable.ic_action_alerts_and_states_warning;
		} else if (RESTRICTED_256.equals(data.summary.bandwidthClass)) {
			icon = R.drawable.ic_action_alerts_and_states_error;
		}

		views.setImageViewResource(R.id.bandwidthClassIcon, icon);

		views.setTextViewText(
				R.id.summaryPolicyMegabytesReceived,
				getString(R.string.megabytes,
						data.summary.policyMegabytesReceived));
		views.setProgressBar(R.id.summaryPolicyMegabytesReceivedProgress,
				UPPER_THROTTLE_LIMIT,
				data.summary.policyMegabytesReceived.intValue(), false);

		views.setTextViewText(R.id.summaryPolicyMegabytesSent,
				getString(R.string.megabytes, data.summary.policyMegabytesSent));
		views.setProgressBar(R.id.summaryPolicyMegabytesSentProgress,
				UPPER_THROTTLE_LIMIT,
				data.summary.policyMegabytesSent.intValue(), false);

		appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views);
	}

	private void update(AppWidgetManager appWidgetManager, int[] appWidgetIds,
			String error) {
		RemoteViews views = new RemoteViews(getPackageName(),
				R.layout.widget_error);

		Intent update = new Intent(this, UpdateService.class);
		update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

		PendingIntent pendingIntent = PendingIntent.getService(this, 0, update,
				PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.error, pendingIntent);

		if (error == null) {
			error = "Unknown error";
		}

		views.setTextViewText(R.id.error, error);

		appWidgetManager.updateAppWidget(appWidgetIds, views);
	}

	public static NetworkUsage getNetRegData(final String username,
			final String password) throws ParseException,
			ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.getAuthSchemes().register("ntlm", new NTLMSchemeFactory());

		httpclient.getCredentialsProvider().setCredentials(
				new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
				new NTCredentials(username, password, "", ""));

		HttpResponse response = httpclient.execute(new HttpGet(NETREG_URL));

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new IOException(response.getStatusLine().getReasonPhrase());
		}

		HttpEntity entity = response.getEntity();

		if (entity == null) {
			return null;
		}

		return parseNetRegData(entity.getContent());
	}

	private static NetworkUsage parseNetRegData(InputStream stream)
			throws IOException {
		NetworkUsage result = new NetworkUsage();

		Document document = Jsoup.parse(stream, "UTF-8", NETREG_URL);
		Elements tables = document
				.select(".mainContainer .ms-rteTable-1 tbody");

		Element summaryTable = tables.get(0);
		Element summaryRow = summaryTable.select(
				".ms-rteTableOddRow-1, .ms-rteTableEvenRow-1").get(0);

		result.summary = new NetworkUsageSummary();
		result.summary.bandwidthClass = summaryRow.child(0).text();
		result.summary.policyMegabytesReceived = parseMegabytes(summaryRow
				.child(1).text());
		result.summary.policyMegabytesSent = parseMegabytes(summaryRow.child(2)
				.text());
		result.summary.actualMegabytesReceived = parseMegabytes(summaryRow
				.child(3).text());
		result.summary.actualMegabytesSent = parseMegabytes(summaryRow.child(4)
				.text());

		Element detailsTable = tables.get(1);
		Elements detailRows = detailsTable
				.select(".ms-rteTableOddRow-1, .ms-rteTableEvenRow-1");

		result.details = new ArrayList<NetworkUsageDetails>();

		for (Element detailRow : detailRows) {
			NetworkUsageDetails networkUsageDetails = new NetworkUsageDetails();
			networkUsageDetails.networkAddress = detailRow.child(0).text();
			networkUsageDetails.comment = detailRow.child(2).text();
			networkUsageDetails.policyMegabytesReceived = parseMegabytes(detailRow
					.child(3).text());
			networkUsageDetails.policyMegabytesSent = parseMegabytes(detailRow
					.child(4).text());
			networkUsageDetails.actualMegabytesReceived = parseMegabytes(detailRow
					.child(5).text());
			networkUsageDetails.actualMegabytesSent = parseMegabytes(detailRow
					.child(6).text());

			result.details.add(networkUsageDetails);
		}

		return result;
	}

	private static Float parseMegabytes(String megabytes) {
		Matcher matcher = megabytesPattern.matcher(megabytes);

		if (matcher.find()) {
			megabytes = matcher.group().replaceAll(",", "");

			try {
				return Float.parseFloat(megabytes);
			} catch (NumberFormatException e) {
			}
		}

		return null;
	}
}
