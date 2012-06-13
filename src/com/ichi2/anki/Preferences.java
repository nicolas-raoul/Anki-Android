/***************************************************************************************
 * Copyright (c) 2009 Nicolas Raoul <nicolas.raoul@gmail.com>                           *
 * Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com>                                   *
 * Copyright (c) 2010 Norbert Nagold <norbert.nagold@gmail.com>                         *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki;import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki2.R;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager.BadTokenException;

import com.hlidskialf.android.preference.SeekBarPreference;
import com.ichi2.async.DeckTask;
import com.ichi2.libanki.Utils;
import com.ichi2.themes.StyledProgressDialog;
import com.ichi2.themes.Themes;
import com.tomgibara.android.veecheck.util.PrefSettings;

/**
 * Preferences dialog.
 */
public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private static final int DIALOG_ASYNC = 1;
	private static final int DIALOG_BACKUP = 2;
	private static final int DIALOG_WRITE_ANSWERS = 4;

//    private boolean mVeecheckStatus;
    private PreferenceManager mPrefMan;
    private CheckBoxPreference zoomCheckboxPreference;
    private CheckBoxPreference keepScreenOnCheckBoxPreference;
    private CheckBoxPreference showAnswerCheckBoxPreference;
    private CheckBoxPreference swipeCheckboxPreference;
    private CheckBoxPreference animationsCheckboxPreference;
    private CheckBoxPreference useBackupPreference;
    private CheckBoxPreference asyncModePreference;
    private CheckBoxPreference hideDueCountPreference;
    private CheckBoxPreference overtimePreference;
    private CheckBoxPreference eInkDisplayPreference;
    private ListPreference mLanguageSelection;
    private CharSequence[] mLanguageDialogLabels;
    private CharSequence[] mLanguageDialogValues;
    private static String[] mAppLanguages = {"ar", "bg", "ca", "cs", "de", "el", "es", "et", "fi", "fr", "hu", "id", "it", "ja", "ko", "nl", "no", "pl", "pt_PT", "pt_BR", "ro", "ru", "sr", "sv", "th", "tr", "uk", "vi", "zh_CN", "zh_TW", "en"};
    private static String[] mShowValueInSummList = {"language", "dictionary", "reportErrorMode", "minimumCardsDueForNotification", "gestureShake", "gestureSwipeUp", "gestureSwipeDown", "gestureSwipeLeft", "gestureSwipeRight", "gestureDoubleTap", "gestureTapTop", "gestureTapBottom", "gestureTapRight", "gestureLongclick", "gestureTapLeft", "theme"};
    private static String[] mShowValueInSummSeek = {"relativeDisplayFontSize", "relativeCardBrowserFontSize", "answerButtonSize", "whiteBoardStrokeWidth", "minShakeIntensity", "swipeSensibility", "timeoutAnswerSeconds", "timeoutQuestionSeconds", "animationDuration", "backupMax"};
    private TreeMap<String, String> mListsToUpdate = new TreeMap<String, String>();
    private StyledProgressDialog mProgressDialog;
    private boolean lockCheckAction = false;
    private String dialogMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//    	Themes.applyTheme(this);
        super.onCreate(savedInstanceState);

        mPrefMan = getPreferenceManager();
        mPrefMan.setSharedPreferencesName(PrefSettings.SHARED_PREFS_NAME);

        addPreferencesFromResource(R.xml.preferences);
