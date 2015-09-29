package com.ashish.geofenceworkhours;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class ResetDialog extends DialogFragment
{
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		return new AlertDialog.Builder(getActivity())
			.setTitle(R.string.reset_message)
			.setPositiveButton(
				android.R.string.ok, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						((OnResetConfirmedListener)getActivity()).onResetConfirmed(true);
					}
				})
			.setNegativeButton(android.R.string.cancel, null)
			.create();
	}

	public interface OnResetConfirmedListener
	{
		void onResetConfirmed(boolean confirmed);
	}
}
