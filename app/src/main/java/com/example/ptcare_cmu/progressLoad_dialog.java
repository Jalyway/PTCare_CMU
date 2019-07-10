package com.example.ptcare_cmu;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

public class progressLoad_dialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder cdlog=new AlertDialog.Builder(getActivity());
        //
        LayoutInflater inflater=getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.loading_dialog, null);
        ProgressBar progressBar=view.findViewById(R.id.progressBar2);
        progressBar.setProgress(-1);
        cdlog.setView(view).setCancelable(false);
        return cdlog.create();
    }
}