//        mVeecheckStatus = mPrefMan.getSharedPreferences().getBoolean(PrefSettings.KEY_ENABLED, PrefSettings.DEFAULT_ENABLED);
        
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        swipeCheckboxPreference = (CheckBoxPreference) getPreferenceScreen().findPreference("swipe");
        zoomCheckboxPreference = (CheckBoxPreference) getPreferenceScreen().findPreference("zoom");
        keepScreenOnCheckBoxPreference = (CheckBoxPreference) getPreferenceScreen().findPreference("keepScreenOn");
        showAnswerCheckBoxPreference = (CheckBoxPreference) getPreferenceScreen().findPreference("timeoutAnswer");
        animationsCheckboxPreference = (CheckBoxPreference) getPreferenceScreen().findPreference("themeAnimations");
        useBackupPreference = (CheckBoxPreference) getPreferenceScreen().findPreference("useBackup");
        asyncModePreference = (CheckBoxPreference) getPreferenceScreen().findPreference("asyncMode");
        hideDueCountPreference = (CheckBoxPreference) getPreferenceScreen().findPreference("hideDueCount");
        overtimePreference = (CheckBoxPreference) getPreferenceScreen().findPreference("overtime");
        eInkDisplayPreference = (CheckBoxPreference) getPreferenceScreen().findPreference("eInkDisplay");
        ListPreference listpref = (ListPreference) getPreferenceScreen().findPreference("theme");
        String theme = listpref.getValue();
        animationsCheckboxPreference.setEnabled(theme.equals("2") || theme.equals("3"));
        zoomCheckboxPreference.setEnabled(!swipeCheckboxPreference.isChecked());
        initializeLanguageDialog();
        initializeCustomFontsDialog();
        for (String key : mShowValueInSummList) {
            updateListPreference(key);
        }
        for (String key : mShowValueInSummSeek) {
            updateSeekBarPreference(key);
        }
    }


    private void updateListPreference(String key) {
        ListPreference listpref = (ListPreference) getPreferenceScreen().findPreference(key);
        String entry;
        try {
            entry = listpref.getEntry().toString();            
        } catch (NullPointerException e) {
            Log.e(AnkiDroidApp.TAG, "Error getting set preference value of " + key + ": " + e);
            entry = "?";
        }
        if (mListsToUpdate.containsKey(key)) {
            listpref.setSummary(replaceString(mListsToUpdate.get(key), entry));
        } else {
            String oldsum = (String) listpref.getSummary();
            if (oldsum.contains("XXX")) {
                mListsToUpdate.put(key, oldsum);
                listpref.setSummary(replaceString(oldsum, entry));
            } else {
                listpref.setSummary(entry);
            }
        }
    }


    private void updateSeekBarPreference(String key) {
        SeekBarPreference seekpref = (SeekBarPreference) getPreferenceScreen().findPreference(key);
        try {
            if (mListsToUpdate.containsKey(key)) {
                seekpref.setSummary(replaceString(mListsToUpdate.get(key), Integer.toString(seekpref.getValue())));
            } else {
                String oldsum = (String) seekpref.getSummary();
                if (oldsum.contains("XXX")) {
                    mListsToUpdate.put(key, oldsum);
                    seekpref.setSummary(replaceString(oldsum, Integer.toString(seekpref.getValue())));
                } else {
                    seekpref.setSummary(Integer.toString(seekpref.getValue()));
                }
            }        	
        } catch (NullPointerException e) {
        	Log.e(AnkiDroidApp.TAG, "Exception when updating seekbar preference: " + e);
        }
    }


