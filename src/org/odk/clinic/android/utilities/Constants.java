package org.odk.clinic.android.utilities;

public class Constants {

	/** Value representing a not yet set status. */
	public static final int STATUS_NULL = -1;

	/** Value representing success of an action. */
	public static final int STATUS_SUCCESS = 1;

	/** Value representing failure of an action. */
	public static final int STATUS_FAILURE = 0;
	
	/** Value representing failure of an action. */
	public static final int STATUS_ACCESS_DENIED = 2;

	public static final String PATIENT_DOWNLOAD_URL = "/module/mclinic/download/patients.form";
	
	public static final String COHORT_DOWNLOAD_URL = "/module/mclinic/download/cohort.form";
	
	public static final String PATIENT_SEARCH_URL = "/module/mclinic/search/patient.form";
	
	public static final String FORMLIST_DOWNLOAD_URL = "/module/mclinic/download/xformList.form?type=mclinic";
	
	public static final String FORM_DOWNLOAD_URL = "/module/mclinic/download/xform.form?type=mclinic";
	
	public static final String INSTANCE_UPLOAD_URL = "/module/mclinic/upload/xform.form";
	
	public static final int TYPE_STRING = 1;
	public static final int TYPE_INT = 2;
	public static final int TYPE_DOUBLE = 3;
	public static final int TYPE_DATE = 4;
	public static final int CONNECTION_TIMEOUT = 120000;
	
	public static final String KEY_PATIENT_ID = "PATIENT_ID";
	public static final String KEY_PATIENT_NAME = "PATIENT_NAME";
	public static final String KEY_PATIENT_IDENTIFIER = "PATIENT_IDENTIFIER";
	
	public static final String KEY_OBSERVATION_FIELD_NAME = "KEY_OBSERVATION_FIELD_NAME";
	
	public static final String TASK_SUCCESS = "Success";
	public static final String TASK_ERROR = "Error";
	
	public static final String PATIENTS = "Patients";
	public static final String OBSERVATIONS = "Observations";
	
	public static final String SCREEN_LOCKER = "org.odk.clinic.android.intent.action.LOCK";
	public static final String CODE_REGEX = "[0-9]{6,}$";
	public static final String BACK_DOOR = "732738";
}