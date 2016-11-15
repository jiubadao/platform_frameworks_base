/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.content.pm;

import static android.net.TrafficStats.MB_IN_BYTES;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IStorageManager;
import android.test.AndroidTestCase;
import android.util.Log;

import com.android.internal.content.PackageHelper;

public class PackageHelperTests extends AndroidTestCase {
    private static final boolean localLOGV = true;
    public static final String TAG = "PackageHelperTests";
    protected final String PREFIX = "android.content.pm";
    private IStorageManager mSm;
    private String fullId;
    private String fullId2;

    private IStorageManager getSm() {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return IStorageManager.Stub.asInterface(service);
        } else {
            Log.e(TAG, "Can't get mount service");
        }
        return null;
    }

    private void cleanupContainers() throws RemoteException {
        Log.d(TAG,"cleanUp");
        IStorageManager sm = getSm();
        String[] containers = sm.getSecureContainerList();
        for (int i = 0; i < containers.length; i++) {
            if (containers[i].startsWith(PREFIX)) {
                Log.d(TAG,"cleaing up "+containers[i]);
                sm.destroySecureContainer(containers[i], true);
            }
        }
    }

    void failStr(String errMsg) {
        Log.w(TAG, "errMsg=" + errMsg);
        fail(errMsg);
    }

    void failStr(Exception e) {
        failStr(e.getMessage());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (localLOGV) Log.i(TAG, "Cleaning out old test containers");
        cleanupContainers();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (localLOGV) Log.i(TAG, "Cleaning out old test containers");
        cleanupContainers();
    }

    public void testMountAndPullSdCard() {
        try {
            fullId = PREFIX;
            fullId2 = PackageHelper.createSdDir(1024 * MB_IN_BYTES, fullId, "none",
                    android.os.Process.myUid(), true);

            Log.d(TAG,PackageHelper.getSdDir(fullId));
            PackageHelper.unMountSdDir(fullId);

            Runnable r1 = getMountRunnable();
            Runnable r2 = getDestroyRunnable();
            Thread thread = new Thread(r1);
            Thread thread2 = new Thread(r2);
            thread2.start();
            thread.start();
        } catch (Exception e) {
            failStr(e);
        }
    }

    public Runnable getMountRunnable() {
        Runnable r = new Runnable () {
            public void run () {
                try {
                    Thread.sleep(5);
                    String path = PackageHelper.mountSdDir(fullId, "none",
                            android.os.Process.myUid());
                    Log.e(TAG, "mount done " + path);
                } catch (IllegalArgumentException iae) {
                    throw iae;
                } catch (Throwable t) {
                    Log.e(TAG, "mount failed", t);
                }
            }
        };
        return r;
    }

    public Runnable getDestroyRunnable() {
        Runnable r = new Runnable () {
            public void run () {
                try {
                    PackageHelper.destroySdDir(fullId);
                    Log.e(TAG, "destroy done: " + fullId);
                } catch (Throwable t) {
                    Log.e(TAG, "destroy failed", t);
                }
            }
        };
        return r;
    }
}
