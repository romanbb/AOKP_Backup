package com.aokp.backup.backup;

import android.content.Context;

import java.io.File;
import java.io.IOException;

/**
 * Created by roman on 9/8/13.
 */
public class JBMR2Backup extends JBMR1Backup {
    public JBMR2Backup(Context c, String name) {
        super(c, name);
    }

    public JBMR2Backup(Context c, File zip) throws IOException {
        super(c, zip);
    }
}
