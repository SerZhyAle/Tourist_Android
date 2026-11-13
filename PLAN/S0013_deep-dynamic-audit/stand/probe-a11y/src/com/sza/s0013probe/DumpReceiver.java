package com.sza.s0013probe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * `am broadcast -n com.sza.s0013probe/.DumpReceiver [--ez all true]` writes files/dump.xml and answers
 * "ok <bytes>" as the broadcast result data; "no-service" when the accessibility service is not bound.
 */
public class DumpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ProbeService service = ProbeService.instance;
        if (service == null) {
            setResultData("no-service");
            return;
        }
        try {
            String xml = service.dump(intent.getBooleanExtra("all", false));
            File out = new File(context.getFilesDir(), "dump.xml");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(xml.getBytes(StandardCharsets.UTF_8));
            }
            setResultData("ok " + out.length());
        } catch (Exception e) {
            setResultData("error " + e.getClass().getSimpleName());
        }
    }
}
