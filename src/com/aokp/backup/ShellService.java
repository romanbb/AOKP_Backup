package com.aokp.backup;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import eu.chainfire.libsuperuser.Shell;


public class ShellService extends IntentService {

    public static final String ACTION_COMMAND_SU = "com.aokp.backup.ShellService.ACTION_COMMAND_SU";
    public static final String ACTION_COMMAND_SH = "com.aokp.backup.ShellService.ACTION_COMMAND_SH";

    public static void su(Context c, String command) {
        runCommand(c, command, true);
    }

    public static void sh(Context c, String command) {
        runCommand(c, command, false);
    }

    public static void runCommand(Context c, String command, boolean superuser) {
        Intent run = new Intent(c, ShellService.class);
        run.setAction(superuser ? ACTION_COMMAND_SU : ACTION_COMMAND_SU);
        run.putExtra("command", command);
        c.startService(run);
    }

    public ShellService() {
        super("ShellService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
            final String action = intent.getAction();
        if (action == null || action.isEmpty())
            return;

        if (intent.hasExtra("command")) {
            final String command = intent.getStringExtra("command");
            if (ACTION_COMMAND_SU.equals(action)) {
                if (Shell.SU.available())
                    Shell.SU.run(command);
            } else if (ACTION_COMMAND_SH.equals(action)) {
                Shell.SH.run(command);
            }
        }
    }
}
