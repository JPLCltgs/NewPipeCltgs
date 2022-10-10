package org.cltgs.ytb;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;


import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;

import androidx.work.OneTimeWorkRequest.Builder;
import androidx.work.WorkManager;



public final class PlaylistManager {
    private PlaylistManager() {

    }
    public static void ititializeWorker(final Context context) {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                final Intent getpermission = new Intent();
                getpermission.setAction(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                context.startActivity(getpermission);
            }
        }
        final Builder myWorkBuilder =
                new Builder(PlaylistCreator.class); //, 60, TimeUnit.SECONDS);
        WorkManager.getInstance(context).cancelAllWorkByTag("PlaylistManager");
        final OneTimeWorkRequest myWork = myWorkBuilder.build();
        WorkManager.getInstance(context)
                .enqueueUniqueWork("PlaylistManager",
                        ExistingWorkPolicy.KEEP, myWork);
    }


}
