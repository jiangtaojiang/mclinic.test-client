package org.odk.clinic.android.tasks;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.odk.clinic.android.openmrs.Observation;
import org.odk.clinic.android.openmrs.Patient;
import org.odk.clinic.android.utilities.Constants;
import org.odk.clinic.android.utilities.FileUtils;

public class DownloadPatientTask extends DownloadTask {

	public static final String KEY_ERROR = "error";

	public static final String KEY_PATIENTS = "patients";

	public static final String KEY_OBSERVATIONS = "observations";
	
	private File mFile;
		
	@Override
	protected String doInBackground(String... values) {

		String url = values[0];
		String username = values[1];
		String password = values[2];
		boolean savedSearch = Boolean.valueOf(values[3]);
		int cohort = Integer.valueOf(values[4]);
		int program = Integer.valueOf(values[5]);


		try {
			DataInputStream zdis = connectToServer(url, username, password, savedSearch, cohort, program);
			if (zdis != null) {

                zdis = saveStream(zdis);

				// open db and clean entries
				mPatientDbAdapter.open();
				mPatientDbAdapter.deleteAllPatients();
				mPatientDbAdapter.deleteAllObservations();
				mPatientDbAdapter.deleteAllFormInstances();

				// download and insert patients and obs
				insertPatients(zdis);
				insertObservations(zdis);
				insertPatientForms(zdis);

				// close db and stream
				mPatientDbAdapter.close();
				zdis.close();
				//and delete temporary file
				if (mFile != null)
					FileUtils.deleteFile(mFile.getAbsolutePath());
			}
		} catch (Exception e) {
			//and delete temporary file
			if (mFile != null)
				FileUtils.deleteFile(mFile.getAbsolutePath());
			return e.getLocalizedMessage();
		}

		return null;
	}

    private DataInputStream saveStream(final DataInputStream dataInputStream) throws Exception {
        mFile = File.createTempFile("odk-connector", "-stream");
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(mFile));

        int length = 0;
        byte[] bytes = new byte[4096];
        while ((length = dataInputStream.read(bytes)) > 0)
            bufferedOutputStream.write(bytes, 0, length);
        bufferedOutputStream.close();

        return new DataInputStream(new FileInputStream(mFile));
    }

	private void insertPatientForms(final DataInputStream zdis) throws Exception {

		
		// for every patient
		int icount = zdis.readInt();
		
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
			mPatientDbAdapter.createObservation(o);

			publishProgress("mappings", Integer.valueOf(i).toString(),
					Integer.valueOf(icount).toString());
		}
	}

	private void insertPatients(DataInputStream zdis) throws Exception {
		int c = zdis.readInt();
		System.out.println("inside insertPatients, c=" + c);
		for (int i = 1; i < c + 1; i++) {
			Patient p = new Patient();
			p.setPatientId(zdis.readInt());
			p.setFamilyName(zdis.readUTF());
			p.setMiddleName(zdis.readUTF());
			p.setGivenName(zdis.readUTF());
			p.setGender(zdis.readUTF());
			p.setBirthDate(parseDate(zdis.readUTF()));
			p.setIdentifier(zdis.readUTF());
			mPatientDbAdapter.createPatient(p);

			publishProgress("patients", Integer.valueOf(i).toString(), Integer
					.valueOf(c).toString());
		}

	}

	private void insertObservations(DataInputStream zdis) throws Exception {

		// for every patient
		int icount = zdis.readInt();
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
			mPatientDbAdapter.createObservation(o);

			publishProgress("observations", Integer.valueOf(i).toString(),
					Integer.valueOf(icount).toString());
		}

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
}
