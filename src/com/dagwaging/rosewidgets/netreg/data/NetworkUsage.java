package com.dagwaging.rosewidgets.netreg.data;

import java.util.List;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class NetworkUsage {
	private static final String SUMMARY_BANDWIDTH_CLASS = "summaryBandwidthClass";

	private static final String SUMMARY_POLICY_MEGABYTES_RECEIVED = "summaryPolicyMegabytesReceived";

	private static final String SUMMARY_POLICY_MEGABYTES_SENT = "summaryPolicyMegabytesSent";

	private static final String SUMMARY_ACTUAL_MEGABYTES_RECEIVED = "summaryActualMegabytesReceived";

	private static final String SUMMARY_ACTUAL_MEGABYTES_SENT = "summaryActualMegabytesSent";

	public NetworkUsageSummary summary;

	public List<NetworkUsageDetails> details;

	public static NetworkUsage load(SharedPreferences prefs) {
		NetworkUsage usage = new NetworkUsage();
		usage.summary = new NetworkUsageSummary();

		usage.summary.bandwidthClass = prefs.getString(SUMMARY_BANDWIDTH_CLASS,
				null);
		usage.summary.policyMegabytesReceived = prefs.getFloat(
				SUMMARY_POLICY_MEGABYTES_RECEIVED, 0);
		usage.summary.policyMegabytesSent = prefs.getFloat(
				SUMMARY_POLICY_MEGABYTES_SENT, 0);
		usage.summary.actualMegabytesReceived = prefs.getFloat(
				SUMMARY_ACTUAL_MEGABYTES_RECEIVED, 0);
		usage.summary.actualMegabytesSent = prefs.getFloat(
				SUMMARY_ACTUAL_MEGABYTES_SENT, 0);

		return usage.summary.bandwidthClass != null ? usage : null;
	}

	public void save(SharedPreferences prefs) {
		Editor editor = prefs.edit();

		editor.putString(SUMMARY_BANDWIDTH_CLASS, summary.bandwidthClass);
		editor.putFloat(SUMMARY_POLICY_MEGABYTES_RECEIVED,
				summary.policyMegabytesReceived);
		editor.putFloat(SUMMARY_POLICY_MEGABYTES_SENT,
				summary.policyMegabytesSent);
		editor.putFloat(SUMMARY_ACTUAL_MEGABYTES_RECEIVED,
				summary.actualMegabytesReceived);
		editor.putFloat(SUMMARY_ACTUAL_MEGABYTES_SENT,
				summary.actualMegabytesSent);

		editor.commit();
	}

}
