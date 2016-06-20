package uk.co.ashtonbrsc.emailtoself;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.util.Arrays;

public class EmailToSelf extends AppCompatActivity {

	private ProgressDialog progressDialog;

	private String mWebPageTitle;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case WebPageTitleRequest.HANDLE_TITLE:
				progressDialog.dismiss();
				mWebPageTitle = (String) msg.obj;
				if (mWebPageTitle == null) {
					mWebPageTitle = "";
					Toast.makeText(EmailToSelf.this,
							R.string.webpage_title_could_not_be_retrieved,
							Toast.LENGTH_SHORT).show();
				} else {
					incomingIntent
							.putExtra(Intent.EXTRA_SUBJECT, mWebPageTitle);
				}
				startEmailIntent();
				break;
			}
		}
	};

	private boolean mRequestTitle;

	private SharedPreferences mAppPreferences;

	private Intent incomingIntent;

	private String mSharedText;

	private String mPrefix;

	private String mEmailAddress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAppPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		mAppPreferences.edit().putBoolean("showDefaultAppWarning", true)
				.apply();

		mRequestTitle = mAppPreferences.getBoolean("titleDownload", true);
		incomingIntent = getIntent();
		String action = incomingIntent.getAction();
		String type = incomingIntent.getType();

		if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
			mSharedText = incomingIntent.getStringExtra(Intent.EXTRA_TEXT);
			mWebPageTitle = incomingIntent.getStringExtra(Intent.EXTRA_SUBJECT);
		} else if (Intent.ACTION_SEND.equals(action)) {
			mSharedText = incomingIntent.getStringExtra(Intent.EXTRA_TEXT);
			// mImageUri =
			// incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM);
		} else if (Intent.ACTION_VIEW.equals(action)) { // when pretending to be
														// a browser
			mSharedText = incomingIntent.getDataString();
			incomingIntent.putExtra(Intent.EXTRA_TEXT, mSharedText);
			incomingIntent.setData(null);
		}

		if (mAppPreferences.getString("email", "").equals("")) {
			Intent prefIntent = new Intent(this, EditPreferences.class);
			prefIntent.putExtra("noemail", "true");
			startActivityForResult(prefIntent, 0);
		} else {
			requestTitleOrStartEmailIntent();
		}

	}

	protected void checkForEmptyEmailAddress() {
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

	private void requestTitle(String url) {
		final Thread webPageRequestThread = new Thread(new WebPageTitleRequest(
				url, mHandler));

		progressDialog = new ProgressDialog(EmailToSelf.this);
		progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE,
				getString(R.string.cancel),
				new ProgressDialog.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						webPageRequestThread.interrupt();

						startEmailIntent();

					}
				});
		progressDialog.setMessage(getString(R.string.retrieving_webpage_title));
		progressDialog.setIndeterminate(true);
		progressDialog.show();
		webPageRequestThread.start();

	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		requestTitleOrStartEmailIntent();
	}

	private void requestTitleOrStartEmailIntent() {
		if ((mWebPageTitle == null || "".equals(mWebPageTitle)) && mRequestTitle && mSharedText != null) {
			if (!mSharedText.endsWith("jpg")
					&& !mSharedText.endsWith("png")
					&& (mSharedText.startsWith("http") || mSharedText
							.startsWith("https"))) {
				requestTitle(mSharedText);
			} else {
				startEmailIntent();
			}
		} else {
			startEmailIntent();
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
			sendEmailIntent();
		} else { // This is possibly never called because prefrixes always at
					// least has "" as a value?
			sendEmailIntent();
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

	private void sendEmailIntent() {
		boolean squareBrackets = mAppPreferences.getBoolean("squareBrackets",
				true);
		StringBuilder subjectStringBuilder = new StringBuilder();
		if (mPrefix != null && mPrefix != "" && !mPrefix.equals("No Label")) {
			if (squareBrackets) {
				subjectStringBuilder.append("[");
				subjectStringBuilder.append(mPrefix);
				subjectStringBuilder.append("] ");
			} else {
				subjectStringBuilder.append(mPrefix).append(" ");
			}
			String incomingSubject = incomingIntent
					.getStringExtra(Intent.EXTRA_SUBJECT);
			if (incomingSubject != null) {
				subjectStringBuilder.append(incomingSubject);
			}
			incomingIntent.putExtra(Intent.EXTRA_SUBJECT,
					subjectStringBuilder.toString());
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
													// if (incomingIntent.)
			// incomingIntent.setAction(Intent.ACTION_SEND);
			// intent.setType("text/plain"); //this makes too many options come
			// up
			// incomingIntent.setType("message/rfc822");
			incomingIntent.putExtra(Intent.EXTRA_EMAIL,
					new String[] { mEmailAddress });
		} else {
			incomingIntent.setAction(Intent.ACTION_SENDTO);
			incomingIntent.setData(Uri.parse("mailto:" + mEmailAddress));
		}

		incomingIntent.setComponent(null);

		final GetEmailApp getEmailApp = new GetEmailApp(EmailToSelf.this);
		getEmailApp.addEmailAppToIntent(incomingIntent,
				getEmailApp.new OnChooseEmailAppListener() {
					@Override
					public void onChooseEmailApp(Intent intent) {
						try {
							startActivity(intent);
							finish();
						} catch (Exception e) {
							EditPreferencesFragment.saveEmailApp(EmailToSelf.this, "",
									"");
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
														EmailToSelf.this,
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
		// ResolveInfo resolveInfo =
		// packageManger.resolveActivity(incomingIntent,
		// PackageManager.MATCH_DEFAULT_ONLY);
		// if (mAppPreferences.getBoolean("showDefaultAppWarning", true)
		// && resolveInfo.activityInfo.name
		// .equals("com.android.internal.app.ResolverActivity")) {
		// Toast.makeText(this, "no default", Toast.LENGTH_SHORT);
		// AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// builder.setMessage(
		// "Now select your email client.\nSelect \"use as default\" to avoid seeing this message again.")
		// .setCancelable(false)
		// .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		// public void onClick(DialogInterface dialog, int id) {
		// startActivity(incomingIntent);
		// finish();
		// }
		// })
		// .setNegativeButton("Don't show this again",
		// new DialogInterface.OnClickListener() {
		// public void onClick(DialogInterface dialog, int which) {
		// mAppPreferences.edit()
		// .putBoolean("showDefaultAppWarning", false)
		// .commit();
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
		// Toast.makeText(this, "No email clients setup",
		// Toast.LENGTH_SHORT).show();
		// }
	}
}