//    private void enableWalSupport() {
//    	Cursor cursor = null;
//    	String sqliteVersion = "";
//    	SQLiteDatabase database = null;
//        try {
//        	database = SQLiteDatabase.openOrCreateDatabase(":memory:", null);
//        	cursor = database.rawQuery("select sqlite_version() AS sqlite_version", null);
//        	while(cursor.moveToNext()){
//        	   sqliteVersion = cursor.getString(0);
//        	}
//        } finally {
//        	database.close();
//            if (cursor != null) {
//            	cursor.close();
//            }
//        }
//        if (sqliteVersion.length() >= 3 && Double.parseDouble(sqliteVersion.subSequence(0, 3).toString()) >= 3.7) {
//        	walModePreference.setEnabled(true);
//        } else {
//        	Log.e(AnkiDroidApp.TAG, "WAL mode not available due to a SQLite version lower than 3.7.0");
//        	walModePreference.setChecked(false);
//        }
//    }


    private String replaceString(String str, String value) {
        if (str.contains("XXX")) {
            return str.replace("XXX", value);
        } else {
            return str;
        }
    }


    private void initializeLanguageDialog() {
    	TreeMap<String, String> items = new TreeMap<String, String>();
        for (String localeCode : mAppLanguages) {
			Locale loc;
			if (localeCode.length() > 2) {
				loc = new Locale(localeCode.substring(0,2), localeCode.substring(3,5));				
			} else {
				loc = new Locale(localeCode);				
			}
	    	items.put(loc.getDisplayName(), loc.toString());
		}
		mLanguageDialogLabels = new CharSequence[items.size() + 1];
		mLanguageDialogValues = new CharSequence[items.size() + 1];
		mLanguageDialogLabels[0] = getResources().getString(R.string.language_system);
		mLanguageDialogValues[0] = ""; 
		int i = 1;
		for (Map.Entry<String, String> e : items.entrySet()) {
			mLanguageDialogLabels[i] = e.getKey();
			mLanguageDialogValues[i] = e.getValue();
			i++;
		}
        mLanguageSelection = (ListPreference) getPreferenceScreen().findPreference("language");
        mLanguageSelection.setEntries(mLanguageDialogLabels);
        mLanguageSelection.setEntryValues(mLanguageDialogValues);
    }


    /** Initializes the list of custom fonts shown in the preferences. */
    private void initializeCustomFontsDialog() {
        ListPreference customFontsPreference =
            (ListPreference) getPreferenceScreen().findPreference("defaultFont");
        customFontsPreference.setEntries(getCustomFonts("System default"));
        customFontsPreference.setEntryValues(getCustomFonts(""));
    }


    @Override
    protected void onPause() {
        super.onPause();
        // Reschedule the checking in case the user has changed the veecheck switch
//        if (mVeecheckStatus ^ mPrefMan.getSharedPreferences().getBoolean(PrefSettings.KEY_ENABLED, mVeecheckStatus)) {
//            sendBroadcast(new Intent(Veecheck.getRescheduleAction(this)));
//        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	try {
            if (key.equals("swipe")) {
            	zoomCheckboxPreference.setChecked(false);
            	zoomCheckboxPreference.setEnabled(!swipeCheckboxPreference.isChecked());
            } else if (key.equals("timeoutAnswer")) {
            	keepScreenOnCheckBoxPreference.setChecked(showAnswerCheckBoxPreference.isChecked());
            } else if (key.equals("language")) {
    			Intent intent = this.getIntent();
//    			setResult(DeckPicker.RESULT_RESTART, intent);
    			closePreferences();
            } else if (key.equals("startup_mode")) {
    			Intent intent = this.getIntent();
//    			setResult(DeckPicker.RESULT_RESTART, intent);
    			closePreferences();
            } else if (key.equals("theme")) {
            	String theme = sharedPreferences.getString("theme", "3");
            	if (theme.equals("2") || theme.equals("3")) {
            		animationsCheckboxPreference.setChecked(false);
            		animationsCheckboxPreference.setEnabled(false);
            	} else {
            		animationsCheckboxPreference.setEnabled(true);
            	}
            	Themes.loadTheme();
            	switch (Integer.parseInt(sharedPreferences.getString("theme", "3"))) {
            	case Themes.THEME_ANDROID_DARK:
            	case Themes.THEME_ANDROID_LIGHT:
            	case Themes.THEME_BLUE:
            		sharedPreferences.edit().putString("defaultFont", "").commit();
            		break;
            	case Themes.THEME_FLAT:
            		sharedPreferences.edit().putString("defaultFont", "OpenSans-Regular").commit();
            		break;
            	case Themes.THEME_WHITE:
            		sharedPreferences.edit().putString("defaultFont", "OpenSans-Regular").commit();
            		break;
            	}
    			Intent intent = this.getIntent();
    			setResult(DeckPicker.RESULT_RESTART, intent);
    			closePreferences();
            } else if (Arrays.asList(mShowValueInSummList).contains(key)) {
                updateListPreference(key);
            } else if (Arrays.asList(mShowValueInSummSeek).contains(key)) {
                updateSeekBarPreference(key);
            } else if (key.equals("writeAnswers") && sharedPreferences.getBoolean("writeAnswers", false)) {
                showDialog(DIALOG_WRITE_ANSWERS);
            } else if (key.equals("useBackup")) {
            	if (lockCheckAction)  {
            		lockCheckAction = false;
            	} else if (!useBackupPreference.isChecked()) {
            		lockCheckAction = true;
            		useBackupPreference.setChecked(true);
        			showDialog(DIALOG_BACKUP);
            	} else {
            		setReloadDeck();
            	}
            } else if (key.equals("asyncMode")) {
            	if (lockCheckAction)  {
            		lockCheckAction = false;
            	} else if (asyncModePreference.isChecked()) {
            		lockCheckAction = true;
            		asyncModePreference.setChecked(false);
        			showDialog(DIALOG_ASYNC);
            	} else {
            		setReloadDeck();
            	}
            } else if (key.equals("deckPath")) {
                File decksDirectory = new File(sharedPreferences.getString("deckPath", AnkiDroidApp.getDefaultAnkiDroidDirectory()));
            	if (decksDirectory.exists()) {
            		AnkiDroidApp.createNoMediaFileIfMissing(decksDirectory);
            	}
            } else if (key.equals("eInkDisplay")) {
            	boolean enableAnimation = !eInkDisplayPreference.isChecked();
            }
        } catch (BadTokenException e) {
        	Log.e(AnkiDroidApp.TAG, "Preferences: BadTokenException on showDialog: " + e);
        }
   }


    /** Returns a list of the names of the installed custom fonts. */
    private String[] getCustomFonts(String defaultValue) {
        String[] files = Utils.getCustomFonts(this);
        int count = files.length;
        Log.d(AnkiDroidApp.TAG, "There are " + count + " custom fonts");
        String[] names = new String[count + 1];
        names[0] = defaultValue;
        for (int index = 1; index < count + 1; ++index) {
            names[index] =  Utils.removeExtension((new File(files[index - 1])).getName());
            Log.d(AnkiDroidApp.TAG, "Adding custom font: " + names[index]);
        }
        return names;
    }


    private void setReloadDeck() {
//    	DeckManager.closeMainDeck();
//		setResult(StudyOptions.RESULT_RELOAD_DECK, getIntent());
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			Log.i(AnkiDroidApp.TAG, "DeckOptions - onBackPressed()");
			closePreferences();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

    private void closePreferences() {
		finish();
		if (UIUtils.getApiLevel() > 4) {
			ActivityTransitionAnimation.slide(this,
					ActivityTransitionAnimation.FADE);
		}    	
    }

    @Override
    protected Dialog onCreateDialog(int id) {
		Resources res = getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
        case DIALOG_BACKUP:
    		builder.setTitle(res.getString(R.string.backup_manager_title));
    		builder.setCancelable(false);
    		builder.setMessage(res.getString(R.string.pref_backup_warning));
    		builder.setPositiveButton(res.getString(R.string.yes), new OnClickListener() {

    			@Override
    			public void onClick(DialogInterface arg0, int arg1) {
    				lockCheckAction = true;
    				useBackupPreference.setChecked(false);
    				dialogMessage = getResources().getString(R.string.backup_delete);
    				DeckTask.launchDeckTask(DeckTask.TASK_TYPE_DELETE_BACKUPS, mDeckOperationHandler, (DeckTask.TaskData[]) null);
    			}
    		});
    		builder.setNegativeButton(res.getString(R.string.no), null);
    		break;
        case DIALOG_ASYNC:
    		builder.setTitle(res.getString(R.string.async_mode));
    		builder.setCancelable(false);
    		builder.setMessage(res.getString(R.string.async_mode_message));
    		builder.setPositiveButton(res.getString(R.string.yes), new OnClickListener() {

    			@Override
    			public void onClick(DialogInterface arg0, int arg1) {
    				lockCheckAction = true;
    				asyncModePreference.setChecked(true);
    				setReloadDeck();
    			}
    		});
    		builder.setNegativeButton(res.getString(R.string.no), null);
    		break;
        case DIALOG_WRITE_ANSWERS:
    		builder.setTitle(res.getString(R.string.write_answers));
    		builder.setCancelable(false);
    		builder.setMessage(res.getString(R.string.write_answers_message));
    		builder.setNegativeButton(res.getString(R.string.ok), null);
    		break;
        }
		return builder.create();    	
    }


    private DeckTask.TaskListener mDeckOperationHandler = new DeckTask.TaskListener() {
        @Override
        public void onPreExecute() {
        	mProgressDialog = StyledProgressDialog.show(Preferences.this, "", dialogMessage, true);
        }


        @Override
        public void onProgressUpdate(DeckTask.TaskData... values) {
        }


        @Override
        public void onPostExecute(DeckTask.TaskData result) {
        	if (mProgressDialog != null && mProgressDialog.isShowing()) {
        		mProgressDialog.dismiss();
        	}
        	lockCheckAction = false;
        }
    };

}
