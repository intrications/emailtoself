package uk.co.ashtonbrsc.emailtoself;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.materialdialogs.AlertDialogWrapper;

public class EditPreferences extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (this.getIntent().getExtras() != null) {
			String noEmail = this.getIntent().getExtras().getString("noemail");
			if (noEmail != null && noEmail.equalsIgnoreCase("true")) {
				AlertDialogWrapper.Builder dialog = new AlertDialogWrapper.Builder(
						EditPreferences.this);
				dialog.setTitle("Email To Self");
				dialog.setMessage("Please set your email address. When finished press the back button.");
				dialog.setNeutralButton("Ok",
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();

							}
						});
				dialog.show();
			}
		}

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().
					replace(android.R.id.content, new EditPreferencesFragment()).commit();
		}

	}

//    @Override
//    protected void onActivityResult(final int requestCode,
//                                    final int resultCode, final Intent data) {
//        if (requestCode == PICK_ACCOUNT_REQUEST && resultCode == RESULT_OK) {
//            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
//            Toast.makeText(this, "Account Name=" + accountName, Toast.LENGTH_SHORT).show();
//        }
//    }
}
