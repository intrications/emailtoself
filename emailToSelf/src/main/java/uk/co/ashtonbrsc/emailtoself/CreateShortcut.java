package uk.co.ashtonbrsc.emailtoself;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.widget.EditText;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.util.Arrays;

public class CreateShortcut extends Activity {

	private SharedPreferences mAppPreferences;

	private String mPrefix;

	private String mEmailAddress;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		checkForEmptyEmailAddress();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		startEmailIntent();
	}

	private void checkForEmptyEmailAddress() {
		SharedPreferences appPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (appPreferences.getString("email", "").equals("")) {
			Intent prefIntent = new Intent(this, EditPreferences.class);
			prefIntent.putExtra("noemail", "true");
			startActivityForResult(prefIntent, 0);
		} else {
			startEmailIntent();
		}
	}

	private void startEmailIntent() {
		String[] emailAddresses = mAppPreferences.getString("email", "").split(
				",");
		for (int i = 0; i < emailAddresses.length; i++) {
			emailAddresses[i] = emailAddresses[i].trim();
		}
		if (mAppPreferences.getBoolean("alphaEmail", false)) { // alphabetize?
			Arrays.sort(emailAddresses);
		}
		if (emailAddresses.length > 1) {
			showEmailDialog(emailAddresses);
		} else if (emailAddresses.length == 1) {
			mEmailAddress = emailAddresses[0];
			choosePrefix();
		} else {
			mEmailAddress = "";
			choosePrefix();
		}
	}

	private void choosePrefix() {
		String prefix = mAppPreferences.getString("prefix", "");
		boolean blankPrefix = mAppPreferences.getBoolean("blankPrefix", false);
		String[] prefixes = prefix.split(",");
		for (int i = 0; i < prefixes.length; i++) {
			prefixes[i] = prefixes[i].trim();
		}
		if (mAppPreferences.getBoolean("alphaPrefix", false)) { // alphabetize?
			Arrays.sort(prefixes, String.CASE_INSENSITIVE_ORDER);
		}
		if (blankPrefix && prefixes[0] != "") {
			String[] prefixesWithBlank = new String[prefixes.length + 1];
			for (int i = 0; i < prefixes.length; i++) {
				prefixesWithBlank[i + 1] = prefixes[i];
			}
			prefixesWithBlank[0] = "No Label";
			prefixes = prefixesWithBlank;
		}
		if (prefixes.length > 1) {
			showLabelDialog(prefixes);
		} else if (prefixes.length == 1) {
			mPrefix = prefixes[0];
			createShortcut();
		} else {
			createShortcut();
		}
	}

	private void showLabelDialog(String[] prefixes) {
		final String[] prefixesWithChoose = new String[prefixes.length + 1];
		for (int i = 0; i < prefixes.length; i++) {
			prefixesWithChoose[i + 1] = prefixes[i];
		}
		prefixesWithChoose[0] = "Choose Each Time";
		AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
		builder.setTitle("Choose Label");
		builder.setItems(prefixesWithChoose,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						if (item != 0) {
							mPrefix = prefixesWithChoose[item];
						}
						createShortcut();
						dialog.dismiss();
					}
				});
		Dialog alert = builder.create();
		alert.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		alert.show();
	}

	private void showEmailDialog(final String[] prefixes) {
		final String[] prefixesWithChoose = new String[prefixes.length + 1];
		for (int i = 0; i < prefixes.length; i++) {
			prefixesWithChoose[i + 1] = prefixes[i];
		}
		prefixesWithChoose[0] = "Choose Each Time";
		AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
		builder.setTitle("Choose Email Address");
		builder.setItems(prefixesWithChoose,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						if (item != 0) {
							mEmailAddress = prefixesWithChoose[item];
						}
						choosePrefix();
						dialog.dismiss();
					}
				});
		Dialog alert = builder.create();
		alert.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		alert.show();
	}

	private void createShortcut() {
		AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
		builder.setTitle("Choose Name For Shortcut");
		final EditText editText = new EditText(this);
		builder.setView(editText);
		builder.setPositiveButton("Create Shortcut",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent shortcutIntent = new Intent(CreateShortcut.this,
								QuickEmailToSelf.class);
						if (mEmailAddress != null) {
							shortcutIntent.putExtra("emailAddress",
									mEmailAddress);
						}
						if (mPrefix != null && mPrefix != "") {
							shortcutIntent.putExtra("label", mPrefix);
						}
						Intent intent = new Intent();
						intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
								shortcutIntent);
						intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, editText
								.getText().toString());
						Parcelable iconResource = Intent.ShortcutIconResource
								.fromContext(CreateShortcut.this,
										R.drawable.icon);
						intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
								iconResource);
						setResult(RESULT_OK, intent);
						finish();
					}
				});
		builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
		builder.setCancelable(false);
		builder.show();

	}
}
