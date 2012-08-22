package org.odk.clinic.android.tasks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.odk.clinic.android.activities.PreferencesActivity;
import org.odk.clinic.android.listeners.DownloadListener;
import org.odk.clinic.android.openmrs.Observation;
import org.odk.clinic.android.openmrs.Patient;
import org.odk.clinic.android.utilities.App;
import org.odk.clinic.android.utilities.Constants;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

public class SearchPatientTask
		extends AsyncTask<String, String, HashMap<String, Object>> {

	private DownloadListener mSearchPatientListener;
	private HashMap<String, Object> mResults = new HashMap<String, Object>();
	private HttpURLConnection c;

	@Override
	protected HashMap<String, Object> doInBackground(String... values) {

		String name = values[0];
		String identifier = values[1];
		
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(App.getApp());
		String url = sharedPreferences.getString(PreferencesActivity.KEY_SERVER,
				null) + Constants.PATIENT_SEARCH_URL;
		String username = sharedPreferences.getString(PreferencesActivity.KEY_USERNAME, null);
		String password = sharedPreferences.getString(PreferencesActivity.KEY_PASSWORD, null);
		int programId = 1;
		
		try {
			URL u = new URL(url);
	
			// setup http url connection
			c  = (HttpURLConnection) u.openConnection();
			c.setDoOutput(true);
			c.setRequestMethod("POST");
			c.setConnectTimeout(Constants.CONNECTION_TIMEOUT);
			c.setReadTimeout(Constants.CONNECTION_TIMEOUT);
			c.addRequestProperty("Content-type", "application/octet-stream");
			
			// write auth details to connection
			DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(c.getOutputStream()));
			dos.writeUTF(username); // username
			dos.writeUTF(password); // password
			dos.writeInt(programId);
			dos.writeUTF(name);
			dos.writeUTF(identifier);
	
			dos.flush();
			dos.close();
	
			if (isCancelled()) {
				cleanup();
				mResults.clear();
				mResults.put(Constants.TASK_ERROR, "Task cancelled");
				return mResults;
			}
			
			DataInputStream zdis = new DataInputStream(new GZIPInputStream(c.getInputStream()));
	
			int status = zdis.readInt();
			
			if (status == Constants.STATUS_FAILURE) {
				zdis = null;
				mResults.put(Constants.TASK_ERROR, "Connection failed. Please try again.");
				return mResults;
			} else if (status == Constants.STATUS_ACCESS_DENIED) {
				zdis = null;
				mResults.put(Constants.TASK_ERROR, "Access denied. Check your username and password.");
				return mResults;
			} 
			
			if (isCancelled()) {
				cleanup();
				mResults.clear();
				mResults.put(Constants.TASK_ERROR, "Task cancelled");
				return mResults;
			}
	
			if (zdis != null) {
				mResults.put(Constants.TASK_SUCCESS, Constants.TASK_SUCCESS);
				mResults.put(Constants.PATIENTS, readPatients(zdis));
				mResults.put(Constants.OBSERVATIONS, readObservations(zdis));
			}
		} catch (Exception e) {
			e.printStackTrace();
			mResults.put(
					Constants.TASK_ERROR,
					"Kindly move to get a stronger signal to OpenMRS.\n\nIf you have good reception and it still fails, please contact the system administrator.");
			return mResults;
		} finally {
			// shutdown connections
			cleanup();
		}
		return mResults;
	}

	private HashMap<String, Patient> readPatients(DataInputStream zdis) throws Exception {
		int c = zdis.readInt();
		HashMap<String, Patient> lPatients = new HashMap<String, Patient>();
	
		for (int i = 1; i < c + 1; i++) {
			Patient p = new Patient();
			p.setPatientId(zdis.readInt());
			p.setFamilyName(zdis.readUTF());
			p.setMiddleName(zdis.readUTF());
			p.setGivenName(zdis.readUTF());
			p.setGender(zdis.readUTF());
			p.setBirthDate(parseDate(zdis.readUTF()));
			p.setIdentifier(zdis.readUTF());
			
			mResults.put(Integer.toString(p.getPatientId()), 
					p.getName() + "|" + 
					p.getIdentifier() + "|" + 
					p.getPatientId());
			
			//add to list of patients
			lPatients.put(Integer.toString(p.getPatientId()), p);
		}
		return lPatients;
	}
	
	private HashMap<String, List<Observation>> readObservations(DataInputStream zdis) throws Exception {
		int icount = zdis.readInt();
		HashMap<String, List<Observation>> lObservations = new HashMap<String, List<Observation>>();
		
		// for every patient
		for (int i = 1; i < icount + 1; i++) {

			Observation o = new Observation();
			o.setPatientId(zdis.readInt());
			o.setFieldName(zdis.readUTF());

			byte dataType = zdis.readByte();
			if (dataType == Constants.TYPE_STRING) {
				o.setValueText(zdis.readUTF());
			} else if (dataType == Constants.TYPE_INT) {
				o.setValueInt(zdis.readInt());
			} else if (dataType == Constants.TYPE_DOUBLE) {
				o.setValueNumeric(zdis.readDouble());
			} else if (dataType == Constants.TYPE_DATE) {
				o.setValueDate(parseDate(zdis.readUTF()));
			}

			o.setDataType(dataType);
			o.setEncounterDate(parseDate(zdis.readUTF()));					
			String key = Integer.toString(o.getPatientId());
			
			if (!lObservations.containsKey( key ) ) {
	            List<Observation> list = new ArrayList<Observation>();
	            lObservations.put( key, list);
	        }
	        
	        List<Observation> list = (List<Observation>) lObservations.get(key);
	        list.add(o);
		}
		return lObservations;
	}
	
	private static String parseDate(String s) {
		String date = s.split("T")[0];
		SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy");
		SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			return outputFormat.format(inputFormat.parse(date));
		} catch (ParseException e) {
			return "Unknown date";
		}
	}

	private void cleanup() {
		if (c != null) {
			c.disconnect();
			c = null;
		}
	}

	@Override
	protected void onPostExecute(HashMap<String, Object> result) {
		synchronized (this) {
			if (mSearchPatientListener != null) {
				mSearchPatientListener.taskComplete(result);
			}
		}
	}

	@Override
	protected void onProgressUpdate(String... progress) {
		synchronized (this) {
			if (mSearchPatientListener != null) {
				mSearchPatientListener.progressUpdate(progress[0]);
			}
		}
	}

	public void setSearchPatientListener(DownloadListener dl) {
		synchronized (this) {
			mSearchPatientListener = dl;
		}
	}
}