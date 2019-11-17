/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.example.background;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import com.example.background.workers.BlurWorker;
import com.example.background.workers.CleanupWorker;
import com.example.background.workers.SaveImageToFileWorker;

import java.util.List;

import static com.example.background.Constants.IMAGE_MANIPULATION_WORK_NAME;
import static com.example.background.Constants.KEY_IMAGE_URI;
import static com.example.background.Constants.TAG_OUTPUT;

public class BlurViewModel extends AndroidViewModel {
    // New instance variable for the WorkInfo
    private LiveData<List<WorkInfo>> mSavedWorkInfo;
    // New instance variable for the WorkInfo
    private Uri mOutputUri;

    private Uri mImageUri;
    private WorkManager mWorkManager;

    public BlurViewModel(@NonNull Application application) {
        super(application);
        mWorkManager = WorkManager.getInstance(application);

        //In the BlurViewModel constructor
        mSavedWorkInfo = mWorkManager.getWorkInfosByTagLiveData(TAG_OUTPUT);
    }

    // Add a getter method for mSavedWorkInfo
    LiveData<List<WorkInfo>> getOutputWorkInfo() {
        return mSavedWorkInfo;
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    //With using blurLevel
    void applyBlur(int blurLevel) {
        // Add WorkRequest to Cleanup temporary images
        // REPLACE THIS CODE:
//        WorkContinuation continuation = mWorkManager.beginWith(OneTimeWorkRequest.from(CleanupWorker.class));
        // WITH
        WorkContinuation continuation = mWorkManager
                .beginUniqueWork(IMAGE_MANIPULATION_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequest.from(CleanupWorker.class));

        // Add WorkRequests to blur the image the number of times requested
        for (int i = 0; i < blurLevel; i++) {
            OneTimeWorkRequest.Builder blurBuilder =
                    new OneTimeWorkRequest.Builder(BlurWorker.class);

            // Input the Uri if this is the first blur operation
            // After the first blur operation the input will be the output of previous
            // blur operations.
            if ( i == 0 ) {
                blurBuilder.setInputData(createInputDataForUri());
            }

            continuation = continuation.then(blurBuilder.build());
        }


        // Create charging constraint
        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true)
                .build();


        // Add WorkRequest to save the image to the filesystem
        OneTimeWorkRequest save = new OneTimeWorkRequest.Builder(SaveImageToFileWorker.class)
                .setConstraints(constraints) // This adds the Constraints
                .addTag(TAG_OUTPUT) // This adds the tag
                .build();
        continuation = continuation.then(save);

        // Actually start the work
        continuation.enqueue();
    }

    //Without using blurLevel
    void applyBlur2(int blurLevel) {
/*
        WorkContinuation continuation = mWorkManager.beginWith(workA);

        continuation.then(workB) // FYI, then() returns a new WorkContinuation instance
                .then(workC)
                .enqueue(); // Enqueues the WorkContinuation which is a chain of work
        */

        // Add WorkRequest to Cleanup temporary images
        WorkContinuation continuation = mWorkManager.beginWith(OneTimeWorkRequest.from(CleanupWorker.class));

        // Add WorkRequest to blur the image
        OneTimeWorkRequest blurRequest = new OneTimeWorkRequest.Builder(BlurWorker.class)
                .setInputData(createInputDataForUri())
                .build();
        continuation = continuation.then(blurRequest);

        // Add WorkRequest to save the image to the filesystem
        OneTimeWorkRequest save =
                new OneTimeWorkRequest.Builder(SaveImageToFileWorker.class)
                        .build();
        continuation = continuation.then(save);

        // Actually start the work
        continuation.enqueue();
    }

    //Not using blurLevel
    void applyBlur1(int blurLevel) {
//        mWorkManager.enqueue(OneTimeWorkRequest.from(BlurWorker.class));

        OneTimeWorkRequest blurRequest =
                new OneTimeWorkRequest.Builder(BlurWorker.class)
                        .setInputData(createInputDataForUri())
                        .build();

        mWorkManager.enqueue(blurRequest);
    }

    private Uri uriOrNull(String uriString) {
        if (!TextUtils.isEmpty(uriString)) {
            return Uri.parse(uriString);
        }
        return null;
    }

    /**
     * Setters
     */
    void setImageUri(String uri) {
        mImageUri = uriOrNull(uri);
    }

    /**
     * Getters
     */
    Uri getImageUri() {
        return mImageUri;
    }

    /**
     * Creates the input data bundle which includes the Uri to operate on
     * @return Data which contains the Image Uri as a String
     */
    private Data createInputDataForUri() {
        Data.Builder builder = new Data.Builder();
        if (mImageUri != null) {
            builder.putString(KEY_IMAGE_URI, mImageUri.toString());
        }
        return builder.build();
    }

    // Add a getter and setter for mOutputUri
    void setOutputUri(String outputImageUri) {
        mOutputUri = uriOrNull(outputImageUri);
    }

    Uri getOutputUri() { return mOutputUri; }

    /**
     * Cancel work using the work's unique name
     */
    void cancelWork() {
        mWorkManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME);
    }

}