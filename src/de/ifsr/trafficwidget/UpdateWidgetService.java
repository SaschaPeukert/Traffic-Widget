/* 
 * @version: 1.1 March 2014
 * @autor: Sascha Peukert
 */

package de.ifsr.trafficwidget;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.widget.RemoteViews;

public class UpdateWidgetService extends Service {
	private static final String LOG = "de.ifsr.trafficwidget";

	private static final String WU = "https://www.wh2.tu-dresden.de/traffic/getMyTraffic.php";
	private static final String HSS = "http://wh12.tu-dresden.de/tom.addon2.php";
	private static final String ZEU = "http://zeus.wh25.tu-dresden.de/traffic.php";
	private static final String BOR = "http://wh10.tu-dresden.de/phpskripte/getMyTraffic.php";
	private static final String GER = "http://www.wh17.tu-dresden.de/traffic/prozent";

	private static final String[] Student_Halls = { WU, HSS, ZEU, BOR, GER };

	private static String fav_Hall = "";

	/**
	 * @param urlToRead
	 *            URL of Traffic-Server
	 * @return Result of HTTP-GET-Request from Traffic-Server (either an Error
	 *         [-1 or some text] or the correct Value (between 0 and 100)
	 */
	public String getHTML(String urlToRead) {
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String result = "";

		try {
			url = new URL(urlToRead);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			rd = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));

			while ((line = rd.readLine()) != null) {
				result += line;
			}

			rd.close();
			conn.disconnect();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStart(android.content.Intent, int)
	 */
	@Override
	public void onStart(Intent intent, int startId) {

		// Otherwise NetworkOnMainthreadExeptions will be thrown
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// Update all Widgets
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
				.getApplicationContext());

		int[] allWidgetIds = intent
				.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

		for (int widgetId : allWidgetIds) {

			RemoteViews remoteViews = new RemoteViews(this
					.getApplicationContext().getPackageName(),
					R.layout.widget_layout);

			float traffic = -1;
			boolean not_fav = false;
			String result;

			if (!fav_Hall.equals("")) {
				// Previous saved favorite Network (fav)

				result = getHTML(fav_Hall);
				Log.i(LOG, "Result " + result);

				traffic = calculateTraffic(result, fav_Hall);

				if (traffic != -1) {
					result = String.valueOf(traffic);
					remoteViews.setTextViewText(R.id.update, "Traffic:\n"
							+ result + "%");
				} else {
					// Not in a Fav-Network
					not_fav = true;
				}

			}

			if ((fav_Hall.equals("")) || (not_fav)) {
				// No Previous saved favorite Network (fav) OR not in
				// Fav-Network
				for (String hall : Student_Halls) {

					result = getHTML(hall);
					Log.i(LOG, "Result " + result);

					traffic = calculateTraffic(result, hall);

					Log.i(LOG, "Traffic:" + traffic);

					if (traffic != -1) {
						result = String.valueOf(traffic);
						remoteViews.setTextViewText(R.id.update, "Traffic:\n"
								+ result + "%");
						break;
					} else {
						remoteViews.setTextViewText(R.id.update, "No Network!");
						fav_Hall = "";
					}
				}

			}

			// Register an onClickListener
			Intent clickIntent = new Intent(this.getApplicationContext(),
					MyWidgetProvider.class);

			clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
					allWidgetIds);

			PendingIntent pendingIntent = PendingIntent.getBroadcast(
					getApplicationContext(), 0, clickIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
			appWidgetManager.updateAppWidget(widgetId, remoteViews);
		}
		stopSelf();
		Log.i(LOG, "successfully stoped");
	}

	/**
	 * @param str_traffic
	 *            String-Representation of Traffic
	 * @param hall
	 *            Student Hall
	 * @return Value of Traffic as Float
	 */
	public float calculateTraffic(String str_traffic, String hall) {
		float traffic;
		try {
			traffic = Float.valueOf(str_traffic);
			traffic = Math.round(traffic * 100);
			traffic = traffic / 100;

			if (traffic != -1) {
				fav_Hall = hall;
			}
		} catch (NumberFormatException e) {
			traffic = -1;
			Log.i(LOG, "NumberFormatException for " + hall);
		}
		return traffic;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
