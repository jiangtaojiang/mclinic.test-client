package org.odk.clinic.android.activities;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.odk.clinic.android.R;
import org.odk.clinic.android.adapters.ObservationAdapter;
import org.odk.clinic.android.database.ClinicAdapter;
import org.odk.clinic.android.openmrs.FormInstance;
import org.odk.clinic.android.openmrs.Observation;
import org.odk.clinic.android.openmrs.Patient;
import org.odk.clinic.android.utilities.App;
import org.odk.clinic.android.utilities.Constants;
import org.odk.clinic.android.utilities.FileUtils;
import org.odk.collect.android.provider.FormsProviderAPI.FormsColumns;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

// TODO if no obs, don't crash when viewing patients

public class ViewPatientActivity extends ListActivity {

	private Button mActionButton;
	private static Patient mPatient;
	private static String mProviderId;
	private String mSelectedId;
	private ArrayList<String> mForms = new ArrayList<String>();
	private static HashMap<String, String> mInstanceValues = new HashMap<String, String>();
	private static final DateFormat COLLECT_INSTANCE_NAME_DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd_HH-mm-ss");

	private static DateFormat mXFormDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private ArrayAdapter<Observation> mObservationAdapter;
	private static ArrayList<Observation> mObservations = new ArrayList<Observation>();
	private static Element mFormNode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_patient);

		if (!FileUtils.storageReady()) {
			showCustomToast(getString(R.string.error_storage));
			finish();
		}

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		mProviderId = settings.getString(PreferencesActivity.KEY_PROVIDER, "0");

		// TODO Check for invalid patient IDs
		String patientIdStr = getIntent().getStringExtra(Constants.KEY_PATIENT_ID);
		Integer patientId = Integer.valueOf(patientIdStr);
		mPatient = getPatient(patientId);

		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.view_patient));

		View patientView = (View) findViewById(R.id.patient_info);
		patientView.setBackgroundResource(R.drawable.search_holder_gradient);

		TextView textView = (TextView) findViewById(R.id.identifier_text);
		if (textView != null)
			textView.setText(mPatient.getIdentifier());

		textView = (TextView) findViewById(R.id.name_text);
		if (textView != null) {
			textView.setText(mPatient.getName());
		}

		textView = (TextView) findViewById(R.id.birthdate_text);
		if (textView != null) {
			textView.setText(mPatient.getBirthdate());
		}

		ImageView imageView = (ImageView) findViewById(R.id.gender_image);
		if (imageView != null) {
			if (mPatient.getGender().equals("M")) {
				imageView.setImageResource(R.drawable.male);
			} else if (mPatient.getGender().equals("F")) {
				imageView.setImageResource(R.drawable.female);
			}
		}

		mActionButton = (Button) findViewById(R.id.fill_forms);
		mActionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {

				getDownloadedForms();
				if (mForms.size() > 0) {
					createFillFormDialog();
				} else {
					showCustomToast(getString(R.string.no_form_to_fill));
				}
			}
		});
	}

	private void getDownloadedForms() {

		ClinicAdapter ca = new ClinicAdapter();
		ca.open();
		Cursor c = ca.fetchAllForms();

		if (c != null && c.getCount() >= 0) {
			mForms.clear();
			int formIdIndex = c.getColumnIndex(ClinicAdapter.KEY_FORM_ID);
			int nameIndex = c.getColumnIndex(ClinicAdapter.KEY_NAME);
			int pathIndex = c.getColumnIndex(ClinicAdapter.KEY_PATH);

			if (c.getCount() > 0) {
				do {
					if (!c.isNull(pathIndex))
						mForms.add(c.getString(nameIndex) + " ("
								+ c.getInt(formIdIndex) + ")");
				} while (c.moveToNext());
			}
		}

		refreshView();

		if (c != null)
			c.close();

		ca.close();
	}

	private static void traverseInstanceNodes(Element element) {

		// extract 'WEIGHT (KG)' from '5089^WEIGHT (KG)^99DCT'
		Pattern pattern = Pattern.compile("\\^.*\\^");

		// loop through all the children of this element
		for (int i = 0; i < element.getChildCount(); i++) {

			Element childElement = element.getElement(i);
			if (childElement != null) {

				String childName = childElement.getName();

				// patient id
				if (childName.equalsIgnoreCase("patient.patient_id")) {
					childElement.clear();
					childElement.addChild(0, org.kxml2.kdom.Node.TEXT, mPatient
							.getPatientId().toString());
				}
				if (childName.equalsIgnoreCase("patient.birthdate")) {
					childElement.clear();					
					childElement.addChild(0, org.kxml2.kdom.Node.TEXT, mXFormDateFormat.format(mPatient
							.getBirthdate()).toString());
				}
				if (childName.equalsIgnoreCase("patient.family_name")) {
					childElement.clear();
					childElement.addChild(0, org.kxml2.kdom.Node.TEXT, mPatient
							.getFamilyName().toString());
				}
				if (childName.equalsIgnoreCase("patient.given_name")) {
					childElement.clear();
					childElement.addChild(0, org.kxml2.kdom.Node.TEXT, mPatient
							.getGivenName().toString());
				}
				if (childName.equalsIgnoreCase("patient.middle_name")) {
					childElement.clear();
					childElement.addChild(0, org.kxml2.kdom.Node.TEXT, mPatient
							.getMiddleName().toString());
				}
				if (childName.equalsIgnoreCase("patient.sex")) {
					childElement.clear();
					childElement.addChild(0, org.kxml2.kdom.Node.TEXT, mPatient
							.getGender().toString());
				}
				if (childName.equalsIgnoreCase("patient.medical_record_number")) {
					childElement.clear();
					childElement.addChild(0, org.kxml2.kdom.Node.TEXT, mPatient
							.getIdentifier().toString());
				}
				// provider id
				if (childName.equalsIgnoreCase("encounter.provider_id")) {
					childElement.clear();
					childElement.addChild(0, org.kxml2.kdom.Node.TEXT,
							mProviderId.toString());
				}
				
				// remove xsi:null
				if (childName.equalsIgnoreCase("date")
						|| childName.equalsIgnoreCase("time")) {
					childElement.clear();
				}

				// value node
				if (childName.equalsIgnoreCase("value")) {
					
					// remove xsi:null
					childElement.clear();

					// parent of value node
					Element parentElement = ((Element) childElement.getParent());
					String parentConcept = parentElement.getAttributeValue("", "openmrs_concept");

					// match the text inside ^^
					String match = null;
					Matcher matcher = pattern.matcher(parentConcept);
					while (matcher.find()) {
						match = matcher.group(0).substring(1,matcher.group(0).length() - 1);
					}

					// write value into value n
					String value = mInstanceValues.get(match);
					if (value != null) {
						childElement.addChild(0, org.kxml2.kdom.Node.TEXT, value.toString());
					}
				}

				if (childElement.getChildCount() > 0) {
					traverseInstanceNodes(childElement);
				}
			}
		}
	}

	private static void prepareInstanceValues() {

		for (int i = 0; i < mObservations.size(); i++) {
			Observation o = mObservations.get(i);
			mInstanceValues.put(o.getFieldName(), o.toString());
		}
	}

	private static int createFormInstance(String formPath, String jrFormId) {

		// reading the form
		Document doc = new Document();
		KXmlParser formParser = new KXmlParser();
		try {
			formParser.setInput(new FileReader(formPath));
			doc.parse(formParser);
		} catch (Exception e) {
			e.printStackTrace();
		}

		findFormNode(doc.getRootElement());
		if (mFormNode!=null) {
			prepareInstanceValues();
			traverseInstanceNodes(mFormNode);
		} else {
			return -1;
		}
		
		// writing the instance file
		File formFile = new File(formPath);
		String formFileName = formFile.getName();
		String instanceName = "";
		if (formFileName.endsWith(".xml")) {
			instanceName = formFileName.substring(0, formFileName.length() - 4);
		} else {
			instanceName = jrFormId;
		}
		instanceName = instanceName + "_"
				+ COLLECT_INSTANCE_NAME_DATE_FORMAT.format(new Date());

		String instancePath = FileUtils.INSTANCES_PATH + instanceName;
		(new File(instancePath)).mkdirs();
		String instanceFilePath = instancePath + "/" + instanceName + ".xml";
		File instanceFile = new File(instanceFilePath);

		KXmlSerializer instanceSerializer = new KXmlSerializer();
		try {
			instanceFile.createNewFile();
			FileWriter instanceWriter = new FileWriter(instanceFile);
			instanceSerializer.setOutput(instanceWriter);
			mFormNode.write(instanceSerializer);
			instanceSerializer.endDocument();
			instanceSerializer.flush();
			instanceWriter.close();

			// register into content provider
			ContentValues insertValues = new ContentValues();
			insertValues.put("displayName", mPatient.getGivenName() + " "
					+ mPatient.getFamilyName());
			insertValues.put("instanceFilePath", instanceFilePath);
			insertValues.put("jrFormId", jrFormId);
			Uri insertResult = App.getApp().getContentResolver()
					.insert(InstanceColumns.CONTENT_URI, insertValues);
			
			// insert to clinic
			// Save form instance to db
			FormInstance fi = new FormInstance();
			fi.setPatientId(mPatient.getPatientId());
			fi.setFormId(Integer.parseInt(jrFormId));
			fi.setPath(instanceFilePath);
			fi.setStatus(ClinicAdapter.STATUS_UNSUBMITTED);

			ClinicAdapter ca = new ClinicAdapter();
			ca.open();
			ca.createFormInstance(fi, mPatient.getGivenName() + " "
					+ mPatient.getFamilyName());
			ca.close();
			
			return Integer.valueOf(insertResult.getLastPathSegment());

		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static void findFormNode(Element element) {

		// loop through all the children of this element
		for (int i = 0; i < element.getChildCount(); i++) {

			Element childElement = element.getElement(i);
			if (childElement != null) {

				String childName = childElement.getName();
				if (childName.equalsIgnoreCase("form"))
					mFormNode = childElement;

				if (childElement.getChildCount() > 0)
					findFormNode(childElement);
			}
		}
	}
	
	private void launchFormEntry(String jrFormId) {

		String formPath = null;
		int id = -1;
		try {
			Cursor mCursor = App.getApp().getContentResolver()
					.query(FormsColumns.CONTENT_URI, null, null, null, null);
			mCursor.moveToPosition(-1);
			while (mCursor.moveToNext()) {

				int dbid = mCursor.getInt(mCursor.getColumnIndex(FormsColumns._ID));
				String dbjrFormId = mCursor.getString(mCursor.getColumnIndex(FormsColumns.JR_FORM_ID));
				formPath = mCursor.getString(mCursor.getColumnIndex(FormsColumns.FORM_FILE_PATH));
				
				if (jrFormId.equalsIgnoreCase(dbjrFormId)) {
					id = dbid;
					break;
				}
			}
			if (mCursor != null) {
				mCursor.close();
			}
			
			if (id != -1) {

				// create instance
				int instanceId = createFormInstance(formPath, jrFormId);
				if (instanceId != -1) {
					Intent intent = new Intent();
					intent.setComponent(new ComponentName(
							"org.odk.collect.android",
							"org.odk.collect.android.activities.FormEntryActivity"));
					intent.setAction(Intent.ACTION_EDIT);
					intent.setData(Uri.parse(InstanceColumns.CONTENT_URI + "/" + instanceId));

					startActivity(intent);
				} else {
					Uri formUri = ContentUris.withAppendedId(FormsColumns.CONTENT_URI, id);
					startActivity(new Intent(Intent.ACTION_EDIT, formUri));
				}
			}else
				showCustomToast(getString(R.string.error_form_load));
		} catch (ActivityNotFoundException e) {
			showCustomToast(getString(R.string.error_odk_collect));
		} catch (NullPointerException e) {
			showCustomToast(getString(R.string.error_initialize_odk_collect));
		}
	}
	
	private void createFillFormDialog() {

		final CharSequence[] items = mForms.toArray(new CharSequence[mForms.size()]);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select a form to fill");

		builder.setSingleChoiceItems(items, -1, null);
		builder.setPositiveButton("Fill Form",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// extract form id
						int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
						
						if (i != -1) {
							Pattern p = Pattern.compile("\\([0-9]+\\)$");
							Matcher m = p.matcher(items[i]);
							while (m.find()) {
								mSelectedId = m.group(0).substring(1,m.group(0).length() - 1);
							}
							dialog.dismiss();
							launchFormEntry(mSelectedId);
						} else {
							dialog.dismiss();
						}

					}
				});
	    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                dialog.cancel();
	           }
	       });
		AlertDialog alert = builder.create();
		alert.show();
	}

	private Patient getPatient(Integer patientId) {

		Patient p = null;
		ClinicAdapter ca = new ClinicAdapter();

		ca.open();
		Cursor c = ca.fetchPatient(patientId);

		if (c != null && c.getCount() > 0) {
			int patientIdIndex = c.getColumnIndex(ClinicAdapter.KEY_PATIENT_ID);
			int identifierIndex = c.getColumnIndex(ClinicAdapter.KEY_IDENTIFIER);
			int givenNameIndex = c.getColumnIndex(ClinicAdapter.KEY_GIVEN_NAME);
			int familyNameIndex = c.getColumnIndex(ClinicAdapter.KEY_FAMILY_NAME);
			int middleNameIndex = c.getColumnIndex(ClinicAdapter.KEY_MIDDLE_NAME);
			int birthDateIndex = c.getColumnIndex(ClinicAdapter.KEY_BIRTH_DATE);
			int genderIndex = c.getColumnIndex(ClinicAdapter.KEY_GENDER);

			p = new Patient();
			p.setPatientId(c.getInt(patientIdIndex));
			p.setIdentifier(c.getString(identifierIndex));
			p.setGivenName(c.getString(givenNameIndex));
			p.setFamilyName(c.getString(familyNameIndex));
			p.setMiddleName(c.getString(middleNameIndex));
			p.setBirthDate(c.getString(birthDateIndex));
			p.setGender(c.getString(genderIndex));
		}

		if (c != null) {
			c.close();
		}
		ca.close();

		return p;
	}

	private void getAllObservations(Integer patientId) {

		ClinicAdapter ca = new ClinicAdapter();

		ca.open();
		Cursor c = ca.fetchPatientObservations(patientId);

		mObservations.clear();
		if (c != null && c.getCount() > 0) {
			int valueTextIndex = c.getColumnIndex(ClinicAdapter.KEY_VALUE_TEXT);
			int valueIntIndex = c.getColumnIndex(ClinicAdapter.KEY_VALUE_INT);
			int valueDateIndex = c.getColumnIndex(ClinicAdapter.KEY_VALUE_DATE);
			int valueNumericIndex = c
					.getColumnIndex(ClinicAdapter.KEY_VALUE_NUMERIC);
			int fieldNameIndex = c.getColumnIndex(ClinicAdapter.KEY_FIELD_NAME);
			int encounterDateIndex = c
					.getColumnIndex(ClinicAdapter.KEY_ENCOUNTER_DATE);
			int dataTypeIndex = c.getColumnIndex(ClinicAdapter.KEY_DATA_TYPE);

			Observation obs;
			String prevFieldName = null;
			do {
				String fieldName = c.getString(fieldNameIndex);

				// We only want most recent observation, so only get first
				// observation
				if (!fieldName.equals(prevFieldName)) {

					obs = new Observation();
					obs.setFieldName(fieldName);
					obs.setEncounterDate(c.getString(encounterDateIndex));

					int dataType = c.getInt(dataTypeIndex);
					obs.setDataType((byte) dataType);
					switch (dataType) {
						case Constants.TYPE_INT :
							obs.setValueInt(c.getInt(valueIntIndex));
							break;
						case Constants.TYPE_DOUBLE :
							obs.setValueNumeric(c.getDouble(valueNumericIndex));
							break;
						case Constants.TYPE_DATE :
							obs.setValueDate(c.getString(valueDateIndex));
							break;
						default :
							obs.setValueText(c.getString(valueTextIndex));
					}

					mObservations.add(obs);

					prevFieldName = fieldName;
				}

			} while (c.moveToNext());
		}

		refreshView();

		if (c != null) {
			c.close();
		}
		ca.close();
	}

	@Override
	protected void onListItemClick(ListView listView, View view, int position,
			long id) {

		if (mPatient != null) {
			// Get selected observation
			Observation obs = (Observation) getListAdapter().getItem(position);

			Intent ip;
			int dataType = obs.getDataType();
			if (dataType == Constants.TYPE_INT || dataType == Constants.TYPE_DOUBLE) {
				ip = new Intent(getApplicationContext(),ObservationChartActivity.class);
				ip.putExtra(Constants.KEY_PATIENT_ID, mPatient.getPatientId().toString());
				ip.putExtra(Constants.KEY_OBSERVATION_FIELD_NAME,obs.getFieldName());
				startActivity(ip);
			} else {
				ip = new Intent(getApplicationContext(),ObservationTimelineActivity.class);
				ip.putExtra(Constants.KEY_PATIENT_ID, mPatient.getPatientId().toString());
				ip.putExtra(Constants.KEY_OBSERVATION_FIELD_NAME,obs.getFieldName());
				startActivity(ip);
			}
		}
	}

	private void refreshView() {

		mObservationAdapter = new ObservationAdapter(this,
				R.layout.observation_list_item, mObservations);
		setListAdapter(mObservationAdapter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mPatient != null) {
			// observation values
			getAllObservations(mPatient.getPatientId());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	private void showCustomToast(String message) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.toast_view, null);

		// set the text in the view
		TextView tv = (TextView) view.findViewById(R.id.message);
		tv.setText(message);

		Toast t = new Toast(this);
		t.setView(view);
		t.setDuration(Toast.LENGTH_LONG);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}
}