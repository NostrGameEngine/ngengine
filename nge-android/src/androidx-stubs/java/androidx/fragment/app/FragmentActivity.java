/**
 * Copyright (c) 2025, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */

package androidx.fragment.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.view.Display;

public class FragmentActivity extends Context{
    public void runOnUiThread(Runnable r){

    }

    public void finish(){

    }

    @Override
    public boolean bindService(Intent arg0, ServiceConnection arg1, int arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'bindService'");
    }

    @Override
    public int checkCallingOrSelfPermission(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkCallingOrSelfPermission'");
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri arg0, int arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkCallingOrSelfUriPermission'");
    }

    @Override
    public int checkCallingPermission(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkCallingPermission'");
    }

    @Override
    public int checkCallingUriPermission(Uri arg0, int arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkCallingUriPermission'");
    }

    @Override
    public int checkPermission(String arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkPermission'");
    }

    @Override
    public int checkSelfPermission(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkSelfPermission'");
    }

    @Override
    public int checkUriPermission(Uri arg0, int arg1, int arg2, int arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkUriPermission'");
    }

    @Override
    public int checkUriPermission(Uri arg0, String arg1, String arg2, int arg3, int arg4, int arg5) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkUriPermission'");
    }

    @Override
    public void clearWallpaper() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'clearWallpaper'");
    }

    @Override
    public Context createConfigurationContext(Configuration arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createConfigurationContext'");
    }

    @Override
    public Context createContextForSplit(String arg0) throws NameNotFoundException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createContextForSplit'");
    }

    @Override
    public Context createDeviceProtectedStorageContext() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createDeviceProtectedStorageContext'");
    }

    @Override
    public Context createDisplayContext(Display arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createDisplayContext'");
    }

    @Override
    public Context createPackageContext(String arg0, int arg1) throws NameNotFoundException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createPackageContext'");
    }

    @Override
    public String[] databaseList() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'databaseList'");
    }

    @Override
    public boolean deleteDatabase(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteDatabase'");
    }

    @Override
    public boolean deleteFile(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteFile'");
    }

    @Override
    public boolean deleteSharedPreferences(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteSharedPreferences'");
    }

    @Override
    public void enforceCallingOrSelfPermission(String arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'enforceCallingOrSelfPermission'");
    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri arg0, int arg1, String arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'enforceCallingOrSelfUriPermission'");
    }

    @Override
    public void enforceCallingPermission(String arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'enforceCallingPermission'");
    }

    @Override
    public void enforceCallingUriPermission(Uri arg0, int arg1, String arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'enforceCallingUriPermission'");
    }

    @Override
    public void enforcePermission(String arg0, int arg1, int arg2, String arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'enforcePermission'");
    }

    @Override
    public void enforceUriPermission(Uri arg0, int arg1, int arg2, int arg3, String arg4) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'enforceUriPermission'");
    }

    @Override
    public void enforceUriPermission(Uri arg0, String arg1, String arg2, int arg3, int arg4, int arg5,
            String arg6) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'enforceUriPermission'");
    }

    @Override
    public String[] fileList() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'fileList'");
    }

    @Override
    public Context getApplicationContext() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getApplicationContext'");
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getApplicationInfo'");
    }

    @Override
    public AssetManager getAssets() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAssets'");
    }

    @Override
    public File getCacheDir() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCacheDir'");
    }

    @Override
    public ClassLoader getClassLoader() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getClassLoader'");
    }

    @Override
    public File getCodeCacheDir() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCodeCacheDir'");
    }

    @Override
    public ContentResolver getContentResolver() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getContentResolver'");
    }

    @Override
    public File getDataDir() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDataDir'");
    }

    @Override
    public File getDatabasePath(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDatabasePath'");
    }

    @Override
    public File getDir(String arg0, int arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDir'");
    }

    @Override
    public File getExternalCacheDir() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getExternalCacheDir'");
    }

    @Override
    public File[] getExternalCacheDirs() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getExternalCacheDirs'");
    }

    @Override
    public File getExternalFilesDir(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getExternalFilesDir'");
    }

    @Override
    public File[] getExternalFilesDirs(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getExternalFilesDirs'");
    }

    @Override
    public File[] getExternalMediaDirs() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getExternalMediaDirs'");
    }

    @Override
    public File getFileStreamPath(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFileStreamPath'");
    }

    @Override
    public File getFilesDir() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFilesDir'");
    }

    @Override
    public Looper getMainLooper() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMainLooper'");
    }

    @Override
    public File getNoBackupFilesDir() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getNoBackupFilesDir'");
    }

    @Override
    public File getObbDir() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getObbDir'");
    }

    @Override
    public File[] getObbDirs() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getObbDirs'");
    }

    @Override
    public String getPackageCodePath() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPackageCodePath'");
    }

    @Override
    public PackageManager getPackageManager() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPackageManager'");
    }

    @Override
    public String getPackageName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPackageName'");
    }

    @Override
    public String getPackageResourcePath() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPackageResourcePath'");
    }

    @Override
    public Resources getResources() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getResources'");
    }

    @Override
    public SharedPreferences getSharedPreferences(String arg0, int arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSharedPreferences'");
    }

    @Override
    public Object getSystemService(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSystemService'");
    }

    @Override
    public String getSystemServiceName(Class<?> arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSystemServiceName'");
    }

    @Override
    public Theme getTheme() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTheme'");
    }

    @Override
    public Drawable getWallpaper() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getWallpaper'");
    }

    @Override
    public int getWallpaperDesiredMinimumHeight() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getWallpaperDesiredMinimumHeight'");
    }

    @Override
    public int getWallpaperDesiredMinimumWidth() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getWallpaperDesiredMinimumWidth'");
    }

    @Override
    public void grantUriPermission(String arg0, Uri arg1, int arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'grantUriPermission'");
    }

    @Override
    public boolean isDeviceProtectedStorage() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isDeviceProtectedStorage'");
    }

    @Override
    public boolean moveDatabaseFrom(Context arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'moveDatabaseFrom'");
    }

    @Override
    public boolean moveSharedPreferencesFrom(Context arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'moveSharedPreferencesFrom'");
    }

    @Override
    public FileInputStream openFileInput(String arg0) throws FileNotFoundException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'openFileInput'");
    }

    @Override
    public FileOutputStream openFileOutput(String arg0, int arg1) throws FileNotFoundException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'openFileOutput'");
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String arg0, int arg1, CursorFactory arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'openOrCreateDatabase'");
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String arg0, int arg1, CursorFactory arg2,
            DatabaseErrorHandler arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'openOrCreateDatabase'");
    }

    @Override
    public Drawable peekWallpaper() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'peekWallpaper'");
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver arg0, IntentFilter arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'registerReceiver'");
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver arg0, IntentFilter arg1, int arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'registerReceiver'");
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver arg0, IntentFilter arg1, String arg2, Handler arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'registerReceiver'");
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver arg0, IntentFilter arg1, String arg2, Handler arg3,
            int arg4) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'registerReceiver'");
    }

    @Override
    public void removeStickyBroadcast(Intent arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeStickyBroadcast'");
    }

    @Override
    public void removeStickyBroadcastAsUser(Intent arg0, UserHandle arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeStickyBroadcastAsUser'");
    }

    @Override
    public void revokeUriPermission(Uri arg0, int arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'revokeUriPermission'");
    }

    @Override
    public void revokeUriPermission(String arg0, Uri arg1, int arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'revokeUriPermission'");
    }

    @Override
    public void sendBroadcast(Intent arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendBroadcast'");
    }

    @Override
    public void sendBroadcast(Intent arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendBroadcast'");
    }

    @Override
    public void sendBroadcastAsUser(Intent arg0, UserHandle arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendBroadcastAsUser'");
    }

    @Override
    public void sendBroadcastAsUser(Intent arg0, UserHandle arg1, String arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendBroadcastAsUser'");
    }

    @Override
    public void sendOrderedBroadcast(Intent arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendOrderedBroadcast'");
    }

    @Override
    public void sendOrderedBroadcast(Intent arg0, String arg1, BroadcastReceiver arg2, Handler arg3, int arg4,
            String arg5, Bundle arg6) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendOrderedBroadcast'");
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent arg0, UserHandle arg1, String arg2, BroadcastReceiver arg3,
            Handler arg4, int arg5, String arg6, Bundle arg7) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendOrderedBroadcastAsUser'");
    }

    @Override
    public void sendStickyBroadcast(Intent arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendStickyBroadcast'");
    }

    @Override
    public void sendStickyBroadcastAsUser(Intent arg0, UserHandle arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendStickyBroadcastAsUser'");
    }

    @Override
    public void sendStickyOrderedBroadcast(Intent arg0, BroadcastReceiver arg1, Handler arg2, int arg3,
            String arg4, Bundle arg5) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendStickyOrderedBroadcast'");
    }

    @Override
    public void sendStickyOrderedBroadcastAsUser(Intent arg0, UserHandle arg1, BroadcastReceiver arg2,
            Handler arg3, int arg4, String arg5, Bundle arg6) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendStickyOrderedBroadcastAsUser'");
    }

    @Override
    public void setTheme(int arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTheme'");
    }

    @Override
    public void setWallpaper(Bitmap arg0) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setWallpaper'");
    }

    @Override
    public void setWallpaper(InputStream arg0) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setWallpaper'");
    }

    @Override
    public void startActivities(Intent[] arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startActivities'");
    }

    @Override
    public void startActivities(Intent[] arg0, Bundle arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startActivities'");
    }

    @Override
    public void startActivity(Intent arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startActivity'");
    }

    @Override
    public void startActivity(Intent arg0, Bundle arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startActivity'");
    }

    @Override
    public ComponentName startForegroundService(Intent arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startForegroundService'");
    }

    @Override
    public boolean startInstrumentation(ComponentName arg0, String arg1, Bundle arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startInstrumentation'");
    }

    @Override
    public void startIntentSender(IntentSender arg0, Intent arg1, int arg2, int arg3, int arg4)
            throws SendIntentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startIntentSender'");
    }

    @Override
    public void startIntentSender(IntentSender arg0, Intent arg1, int arg2, int arg3, int arg4, Bundle arg5)
            throws SendIntentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startIntentSender'");
    }

    @Override
    public ComponentName startService(Intent arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startService'");
    }

    @Override
    public boolean stopService(Intent arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stopService'");
    }

    @Override
    public void unbindService(ServiceConnection arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unbindService'");
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unregisterReceiver'");
    }
}
