/*
 * Copyright (C) 2012 Roman Birg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aokp.backup;

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class BackupDialog extends DialogFragment {

    boolean[] cats;

    static BackupDialog newInstance(boolean[] checkedCats) {
        BackupDialog d = new BackupDialog();
        d.cats = checkedCats;
        return d;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        getDialog().setTitle("Name your backup"); // TODO make it a string
        getDialog().setCanceledOnTouchOutside(false);

        View v = inflater.inflate(R.layout.backup_dialog, container, false);
        EditText name = (EditText) v.findViewById(R.id.save_name);
        Button cancel = (Button) v.findViewById(R.id.cancel);
        Button set = (Button) v.findViewById(R.id.save);

        Date date = new Date();
        java.text.DateFormat dateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity());
        String currentDate = dateFormat.format(date);
        currentDate = currentDate.replace("/", "-");
        name.setText(currentDate);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });

        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newName = ((EditText) getDialog().findViewById(R.id.save_name))
                        .getText()
                        .toString();
                backup(newName);
                getDialog().dismiss();
            }
        });
        return v;
    }

    public void backup(String name) {
        new WorkTask(getActivity(), cats, name).execute();
    }

    public class WorkTask extends AsyncTask<Void, Boolean, Boolean> {

        AlertDialog d;
        Activity context;
        Backup b;
        String name;

        public WorkTask(Activity context, boolean[] cats, String name) {
            this.context = context;
            this.name = name;
            b = new Backup(context, cats);

        }

        @Override
        protected void onPreExecute() {
            d = new AlertDialog.Builder(context)
                    .setMessage("Backup in progress")
                    .create();
            d.show();
        }

        protected Boolean doInBackground(Void... files) {
            return b.backupSettings(name);
        }

        protected void onPostExecute(Boolean result) {
            if (d != null) {
                Toast.makeText(context.getApplicationContext(), result ? "Backup successful!"
                        : "Backup failed!!!!", Toast.LENGTH_SHORT).show();
                d.dismiss();
            }
        }
    }

}
