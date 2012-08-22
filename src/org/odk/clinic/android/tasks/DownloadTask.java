package org.odk.clinic.android.tasks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.odk.clinic.android.database.ClinicAdapter;
import org.odk.clinic.android.listeners.DownloadListener;
import org.odk.clinic.android.utilities.Constants;

import android.os.AsyncTask;

public abstract class DownloadTask extends
		AsyncTask<String, String, String> {

	protected DownloadListener mStateListener;
	protected ClinicAdapter mPatientDbAdapter = new ClinicAdapter();
	
	protected String error;

	// url, username, password, savedSearch, cohort, program
	protected DataInputStream connectToServer(String url, String username, String password, boolean savedSearch, int cohort, int program) throws Exception {

		// compose url
		URL u = new URL(url);

		// setup http url connection
		HttpURLConnection c = (HttpURLConnection) u.openConnection();
		c.setDoOutput(true);
		c.setRequestMethod("POST");
		c.setConnectTimeout(Constants.CONNECTION_TIMEOUT);
		c.setReadTimeout(Constants.CONNECTION_TIMEOUT);
		c.addRequestProperty("Content-type", "application/octet-stream");
		
		// write auth details to connection
		DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(c.getOutputStream()));
		dos.writeUTF(username); // username
		dos.writeUTF(password); // password
		dos.writeBoolean(savedSearch);
		if (cohort > 0)
			dos.writeInt(cohort);
		//if (program > 0)
			dos.writeInt(program);

		dos.flush();
		dos.close();

		// read connection status
		DataInputStream zdis = new DataInputStream(new GZIPInputStream(c.getInputStream()));
		
		int response = c.getResponseCode();
		if (response != HttpURLConnection.HTTP_OK){
			zdis = null;
			error = "Connection failed. Please try again.";
		} else {
			int status = zdis.readInt();
			
			if (status == Constants.STATUS_FAILURE) {
				zdis = null;
				error = "Connection failed. Please try again.";
			} else if (status == Constants.STATUS_ACCESS_DENIED) {
				zdis = null;
				error = "Access denied. Check your username and password.";
			} else {
				assert (status == Constants.STATUS_SUCCESS); // success
			}
		}
		return zdis;
	}
	
	@Override
	protected void onProgressUpdate(String... values) {
		synchronized (this) {
			if (mStateListener != null) {
				// update progress and total
				if (values.length > 1)
					mStateListener.progressUpdate(values[0], Integer.valueOf(values[1]), Integer.valueOf(values[2]));
				else
					mStateListener.progressUpdate(values[0]);
			}
		}
	}

	@Override
	protected void onPostExecute(String result) {
		synchronized (this) {
			if (mStateListener != null) {
				if (error == null)
					error=result;
				mStateListener.taskComplete(error);
			}
		}
	}

	public void setDownloadListener(DownloadListener dl) {
		synchronized (this) {
			mStateListener = dl;
		}
	}
}