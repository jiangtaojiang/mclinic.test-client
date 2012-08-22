package org.odk.clinic.android.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.odk.clinic.android.R;
import org.odk.clinic.android.adapters.PatientAdapter;
import org.odk.clinic.android.database.ClinicAdapter;
import org.odk.clinic.android.listeners.UploadFormListener;
import org.odk.clinic.android.openmrs.Patient;
import org.odk.clinic.android.tasks.UploadInstanceTask;
import org.odk.clinic.android.utilities.Constants;
import org.odk.clinic.android.utilities.FileUtils;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

// TODO Display patient view data
// TODO Display ages instead of dates
// TODO Optimize download patient task

public class ListPatientActivity extends ListActivity implements UploadFormListener{

    // Menu ID's
	private static final int MENU_PREFERENCES = Menu.FIRST;
	private static final int MENU_GET_FORMS = Menu.FIRST + 1;
	
	// Request codes
	public static final int DOWNLOAD_PATIENT = 1;
	public static final int BARCODE_CAPTURE = 2;
	public static final int SEARCH_PATIENT = 3;

	
	private static final String DOWNLOAD_PATIENT_CANCELED_KEY = "downloadPatientCanceled";
	
	private ImageButton mDownloadButton;
	private ImageButton mSearchButton;
	private ImageButton mBarcodeButton;
	private Button mNewPatientButton;
	private Button mFilledFormsButton;
	private EditText mSearchText;
	private TextWatcher mFilterTextWatcher;

	private ArrayAdapter<Patient> mPatientAdapter;
	private ArrayList<Patient> mPatients = new ArrayList<Patient>();
	private boolean mDownloadPatientCanceled = false;
	
