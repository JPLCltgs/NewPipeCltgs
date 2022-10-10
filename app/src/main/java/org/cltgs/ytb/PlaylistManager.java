package org.cltgs.ytb;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.work.PeriodicWorkRequest.Builder;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;


public final class PlaylistManager {
    private PlaylistManager() {

    }
    public static void ititializeWorker(final Context context) {
        //Permissions
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                final Intent getpermission = new Intent();
                getpermission.setAction(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                context.startActivity(getpermission);
            }
        }

        //Work builder
        final Builder myWorkBuilder =
                new Builder(PlaylistCreator.class, 4, TimeUnit.HOURS, 15,
                        TimeUnit.MINUTES);
        WorkManager.getInstance(context).cancelAllWorkByTag("PlaylistManager");
        final PeriodicWorkRequest myWork = myWorkBuilder.addTag("PlaylistManager").build();
        WorkManager.getInstance(context).enqueue(myWork);
    }
}
