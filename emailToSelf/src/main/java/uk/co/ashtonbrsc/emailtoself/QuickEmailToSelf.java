package uk.co.ashtonbrsc.emailtoself;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.util.Arrays;

public class QuickEmailToSelf extends Activity {

	private SharedPreferences mAppPreferences;

	private String mPrefix;

	private String mEmailAddress;

	private String mSubject;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		checkForEmptyEmailAddress();
		// mAppPreferences.edit().putBoolean("showDefaultAppWarning",
		// true).commit();
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
		mEmailAddress = getIntent().getStringExtra("emailAddress");
		if (mEmailAddress != null) {
			choosePrefix();
		} else {
			String[] emailAddresses = mAppPreferences.getString("email", "")
					.split(",");
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
	}

	private void choosePrefix() {
		mPrefix = getIntent().getStringExtra("label");
		if (mPrefix != null) {
			sendEmailIntent();
		} else {
			String prefix = mAppPreferences.getString("prefix", "");
			boolean blankPrefix = mAppPreferences.getBoolean("blankPrefix",
					false);
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
				sendEmailIntent();
			} else {
				sendEmailIntent();
			}
		}
	}

	private void showLabelDialog(final String[] items) {
		AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
		builder.setTitle("Choose Label");
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				mPrefix = items[item];
				sendEmailIntent();
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

	private void showEmailDialog(final String[] items) {
		AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
		builder.setTitle("Choose Email Address");
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				mEmailAddress = items[item];
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

	private void sendEmailIntent() {
		final Intent incomingIntent = new Intent();
		boolean squareBrackets = mAppPreferences.getBoolean("squareBrackets",
				true);
		if (mPrefix != null && mPrefix != "" && !mPrefix.equals("No Label")) {
			StringBuilder sb = new StringBuilder();
			if (squareBrackets) {
				sb.append("[");
			}
			sb.append(mPrefix);
			if (squareBrackets) {
				sb.append("] ");
			}
			mSubject = sb.toString();
		}
		if (mEmailAddress.endsWith("googlemail.com")
				|| mEmailAddress.endsWith("gmail.com")
				|| mEmailAddress.contains("+")) { // send with ACTION_SEND if
													// there is a plus in the
													// email address to avoid it
													// being striped out. Only
													// works with the GMail app
													// though. May need to add a
													// preference if using Email
													// app but most seem to use
													// GMail app.
			incomingIntent.setAction(Intent.ACTION_SEND);
			// intent.setType("text/plain"); //this makes too many options come
			// up
			incomingIntent.setType("message/rfc822");
			incomingIntent.putExtra(Intent.EXTRA_EMAIL,
					new String[] { mEmailAddress });
		} else {
			incomingIntent.setAction(Intent.ACTION_SENDTO);
			incomingIntent.setData(Uri.parse("mailto:" + mEmailAddress));
		}

		if (mSubject != null) {
			incomingIntent.putExtra(Intent.EXTRA_SUBJECT, mSubject);
		}

		incomingIntent.setComponent(null);

		final GetEmailApp getEmailApp = new GetEmailApp(QuickEmailToSelf.this);
		getEmailApp.addEmailAppToIntent(incomingIntent,
				getEmailApp.new OnChooseEmailAppListener() {
					@Override
					public void onChooseEmailApp(Intent intent) {
						try {
							startActivity(intent);
							finish();
						} catch (Exception e) {
							EditPreferencesFragment.saveEmailApp(QuickEmailToSelf.this,
									"", "");
							getEmailApp.addEmailAppToIntent(incomingIntent,
									getEmailApp.new OnChooseEmailAppListener() {
										@Override
										public void onChooseEmailApp(
												Intent intent) {
											try {
												startActivity(intent);
												finish();
											} catch (Exception e) {
												Toast.makeText(
														QuickEmailToSelf.this,
														"Error - please try again.",
														Toast.LENGTH_SHORT)
														.show();
											}
										}
									});
						}
					}
				});

		// try {
		// incomingIntent.setComponent(null);
		// PackageManager packageManger = getPackageManager();
		// ResolveInfo resolveInfo = packageManger.resolveActivity(
		// incomingIntent, PackageManager.MATCH_DEFAULT_ONLY);
		//
		// if (mAppPreferences.getBoolean("showDefaultAppWarning", true)
		// && resolveInfo.activityInfo.name
		// .equals("com.android.internal.app.ResolverActivity")) {
		// Toast.makeText(this, "no default", Toast.LENGTH_SHORT);
		// AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// builder.setMessage(
		// "Now select your email client. Select as default to avoid seeing this message again.")
		// .setCancelable(false)
		// .setPositiveButton("OK",
		// new DialogInterface.OnClickListener() {
		// public void onClick(DialogInterface dialog,
		// int id) {
		// startActivity(incomingIntent);
		// finish();
		// }
		// })
		// .setNegativeButton("Don't show this again",
		// new DialogInterface.OnClickListener() {
		// public void onClick(DialogInterface dialog,
		// int which) {
		// mAppPreferences
		// .edit()
		// .putBoolean(
		// "showDefaultAppWarning",
		// false).commit();
		// startActivity(incomingIntent);
		// finish();
		// }
		// });
		// builder.show();
		// } else {
		// startActivity(incomingIntent);
		// finish();
		// }
		// } catch (ActivityNotFoundException e) {
		// Toast.makeText(this, "No email clients setup", Toast.LENGTH_SHORT)
		// .show();
		// }
	}
}
