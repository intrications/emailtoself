package uk.co.ashtonbrsc.emailtoself;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditPreferencesFragment extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {

	private final static String emailAppPackageName = "packageName";
	private final static String emailAppClassName = "className";
    private static final int PICK_ACCOUNT_REQUEST = 1;

    private EditTextPreference mEmailAddressPreference;

	private EditTextPreference mPrefixPreference;

	private SharedPreferences mAppPreferences;

	private ComponentName quickEmailComponent;

	private PackageManager packageManager;

	private int quickEmailEnabled;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		packageManager = getActivity().getPackageManager();
		addPreferencesFromResource(R.xml.preferences);
		mAppPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mEmailAddressPreference = (EditTextPreference) getPreferenceScreen()
				.findPreference("email");
		mPrefixPreference = (EditTextPreference) getPreferenceScreen()
				.findPreference("prefix");

		findPreference("chooseEmailApp").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						GetEmailApp getEmailApp = new GetEmailApp(getActivity());
						getEmailApp.showListOfPossibleApps(null);
						return false;
					}
				});

//        findPreference("syncPrefs").setOnPreferenceClickListener(new OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                Intent googlePicker = AccountPicker.newChooseAccountIntent(null, null,
//                        new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, true, null, null, null, null) ;
//                startActivityForResult(googlePicker,PICK_ACCOUNT_REQUEST);
//                return true;
//            }
//        })
	}

	@Override
	public void onResume() {
		super.onResume();
		displayCurrentEmailAddress();
		displayCurrentPrefix();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		getPreferenceScreen().findPreference("help")
				.setOnPreferenceClickListener(
						new OnPreferenceClickListener() {
							public boolean onPreferenceClick(
									Preference preference) {
								AlertDialogWrapper.Builder dialog = new AlertDialogWrapper.Builder(getActivity());
								dialog.setTitle("How to use Email To Self");
								dialog.setMessage("From Launcher: Click the Email Note To Self app icon to send a quick email\n\nFrom Browser: Click Menu, More, Share Page, Email To Self.\n\nFrom YouTube: Click Menu, Share, Email To Self.");
								dialog.setNeutralButton("Ok",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												dialog.dismiss();

											}
										});
								dialog.show();
								return true;
							}
						});

		final Preference removePreference = getPreferenceScreen()
				.findPreference("removePrefsFromLauncher");
		removePreference
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					public boolean onPreferenceClick(Preference preference) {

						AlertDialogWrapper.Builder dialog = new AlertDialogWrapper.Builder(
								getActivity());
						dialog.setTitle("Remove Icon From Launcher?");
						dialog.setMessage("Once the icon is removed you will only be able to change settings by uninstalling and then reinstalling the application. You may need to reboot before the icon disappears.");
						dialog.setPositiveButton("Remove Icon",
								new DialogInterface.OnClickListener() {

									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();

										packageManager
												.setComponentEnabledSetting(
														getActivity().getComponentName(),
														PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
														PackageManager.DONT_KILL_APP);
										removePreference.setEnabled(false);

									}
								});
						dialog.setNegativeButton("Cancel",
								new DialogInterface.OnClickListener() {

									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
									}
								});
						dialog.show();
						return true;
					}

				});

		final Preference addPreference = getPreferenceScreen().findPreference(
				"addQuickEmailToLauncher");

		if (mAppPreferences.getBoolean("addQuickEmailToLauncher", true)) {
			addPreference.setTitle("Remove Email Note To Self Icon");
			// addPreference.setSummary("Remove icon from launcher.");
		}

		addPreference
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					public boolean onPreferenceClick(Preference preference) {
						if (mAppPreferences.getBoolean(
								"addQuickEmailToLauncher", true)) {

							AlertDialogWrapper.Builder dialog = new AlertDialogWrapper.Builder(
									getActivity());
							dialog.setTitle("Remove Email Note To Self Icon From Launcher?");
							/*
							 * dialog .setMessage("Remove the icon?");
							 */
							dialog.setPositiveButton("Remove Icon",
									new DialogInterface.OnClickListener() {

										public void onClick(
												DialogInterface dialog,
												int which) {
											dialog.dismiss();

											packageManager
													.setComponentEnabledSetting(
															new ComponentName(
																	"uk.co.ashtonbrsc.emailtoself",
																	"uk.co.ashtonbrsc.emailtoself.QuickEmailToSelf"),
															PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
															PackageManager.DONT_KILL_APP);
											mAppPreferences
													.edit()
													.putBoolean(
															"addQuickEmailToLauncher",
															false).apply();
											addPreference
													.setTitle("Add Email Note To Self Icon");
										}
									});
							dialog.setNegativeButton("Cancel",
									new DialogInterface.OnClickListener() {

										public void onClick(
												DialogInterface dialog,
												int which) {
											dialog.dismiss();

										}
									});
							dialog.show();
							return true;
						}

						else {
							AlertDialogWrapper.Builder dialog = new AlertDialogWrapper.Builder(
									getActivity());
							dialog.setTitle("Add Email Note To Self Icon To Launcher?");
							dialog.setMessage("You may need to reboot before icon appears in launcher.");
							dialog.setPositiveButton("Add Icon",
									new DialogInterface.OnClickListener() {

										public void onClick(
												DialogInterface dialog,
												int which) {
											dialog.dismiss();

											packageManager
													.setComponentEnabledSetting(
															new ComponentName(
																	"uk.co.ashtonbrsc.emailtoself",
																	"uk.co.ashtonbrsc.emailtoself.QuickEmailToSelf"),
															PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
															PackageManager.DONT_KILL_APP);
											mAppPreferences
													.edit()
													.putBoolean(
															"addQuickEmailToLauncher",
															true).apply();
											addPreference
													.setTitle(R.string.remove_quick_notes_prefs);
										}
									});
							dialog.setNegativeButton("Cancel",
									new DialogInterface.OnClickListener() {

										public void onClick(
												DialogInterface dialog,
												int which) {
											dialog.dismiss();

										}
									});
							dialog.show();
							return true;
						}
					}
				});

		final CheckBoxPreference browserPreference = (CheckBoxPreference) getPreferenceScreen()
				.findPreference("fakeBrowser");
		browserPreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						if (newValue == Boolean.TRUE) {
							packageManager
									.setComponentEnabledSetting(
											new ComponentName(
													"uk.co.ashtonbrsc.emailtoself",
													"uk.co.ashtonbrsc.emailtoself.FakeBrowser"),
											PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
											PackageManager.DONT_KILL_APP);
						} else {
							packageManager
									.setComponentEnabledSetting(
											new ComponentName(
													"uk.co.ashtonbrsc.emailtoself",
													"uk.co.ashtonbrsc.emailtoself.FakeBrowser"),
											PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
											PackageManager.DONT_KILL_APP);
						}
						return true;
					}
				});

	}

	@Override
	public void onPause() {
		super.onPause();
		// new BackupManager(this).dataChanged();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals("email")) {
			displayCurrentEmailAddress();
		} else if (key.equals("prefix")) {
			displayCurrentPrefix();
		}
	}

	private void displayCurrentPrefix() {
		mPrefixPreference.setSummary(mAppPreferences.getString("prefix", ""));

	}

	private void displayCurrentEmailAddress() {

		String email = mAppPreferences.getString("email", "");
		Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
		Matcher m = p.matcher(email);
		boolean matchFound = m.matches();
		if (email.equals("")) {
			mEmailAddressPreference
					.setSummary("Email address to send webpages to.");
		} else if (!matchFound) {

			mEmailAddressPreference
					.setSummary("This doesn't look like a valid email address.\n"
							+ email);
		} else {
			mEmailAddressPreference.setSummary(email);
		}
	}

	public static void saveEmailApp(Context context, String packageName,
			String className) {
		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.putString(emailAppPackageName, packageName)
				.putString(emailAppClassName, className).apply();
	}

	public static String[] getEmailApp(Context context) {
		String packageName = PreferenceManager.getDefaultSharedPreferences(
				context).getString(emailAppPackageName, null);
		String className = PreferenceManager.getDefaultSharedPreferences(
				context).getString(emailAppClassName, null);
		if (packageName == null || className == null) {
			return null;
		}
		return new String[] { packageName, className };
	}

}
