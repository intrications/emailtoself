package uk.co.ashtonbrsc.emailtoself;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class GetEmailApp {

	GetEmailApp(Activity activity) {
		super();
		this.activity = activity;
		packageManager = activity.getPackageManager();
	}

	private PackageManager packageManager;
	private Activity activity;
	private OnChooseEmailAppListener listener;

	void addEmailAppToIntent(Intent intent,
							 OnChooseEmailAppListener listener) {

		this.listener = listener;

		String[] emailApp = EditPreferencesFragment.getEmailApp(activity);

		if (emailApp == null || emailApp[0].equals("")) {
			chooseDefaultProgram(intent);
		} else {
			intent.setClassName(emailApp[0], emailApp[1]);
			listener.onChooseEmailApp(intent);
		}
	}

	private void chooseDefaultProgram(final Intent intent) {
		try {
			// ResolveInfo resolveInfo = packageManager.resolveActivity(intent,
			// PackageManager.MATCH_DEFAULT_ONLY);
			// if (resolveInfo == null
			// || resolveInfo.activityInfo.name
			// .equals("com.android.internal.app.ResolverActivity")) {
			showAboutToChooseEmailAppDialog(intent);
			// }
		} catch (ActivityNotFoundException e) {
			Toast.makeText(activity, "No email clients setup",
					Toast.LENGTH_SHORT).show();
		}

	}

	private void showAboutToChooseEmailAppDialog(final Intent shareIntent) {
		AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(activity);
		builder.setTitle(activity.getString(R.string.app_name));
		builder.setMessage(
				"Select your email client. This can be changed in settings.")
				.setCancelable(false)
				.setPositiveButton("Continue",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								showListOfPossibleApps(shareIntent);
							}
						});
		builder.setCancelable(false);
		builder.show();
	}

	void showListOfPossibleApps(final Intent shareIntent) {
		final Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setType("text/plain");
		emailIntent.putExtra(Intent.EXTRA_EMAIL,
				new String[]{"test@nudgemail.com"});
		final List<ResolveInfo> resolved = packageManager
				.queryIntentActivities(emailIntent,
						PackageManager.MATCH_DEFAULT_ONLY);

		Iterator<ResolveInfo> iterator = resolved.iterator();

		while (iterator.hasNext()) {
			ResolveInfo thisResolveInfo = iterator.next();
			if (thisResolveInfo.activityInfo.packageName.equals(activity
					.getPackageName())) {
				iterator.remove();
			}
		}

		Collections.sort(resolved, new ResolveInfoComparator());

		ListView listView = new ListView(activity);
		listView.setAdapter(new AppsArrayAdapter(activity, 0, 0, resolved));

		final MaterialDialog dialog2 = new MaterialDialog.Builder(activity)
				.title("Choose Email App")
				.customView(listView, false)
				.build();
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
									int position, long arg3) {
				EditPreferencesFragment.saveEmailApp(activity,
						resolved.get(position).activityInfo.packageName,
						resolved.get(position).activityInfo.name);
				if (listener != null) {
					shareIntent.setClassName(
							resolved.get(position).activityInfo.packageName,
							resolved.get(position).activityInfo.name);
					listener.onChooseEmailApp(shareIntent);
				}
				dialog2.dismiss();
			}
		});
		dialog2.show();
	}

	class AppsArrayAdapter extends ArrayAdapter<ResolveInfo> {

		private List<ResolveInfo> resolved;
		private PackageManager packageManager;

		AppsArrayAdapter(Context context, int resource,
						 int textViewResourceId, List<ResolveInfo> resolved) {
			super(context, resource, textViewResourceId, resolved);
			packageManager = context.getPackageManager();
			this.resolved = resolved;
		}

		@Override
		public int getCount() {
			return resolved.size();
		}

		@Override
		public int getPosition(ResolveInfo item) {
			return super.getPosition(item);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inf = LayoutInflater.from(getContext());
			View view = inf.inflate(R.layout.app_list_row, parent, false);
			TextView textViewAppName = (TextView) view
					.findViewById(R.id.textViewAppName);
			textViewAppName.setText(resolved.get(position)
					.loadLabel(packageManager).toString());
			ImageView imageViewIcon = (ImageView) view
					.findViewById(R.id.imageViewIcon);
			imageViewIcon.setImageDrawable(resolved.get(position).activityInfo
					.loadIcon(packageManager));
			return view;
		}
	}

	private class ResolveInfoComparator implements Comparator<ResolveInfo> {

		@Override
		public int compare(ResolveInfo lhs, ResolveInfo rhs) {
			String lhsName = lhs.loadLabel(packageManager).toString();
			String rhsName = rhs.loadLabel(packageManager).toString();
			return lhsName.compareTo(rhsName);
		}

	}

	abstract class OnChooseEmailAppListener {

		public abstract void onChooseEmailApp(Intent intent);

	}

}