	private UploadInstanceTask mUploadFormTask;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
		    if (savedInstanceState.containsKey(DOWNLOAD_PATIENT_CANCELED_KEY)) {
		        mDownloadPatientCanceled = savedInstanceState.getBoolean(DOWNLOAD_PATIENT_CANCELED_KEY);
		    }
		}
		
		setContentView(R.layout.list_patients);
		
		if (!FileUtils.storageReady()) {
			showCustomToast(getString(R.string.error_storage));
			finish();
		}
		
		mFilterTextWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (mPatientAdapter != null) {
					mPatientAdapter.getFilter().filter(s);
				}
			}

			@Override
			public void afterTextChanged(Editable s) {

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}
		};

		mSearchText = (EditText) findViewById(R.id.search_text);
		mSearchText.addTextChangedListener(mFilterTextWatcher);

		mBarcodeButton = (ImageButton) findViewById(R.id.barcode_button);
		mBarcodeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent("com.google.zxing.client.android.SCAN");
				try {
					startActivityForResult(i, BARCODE_CAPTURE);
				} catch (ActivityNotFoundException e) {
					Toast t = Toast.makeText(getApplicationContext(), getString(R.string.barcode_error), Toast.LENGTH_LONG);
					t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
					t.show();
				}
			}
		});
		
		mNewPatientButton = (Button) findViewById(R.id.create_patient);
		mNewPatientButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent in = new Intent(getApplicationContext(), CreatePatientActivity.class);
				startActivity(in);
			}
		});
		
		mFilledFormsButton = (Button) findViewById(R.id.filled_forms);
		mFilledFormsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent iu = new Intent(getApplicationContext(), InstanceListActivity.class);
				startActivity(iu);
			}
		});
		
		mDownloadButton = (ImageButton) findViewById(R.id.download_patients);
		mDownloadButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent id = new Intent(getApplicationContext(), DownloadPatientActivity.class);
				startActivityForResult(id, DOWNLOAD_PATIENT);
			}
		});
		
		mDownloadButton.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				Intent id = new Intent(getApplicationContext(), DownloadPatientActivity.class);
				id.putExtra("isLong", true);
				startActivityForResult(id, DOWNLOAD_PATIENT);
				return true;
			}
		});
		
		mSearchButton = (ImageButton) findViewById(R.id.search_patient);
		mSearchButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent is = new Intent(getApplicationContext(), SearchPatientActivity.class);
                is.putExtra("searchText", mSearchText.getText().toString());
                startActivityForResult(is, 	SEARCH_PATIENT);
                
                //and hide that keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0); 
			}
		});
		
	}

	@Override
	protected void onListItemClick(ListView listView, View view, int position,
			long id) {
		// Get selected patient
		Patient p = (Patient) getListAdapter().getItem(position);
		String patientIdStr = p.getPatientId().toString();

		Intent ip = new Intent(getApplicationContext(),
				ViewPatientActivity.class);
		ip.putExtra(Constants.KEY_PATIENT_ID, patientIdStr);
		startActivity(ip);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_PREFERENCES, 0, getString(R.string.server_preferences))
				.setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_GET_FORMS, 0, getString(R.string.openmrs_forms))
				.setIcon(R.drawable.openmrs);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_PREFERENCES:
				Intent ip = new Intent(getApplicationContext(),	PreferencesActivity.class);
				startActivity(ip);
				return true;
			case MENU_GET_FORMS:
				Intent in = new Intent(getApplicationContext(), ListFormActivity.class);
				startActivity(in);
				return true;
		default:
		    return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    if (requestCode == SEARCH_PATIENT) {
	    	TextKeyListener.clear(mSearchText.getText());
	    	return;
	    }
		
		if (resultCode == RESULT_CANCELED) {
		    if (requestCode == DOWNLOAD_PATIENT) {
		        mDownloadPatientCanceled = true;
		    }
			return;
		}

		if (requestCode == BARCODE_CAPTURE && intent != null) {
			String sb = intent.getStringExtra("SCAN_RESULT");
			if (sb != null && sb.length() > 0) {
				mSearchText.setText(sb);
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);

	}

	private void getPatients(String searchStr) {

		ClinicAdapter ca = new ClinicAdapter();

		ca.open();
		Cursor c = null;
		if (searchStr != null) {
			c = ca.fetchPatients(searchStr, searchStr);
		} else {
			c = ca.fetchAllPatients();
		}

		if (c != null && c.getCount() >= 0) {

			mPatients.clear();

			int patientIdIndex = c
					.getColumnIndex(ClinicAdapter.KEY_PATIENT_ID);
			int identifierIndex = c
					.getColumnIndex(ClinicAdapter.KEY_IDENTIFIER);
			int givenNameIndex = c
					.getColumnIndex(ClinicAdapter.KEY_GIVEN_NAME);
			int familyNameIndex = c
					.getColumnIndex(ClinicAdapter.KEY_FAMILY_NAME);
			int middleNameIndex = c
					.getColumnIndex(ClinicAdapter.KEY_MIDDLE_NAME);
			int birthDateIndex = c
					.getColumnIndex(ClinicAdapter.KEY_BIRTH_DATE);
			int genderIndex = c.getColumnIndex(ClinicAdapter.KEY_GENDER);

			if (c.getCount() > 0) {
				
				Patient p;
				do {
					p = new Patient();
					p.setPatientId(c.getInt(patientIdIndex));
					p.setIdentifier(c.getString(identifierIndex));
					p.setGivenName(c.getString(givenNameIndex));
					p.setFamilyName(c.getString(familyNameIndex));
					p.setMiddleName(c.getString(middleNameIndex));
					p.setBirthDate(c.getString(birthDateIndex));
					p.setGender(c.getString(genderIndex));
					mPatients.add(p);
				} while (c.moveToNext());
			}
		
		}

		// sort em
		Collections.sort(mPatients, new Comparator<Patient>() {
			  public int compare(Patient p1, Patient p2) {
			      return p1.getName().compareTo(p2.getName());
			  }
			});
		
		refreshView();

		if (c != null) {
			c.close();
		}
		ca.close();

	}

	private void refreshView() {
		mPatientAdapter = new PatientAdapter(this, R.layout.patient_list_item, mPatients);
		setListAdapter(mPatientAdapter);
		setTitle(getString(R.string.app_name) + " > " + getString(R.string.list_patients) 
				+ " (" + mPatients.size() + ")");
	}

	@Override
	protected void onDestroy() {
		if (mUploadFormTask != null) {
            mUploadFormTask.setUploadListener(null);
            if (mUploadFormTask.getStatus() == AsyncTask.Status.FINISHED) {
                mUploadFormTask.cancel(true);
            }
        }
		
		super.onDestroy();
		mSearchText.removeTextChangedListener(mFilterTextWatcher);
	}

	@Override
	protected void onResume() {
		if (mUploadFormTask != null) {
            mUploadFormTask.setUploadListener(this);
        }
		super.onResume();
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	    boolean firstRun = settings.getBoolean(PreferencesActivity.KEY_FIRST_RUN, true);
	    
		if (firstRun) {
		    // Save first run status
		    SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(PreferencesActivity.KEY_FIRST_RUN, false);
            editor.commit();
            
            // Start preferences activity
		    Intent ip = new Intent(getApplicationContext(),
                    PreferencesActivity.class);
            startActivity(ip);
            
		} else {
            getPatients(null);
            mSearchText.setText(mSearchText.getText().toString());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putBoolean(DOWNLOAD_PATIENT_CANCELED_KEY, mDownloadPatientCanceled);
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

	@Override
	public void uploadComplete(ArrayList<String> result) {
	}

	@Override
	public void progressUpdate(String message, int progress, int max) {
	}
}