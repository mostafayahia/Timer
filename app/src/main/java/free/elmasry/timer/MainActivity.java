/*
 * Copyright (C) 2018 Yahia H. El-Tayeb
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


/*
 * simple application to play holy Quran verses then it closes itself automatically after certain time
 */
package free.elmasry.timer;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity implements OnClickListener, OnCompletionListener,
        AdapterView.OnItemSelectedListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int MAX_NUM_KEY_FOR_LIST_ITEM_SURA_NO = 70;

    // we add another path to work for android 6.0
    private String mSheikhDirPath, mSheikhDirPath2;

    // this media player object plays mp3 from the android device itself
    private MediaPlayer mMediaPlayer;

    // this media player object plays mp3 from the res/raw folder
    private MediaPlayer mResMediaPlayer;

    private TextView mErrorView;
    private Button mPlayButton;
    private TextView mTimerView;
    private SharedPreferences mPref;
    private CheckBox mContinueAfterLastVerseCheckBox;
    private ArrayList<Integer> mSuraNoList;
    private View[] mSuraListLayouts;

    private static final int INVALID_VALUE = -100;

    // convert minutes to milliseconds
    private static final int TIMER_STEP_IN_MILLI = 10 * 60 * 1000;
    private static final int TIMER_UPPER_LIMIT_IN_MILLI = 120 * 60 * 1000;
    private static final int TIMER_LOWER_LIMIT_IN_MILLI = 10 * 60 * 1000;

    private Spinner mChooseSuraSpinner;
    private Spinner mAddSuraSpinner;
    private EditText mStartFromView;
    private int mCurrentSuraNo = INVALID_VALUE;
    private int mOldSuraNo = INVALID_VALUE;
    private int mStartFromNo = INVALID_VALUE;

    private static final int STATE_READY_TO_PLAY = 100;
    private static final int STATE_READY_TO_PAUSE = 101;

    private int mPlayButtonState = STATE_READY_TO_PLAY;
    private int mNextVerseNum = 1;
    private boolean mIsSura1Verse1Played = false;
    private int mLastVerseNum = INVALID_VALUE;
    private int mLastVerseDuration;

    private int mCurrentPlayingListItemIndex = INVALID_VALUE;

    // ==== DON'T MODIFY THESE VALUES =======
    private static final String PREF_NAME = "last_state";
    private static final String TIMER_DURATION_IN_MIN_KEY = "timer_duration";
    private static final String SURA_NO_KEY = "sura_no";
    private static final String START_FROM_KEY = "start_from";
    private static final String CLOSE_AFTER_SURA_KEY = "close_after_sura";
    private static final String CONTINUE_SURA_NO_KEY = "CSNKEY";
    private static final String CONTINUE_VERSE_NO_KEY = "CVNKEY";
    private static final String CONTINUE_AFTER_LAST_VERSE_KEY = "CALVKEY";
    private static final String CURRENT_PLAYING_LIST_ITEM_INDEX_KEY = "CPLIIKEY";

    // this is so important to be able to stop sound from playing when the user
    // press pause button IN THE INSTANT OF the verse is already finished and switch to another one
    private boolean mIsNoSoundState = true;

    // make a global variable to save checking close after sura check box many times
    private boolean mCloseAfterList;

    private int mElapsedTime;
    private int mEndTime;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // set root view for this activity
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);
        mSuraNoList = new ArrayList<>();

        // we play only the voice of Alafasy in this version of the application
        mSheikhDirPath = Environment.getExternalStorageDirectory().getPath() + "/quran_android/audio/5";
        // we add another path to work for android 6.0
        mSheikhDirPath2 = "/mnt/m_internal_storage/quran_android/audio/5";

        // get views from the root views of this activity
        mChooseSuraSpinner = (Spinner) findViewById(R.id.choose_sura_spinner);
        mAddSuraSpinner = (Spinner) findViewById(R.id.add_sura_spinner);
        mStartFromView = (EditText) findViewById(R.id.start_from_verse_edittext);
        mTimerView = (TextView) findViewById(R.id.timer_textview);
        mErrorView = (TextView) findViewById(R.id.err_msg_textview);
        mErrorView.setVisibility(View.GONE);
        CheckBox closeAfterListCheckBox = (CheckBox) findViewById(R.id.close_after_list_checkbox);
        mContinueAfterLastVerseCheckBox =
                (CheckBox) findViewById(R.id.continue_after_last_verse_checkbox);
        Button increaseButton = (Button) findViewById(R.id.increase_button);
        Button decreaseButton = (Button) findViewById(R.id.decrease_button);

        // get buttons from the root view of this activity
        mPlayButton = (Button) findViewById(R.id.play_button);

        // setting parameters for the choose sura spinner
        String[] chooseSuraArray = getResources().getStringArray(R.array.choose_sura_spinner_list);
        ArrayAdapter<String> chooseSuraAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chooseSuraArray);
        mChooseSuraSpinner.setAdapter(chooseSuraAdapter);
        String[] addSuraArray = getResources().getStringArray(R.array.add_sura_spinner_list);
        ArrayAdapter<String> addSuraAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, addSuraArray);
        mAddSuraSpinner.setAdapter(addSuraAdapter);
        mAddSuraSpinner.setOnItemSelectedListener(this);


        // set event handlers' for these buttons
        mPlayButton.setOnClickListener(this);
        increaseButton.setOnClickListener(this);
        decreaseButton.setOnClickListener(this);
        closeAfterListCheckBox.setOnClickListener(this);
        mContinueAfterLastVerseCheckBox.setOnClickListener(this);

        // restore last state
        mPref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        restoreLastState();

        // get end time in case first time application launched
        String endTimeStr = mTimerView.getText().toString();
        // NOTE: you have to convert the time from minutes to milliseconds
        mEndTime = Integer.parseInt(endTimeStr) * 60 * 1000;

        // prevent automatic soft keyboard from showing when launching the app
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );

        // set the cursor at the end of the text in the start from view
        mStartFromView.setSelection(mStartFromView.getText().toString().length());

        // regarding sura list
        mSuraListLayouts = new View[]{
                findViewById(R.id.sura_list_item_0),
                findViewById(R.id.sura_list_item_1),
                findViewById(R.id.sura_list_item_2),
                findViewById(R.id.sura_list_item_3),
                findViewById(R.id.sura_list_item_4),
                findViewById(R.id.sura_list_item_5),
                findViewById(R.id.sura_list_item_6)
        };
        setClickListenersForSuraListItemLayoutChildren();
        hideAndClearAllSuraListLayouts();
        if (mSuraNoList.size() == 0) {
            mAddSuraSpinner.setVisibility(View.GONE);
        }
        updateSuraListLayouts();

    }

    private void setClickListenersForSuraListItemLayoutChildren() {
        for (int i = 0; i < mSuraListLayouts.length; i++) {
            Button removeButton = (Button) mSuraListLayouts[i].findViewById(R.id.sura_list_item_remove_button);
            Button playButton = (Button) mSuraListLayouts[i].findViewById(R.id.sura_list_item_play_button);
            Button pauseButton = (Button) mSuraListLayouts[i].findViewById(R.id.sura_list_item_pause_button);
            removeButton.setOnClickListener(this);
            playButton.setOnClickListener(this);
            pauseButton.setOnClickListener(this);
        }
    }

    private void hideAndClearAllSuraListLayouts() {
        for (int i = 0; i < mSuraListLayouts.length; i++) {
            // IMPORTANT: read this -> https://stackoverflow.com/questions/4787008/how-to-access-button-inside-include-layout
            mSuraListLayouts[i].setVisibility(View.GONE);
            ((TextView) mSuraListLayouts[i].findViewById(R.id.sura_list_item_sura_text_view))
                    .setText("");
        }
    }

    private void updateSuraListLayouts() {

        if (mSuraNoList.size() == 0) return;

        hideAndClearAllSuraListLayouts();
        makeAllLayoutsInSuraListLayoutsUnSelected();
        Collections.sort(mSuraNoList);

        for (int i = 0; i < mSuraNoList.size(); i++) {
            View suraListItemLayout = mSuraListLayouts[i];
            suraListItemLayout.setVisibility(View.VISIBLE);

            int listItemSuraNo = mSuraNoList.get(i);

            ((TextView) suraListItemLayout.findViewById(R.id.sura_list_item_sura_text_view))
                    .setText(Utility.getSuraName(this, listItemSuraNo));

            if (i == mCurrentPlayingListItemIndex) {
                makeLayoutInSuraListLayoutsSelected(i);
            }
        }

    }

    private void makeLayoutInSuraListLayoutsSelected(int selectedLayoutIndex) {

        View suraListItemLayout = mSuraListLayouts[selectedLayoutIndex];
        int listItemLayoutId = getListItemLayoutIdFromIndex(selectedLayoutIndex);

        suraListItemLayout.findViewById(R.id.sura_list_item_remove_button).setVisibility(View.INVISIBLE);

        if ((mResMediaPlayer != null && mResMediaPlayer.isPlaying()) ||
                (mMediaPlayer != null && mMediaPlayer.isPlaying())) {
            showListItemPauseButton(listItemLayoutId);
        } else {
            showListItemPlayButton(listItemLayoutId);
        }

        ((TextView) suraListItemLayout.findViewById(R.id.sura_list_item_sura_text_view))
                .setTypeface(null, Typeface.BOLD);
    }

    private void makeAllLayoutsInSuraListLayoutsUnSelected() {
        for (int i = 0; i < mSuraListLayouts.length; i++) {
            View suraListItemLayout = mSuraListLayouts[i];

            suraListItemLayout.findViewById(R.id.sura_list_item_play_button).setVisibility(View.VISIBLE);
            suraListItemLayout.findViewById(R.id.sura_list_item_remove_button).setVisibility(View.VISIBLE);
            suraListItemLayout.findViewById(R.id.sura_list_item_pause_button).setVisibility(View.INVISIBLE);

            ((TextView) suraListItemLayout.findViewById(R.id.sura_list_item_sura_text_view))
                    .setTypeface(null, Typeface.NORMAL);
        }
    }


    private void restoreLastState() {
        int timerDuration = mPref.getInt(TIMER_DURATION_IN_MIN_KEY, TIMER_STEP_IN_MILLI / 1000 / 60);
        // setting choose sura spinner to zero will set the spinner to the first item on it
        int suraNo = mPref.getInt(SURA_NO_KEY, 0);
        int startFromNo = mPref.getInt(START_FROM_KEY, 1);
        setSuraAndVerseViews(suraNo, startFromNo);
        mTimerView.setText(Integer.toString(timerDuration));

        mCloseAfterList = mPref.getBoolean(CLOSE_AFTER_SURA_KEY, false);
        ((CheckBox) findViewById(R.id.close_after_list_checkbox)).setChecked(mCloseAfterList);

        boolean continueAfterLastVerse = mPref.getBoolean(CONTINUE_AFTER_LAST_VERSE_KEY, false);
        mContinueAfterLastVerseCheckBox.setChecked(continueAfterLastVerse);
        handleContinueAfterLastVerse();

        int continueSuraNo = mPref.getInt(CONTINUE_SURA_NO_KEY, INVALID_VALUE);
        int continueVerseNo = mPref.getInt(CONTINUE_VERSE_NO_KEY, INVALID_VALUE);
        if (continueSuraNo == INVALID_VALUE || INVALID_VALUE == continueVerseNo) {
            continueOptionHideViews();
        }

        closeAfterListOptionAdjustViews();

        // === AFTER ADDING SURA PLAYLIST FEATURE ===
        for (int i = 0; i < MAX_NUM_KEY_FOR_LIST_ITEM_SURA_NO; i++) {
            int listItemSuraNo = mPref.getInt(getListItemSuraNoKey(i), INVALID_VALUE);
            if (listItemSuraNo == INVALID_VALUE) break;
            else mSuraNoList.add(listItemSuraNo);
        }
        mCurrentPlayingListItemIndex = mPref.getInt(CURRENT_PLAYING_LIST_ITEM_INDEX_KEY, INVALID_VALUE);
        if (mSuraNoList.size() > 1)
            findViewById(R.id.continue_after_last_verse_layout).setVisibility(View.GONE);
    }

    private void saveLastState() {

        Log.d(LOG_TAG, "saveLastState() called");

        Editor ed = mPref.edit();

        boolean continueAfterLastVerse = mContinueAfterLastVerseCheckBox.isChecked();
        if (mSuraNoList.size() == 1) // old behavior for the app
            ed.putBoolean(CONTINUE_AFTER_LAST_VERSE_KEY, continueAfterLastVerse);
        else
            ed.putBoolean(CONTINUE_AFTER_LAST_VERSE_KEY, true); // we want to always continue after last verse

        if (!continueAfterLastVerse && mSuraNoList.size() == 1) { // old behavior for the app
            saveSuraAndVerse();
        }

        // detect if the user changed the values of sura and verse
        // after checking continue after last verse option
        continueOptionHandleMismatchIfExist();

        // converting from Milliseconds to minutes
        ed.putInt(TIMER_DURATION_IN_MIN_KEY, mEndTime / 60 / 1000);

        ed.putBoolean(CLOSE_AFTER_SURA_KEY, mCloseAfterList);

        // values regrading continue listening after last verse of previous session
        // the condition handle the case if the user access to the app then exit without playing
        // any sound
        if (mCurrentSuraNo != INVALID_VALUE && mLastVerseNum != INVALID_VALUE) {
            int continueSuraNo = mCurrentSuraNo;
            int continueVerseNo = mNextVerseNum;

            /*
             * to handle the case if the activity is destroyed but the verse wasn't completed
             */
            if (mMediaPlayer != null) {
                int playedPercent = mMediaPlayer.getCurrentPosition() * 100 / mMediaPlayer.getDuration();
                if (playedPercent < 70 && mNextVerseNum > 1) {
                    // in this case we continue from last verse that wasn't listened completely
                    continueVerseNo = mNextVerseNum - 1;
                }
            }

            if (continueVerseNo > mLastVerseNum) {
                if (mSuraNoList.size() == 1) { // old behavior of the app
                    continueSuraNo++;
                } else {
                    if (++mCurrentPlayingListItemIndex >= mSuraNoList.size())
                        mCurrentPlayingListItemIndex = 0;
                    continueSuraNo = mSuraNoList.get(mCurrentPlayingListItemIndex);
                }
                continueVerseNo = 1;
            }

            if (continueSuraNo > 114) continueSuraNo = 1;

            ed.putInt(CONTINUE_SURA_NO_KEY, continueSuraNo);
            ed.putInt(CONTINUE_VERSE_NO_KEY, continueVerseNo);
        }

        // === AFTER ADDING SURA PLAYLIST FEATURE ===
        for (int i = 0; i < MAX_NUM_KEY_FOR_LIST_ITEM_SURA_NO; i++) {
            if (i < mSuraNoList.size())
                ed.putInt(getListItemSuraNoKey(i), mSuraNoList.get(i));
            else
                ed.putInt(getListItemSuraNoKey(i), INVALID_VALUE);
        }
        ed.putInt(CURRENT_PLAYING_LIST_ITEM_INDEX_KEY, mCurrentPlayingListItemIndex);

        // SO IMPORTANT WITHOUT IT NO THING HAPPEN (SAVED)
        ed.commit();
    }

    /**
     * save sura and verse from the views of the app
     */
    private void saveSuraAndVerse() {
        Editor ed = mPref.edit();

        // we get this values from the views because it initialized as member values with invalid values
        String startFromNoStr = mStartFromView.getText().toString();
        String suraNoStr = Integer.toString(mChooseSuraSpinner.getSelectedItemPosition());

        ed.putInt(START_FROM_KEY, Integer.parseInt(startFromNoStr));
        ed.putInt(SURA_NO_KEY, Integer.parseInt(suraNoStr));

        // SO IMPORTANT WITHOUT IT NO THING HAPPEN (SAVED)
        ed.commit();
    }


    @Override
    protected void onDestroy() {
        // You MUST save state here to save last verse played before destroying this activity
        // you MUST also save state before releasing the resources
        saveLastState();

        // must be executed before super.onDestroy()
        // release unneeded resources
        releaseAllMediaPlayers();

        super.onDestroy();
    }

    /**
     * handlers for buttons clicks in this activity
     */
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.play_button:
                // hide soft keyboard if shown
                // Check if no view has focus:
                View view = this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

                handlePlayButton();
                break;

            case R.id.increase_button: {
                int endTime = mEndTime + TIMER_STEP_IN_MILLI;
                if (endTime <= TIMER_UPPER_LIMIT_IN_MILLI) {
                    mEndTime = endTime;
                    // convert from milliseconds to minutes for the user display
                    mTimerView.setText(Integer.toString(mEndTime / 60 / 1000));
                } else {
                    Utility.showToastMessage(this, R.string.msg_cannot_exceed);
                }
            }
            break;


            case R.id.decrease_button: {
                int endTime = mEndTime - TIMER_STEP_IN_MILLI;
                if (endTime >= TIMER_LOWER_LIMIT_IN_MILLI) {
                    mEndTime = endTime;
                    // convert from milliseconds to minutes for the user display
                    mTimerView.setText(Integer.toString(mEndTime / 60 / 1000));
                } else {
                    Utility.showToastMessage(this, R.string.msg_cannot_decrease);
                }
            }
            break;

            case R.id.close_after_list_checkbox:
                // saving it in the variable to not access check box many times unnecessary
                mCloseAfterList = ((CheckBox) findViewById(R.id.close_after_list_checkbox)).isChecked();
                closeAfterListOptionAdjustViews();
                break;

            case R.id.continue_after_last_verse_checkbox:
                handleContinueAfterLastVerse();
                break;

            case R.id.sura_list_item_remove_button: {
                View parent = (LinearLayout) v.getParent();
                handleSuraListItemRemoveButton(parent.getId());
            }
            break;

            case R.id.sura_list_item_play_button: {
                // we want out layout not frame layout which the button inside
                View parent = (LinearLayout) v.getParent().getParent();
                handleListItemPlayButton(parent.getId());
            }
            break;

            case R.id.sura_list_item_pause_button: {
                // we want out layout not frame layout which the button inside
                View parent = (LinearLayout) v.getParent().getParent();
                handleListItemPauseButton(parent.getId());
            }
            break;
        }

    }

    private void handleListItemPlayButton(int parentLayoutId) {

        int listItemLayoutIndex = getIndexFromListItemLayoutId(parentLayoutId);

        if (mResMediaPlayer == null && mMediaPlayer == null &&
                listItemLayoutIndex == mCurrentPlayingListItemIndex) {
            mCurrentPlayingListItemIndex = listItemLayoutIndex;

            findViewById(R.id.play_button).setVisibility(View.GONE);
            findViewById(R.id.edit_sura_layout).setVisibility(View.GONE);

            int suraNo = mSuraNoList.get(listItemLayoutIndex);

            int startFromNo = Integer.parseInt(mStartFromView.getText().toString());

            if (!isPathExist(suraNo, startFromNo)) return; // IMPORTANT: No point for continue

            makeNewStart(suraNo, startFromNo);

        } else if (listItemLayoutIndex != mCurrentPlayingListItemIndex) {

            findViewById(R.id.play_button).setVisibility(View.GONE);
            findViewById(R.id.edit_sura_layout).setVisibility(View.GONE);

            int suraNo = mSuraNoList.get(listItemLayoutIndex);

            if (!isPathExist(suraNo, 1)) return; // IMPORTANT: No point for continue

            mCurrentPlayingListItemIndex = listItemLayoutIndex;

            makeNewStart(suraNo, 1);

        } else if (mResMediaPlayer != null) {
            mResMediaPlayer.start();

        } else {
            mMediaPlayer.start();
        }

        mIsNoSoundState = false;

        updateSuraListLayouts();

        showListItemPauseButton(parentLayoutId);
    }

    private void handleListItemPauseButton(int parentLayoutId) {
        mIsNoSoundState = true;
        showListItemPlayButton(parentLayoutId);
        if (mResMediaPlayer != null)
            mResMediaPlayer.pause();
        else
            mMediaPlayer.pause();
    }

    private void showListItemPauseButton(int parentLayoutId) {
        int listItemLayoutIndex = getIndexFromListItemLayoutId(parentLayoutId);
        View listItemLayout = mSuraListLayouts[listItemLayoutIndex];

        listItemLayout.findViewById(R.id.sura_list_item_play_button).setVisibility(View.INVISIBLE);
        listItemLayout.findViewById(R.id.sura_list_item_pause_button).setVisibility(View.VISIBLE);
    }

    private void showListItemPlayButton(int parentLayoutId) {
        int listItemLayoutIndex = getIndexFromListItemLayoutId(parentLayoutId);
        View listItemLayout = mSuraListLayouts[listItemLayoutIndex];

        listItemLayout.findViewById(R.id.sura_list_item_play_button).setVisibility(View.VISIBLE);
        listItemLayout.findViewById(R.id.sura_list_item_pause_button).setVisibility(View.INVISIBLE);
    }

    private void handleSuraListItemRemoveButton(int parentLayoutId) {

        int listItemLayoutIndex = getIndexFromListItemLayoutId(parentLayoutId);
        mSuraNoList.remove(listItemLayoutIndex);

        if (listItemLayoutIndex < mCurrentPlayingListItemIndex)
            mCurrentPlayingListItemIndex--;

        updateSuraListLayouts();

        if (mSuraNoList.size() < mSuraListLayouts.length)
            mAddSuraSpinner.setVisibility(View.VISIBLE);
    }


    private int getIndexFromListItemLayoutId(int listItemLayoutId) {

        switch (listItemLayoutId) {
            case R.id.sura_list_item_0:
                return 0;
            case R.id.sura_list_item_1:
                return 1;
            case R.id.sura_list_item_2:
                return 2;
            case R.id.sura_list_item_3:
                return 3;
            case R.id.sura_list_item_4:
                return 4;
            case R.id.sura_list_item_5:
                return 5;
            case R.id.sura_list_item_6:
                return 6;
            default:
                throw new IllegalArgumentException("undefined list item layout id: " + listItemLayoutId);

        }
    }

    private int getListItemLayoutIdFromIndex(int index) {
        switch (index) {
            case 0:
                return R.id.sura_list_item_0;
            case 1:
                return R.id.sura_list_item_1;
            case 2:
                return R.id.sura_list_item_2;
            case 3:
                return R.id.sura_list_item_3;
            case 4:
                return R.id.sura_list_item_4;
            case 5:
                return R.id.sura_list_item_5;
            case 6:
                return R.id.sura_list_item_6;
            default:
                throw new IllegalArgumentException("index out of boundary max: " + (mSuraListLayouts.length - 1) +
                        ", but given: " + index);
        }
    }

    private void handlePlayButton() {
        switch (mPlayButtonState) {
            // we toggle the state  after pressing the user the button
            case STATE_READY_TO_PAUSE: {
                mIsNoSoundState = true;
                if (mResMediaPlayer != null)
                    mResMediaPlayer.pause();
                else
                    mMediaPlayer.pause();
                mPlayButtonState = STATE_READY_TO_PLAY;
                mPlayButton.setText(getString(R.string.label_play));
                break;
            }
            case STATE_READY_TO_PLAY: {
                if (acceptFieldsData()) {
                    int suraNo = mChooseSuraSpinner.getSelectedItemPosition();
                    int startFromNo = Integer.parseInt(mStartFromView.getText().toString());

                    if (!isPathExist(suraNo, startFromNo))
                        return; // IMPORTANT: No point for continue


                    continueOptionHideViews();

                    // detect if the user changed the values of sura and verse
                    // after checking continue after last verse option
                    continueOptionHandleMismatchIfExist();

                    // detect if the user change in edit texts' values or not
                    if (suraNo != mOldSuraNo || startFromNo != mStartFromNo) {
                        // if there is change we start playing as the application is launched again
                        makeNewStart(suraNo, startFromNo);

                        // ====AFTER ADDING SURA PLAYLIST FEATURE=========
                        int index = mSuraNoList.indexOf(suraNo);
                        if (index < 0) /* Not exist in sura list */ {
                            if (mSuraNoList.size() == mSuraListLayouts.length)
                                mSuraNoList.remove(mSuraNoList.size() - 1); // removing last item
                            mSuraNoList.add(suraNo);
                            Collections.sort(mSuraNoList);
                        }
                        index = mSuraNoList.indexOf(suraNo);
                        mCurrentPlayingListItemIndex = index;
                        findViewById(R.id.play_button).setVisibility(View.GONE);
                        handleListItemPlayButton(getListItemLayoutIdFromIndex(index));
                        mAddSuraSpinner.setVisibility(View.VISIBLE);
                    } else {
                        // if there is no change we will resume what have been played before the button pressed
                        // NOTE: mOldSuraNo and mStartFromNo have valid values now so the user
                        // was listening to the verses before
                        if (mResMediaPlayer != null)
                            mResMediaPlayer.start();
                        else
                            mMediaPlayer.start();
                    }
                    mIsNoSoundState = false;
                    findViewById(R.id.edit_sura_layout).setVisibility(View.GONE);

                } else {
                    showErrorMessage(R.string.msg_err_incorrect_inputs);
                    // SO IMPORTANT >> no point for continue
                    return;
                }

                mPlayButtonState = STATE_READY_TO_PAUSE;
                mPlayButton.setText(getString(R.string.label_stop));
                break;
            }
        }
    }

    private void makeNewStart(int suraNo, int startFromNo) {
        releaseAllMediaPlayers();
        mOldSuraNo = suraNo;
        mStartFromNo = startFromNo;
        // we save it in a member variable to save the process of search in the map for every time we use
        mLastVerseNum = Utility.SURA_NUM_VERSES_NUM_MAP.get(mOldSuraNo);
        mCurrentSuraNo = mOldSuraNo;
        mNextVerseNum = mStartFromNo;
        mIsSura1Verse1Played = false;
        mErrorView.setVisibility(View.GONE);
        setResMediaPlayer(R.raw.al_estaza);
        mResMediaPlayer.start();
    }

    /**
     * @param suraNo
     * @param startFromNo
     * @return true if the the path for suraNo and startFromNo exists in the device
     */
    private boolean isPathExist(int suraNo, int startFromNo) {
        String path = mSheikhDirPath + "/" + suraNo + "/" + startFromNo + ".mp3";
        // usually the Quran Android application download the first verse of first sura by DEFAULT
        // so we need another check to make sure the first sura was downloaded when the user choose it
        // and want to play it
        if (suraNo == 1) path = mSheikhDirPath + "/" + suraNo + "/" + 2 + ".mp3";

        if (!Utility.isFileExist(path)) {
            // we use another path to be compatible with android 6
            path = path.replace(mSheikhDirPath, mSheikhDirPath2);
            if (!Utility.isFileExist(path)) {
                showErrorMessage(R.string.msg_err_download_sura);
                return false;
            } else {
                // now we sure mSheikhDirPath2 is the correct one
                mSheikhDirPath = mSheikhDirPath2;
                return true;
            }
        }

        return true;
    }

    private void handleContinueAfterLastVerse() {
        int continueSuraNo = mPref.getInt(CONTINUE_SURA_NO_KEY, INVALID_VALUE);
        int continueVerseNo = mPref.getInt(CONTINUE_VERSE_NO_KEY, INVALID_VALUE);

        if (mContinueAfterLastVerseCheckBox.isChecked()) {
            // before updating first saving last values
            // for sura and verse (may be the user entered these values)
            // the next line for convenient if the user entered new values then checked the box
            // then unchecked the box, he will get last values he entered
            saveSuraAndVerse();
            setSuraAndVerseViews(continueSuraNo, continueVerseNo);
        } else { // not checked (changing from checked to unchecked state)
            // if the user changed the continue values we set for verse and sura
            // then keep every thing as it is and do nothing (No need for updating the views)
            // the next 4 lines for convenient because in this case the user already chose
            // what he wants
            int suraNoFromVw = mChooseSuraSpinner.getSelectedItemPosition();
            int startFromNoFromVw = Integer.parseInt(mStartFromView.getText().toString());
            if (continueSuraNo != suraNoFromVw || continueVerseNo != startFromNoFromVw)
                return; //no point to continue

            // updating the views related to sura and verse
            int suraNo = mPref.getInt(SURA_NO_KEY, 0);
            int startFromNo = mPref.getInt(START_FROM_KEY, 1);
            setSuraAndVerseViews(suraNo, startFromNo);
        }
    }

    /**
     * check if sura number edit text and start from edit text
     * have a number entered by user or not and sura number must
     * be less than or equal 114
     *
     * @return true if the edit texts are not empty and accepted data
     */
    private boolean acceptFieldsData() {
        String suraNoStr = Integer.toString(mChooseSuraSpinner.getSelectedItemPosition());
        String startFromStr = mStartFromView.getText().toString();
        if (suraNoStr.length() == 0 || startFromStr.length() == 0)
            return false;

        int suraNo = Integer.parseInt(suraNoStr);
        int startFromNo = Integer.parseInt(startFromStr);
        if (suraNo < 1 || suraNo > 114 || startFromNo < 1
                || startFromNo > Utility.SURA_NUM_VERSES_NUM_MAP.get(suraNo))
            return false;
        return true;
    }

    /**
     * handle playing next verse taking into consideration playing first verse of first sura if needed
     */
    private void playNextVerse() {
        if (mNextVerseNum == 1 && mCurrentSuraNo != 1 && mCurrentSuraNo != 9 && !mIsSura1Verse1Played) {
            setResMediaPlayer(R.raw.sura1_verse1);
            // we want to make sure the application doesn't close directly after playing the previous verse
            if (mResMediaPlayer.getDuration() + mElapsedTime < mEndTime) {
                mResMediaPlayer.start();
                mLastVerseDuration = mResMediaPlayer.getDuration();
            } else {
                finish();
                return;
            }
            mIsSura1Verse1Played = true;
        } else {

            // this code to handle lollipop issue
            // this is very bad to memory to do that but I try to solve the issue as much as I can
            // when resetting mMediaPlayer the same lollipop issue appear so we destroy the object
            // and create a new one
            if (Utility.isLollipop()) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.release();
                }
                mMediaPlayer = null; // to enforce to initialize it again in the next block of code
            }

            if (mMediaPlayer == null) {
                // setting and create our media player for playing mp3 from the android device
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setLooping(false);
                mMediaPlayer.setOnCompletionListener(this);

                // solving the problem in Lollipop only, the playback cuts out
                // the current verse before finishing then moving to the next one when the
                // screen off
                handleLollipopIssue();
            }

            mMediaPlayer.reset();
            try {
                String path = mSheikhDirPath + "/" + mCurrentSuraNo + "/" + mNextVerseNum++ + ".mp3";
                mMediaPlayer.setDataSource(path);
                mMediaPlayer.prepare();
            } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
                // VERY IMPORTANT TO ADD THIS PERMISSION TO MAKE THE APP WORK FOR ANDROID 6
                // https://stackoverflow.com/questions/8854359/exception-open-failed-eacces-permission-denied-on-android
                Log.e("Timer", e.getMessage());

                // this code executes usually if not all verses of the sura are downloaded (user downloads only verses per page using
                // Quran android settings)
                finish();
                // no point from continue
                return;
            }

            mMediaPlayer.start();
            mLastVerseDuration = mMediaPlayer.getDuration();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // IMPORTANT
        // NOTE: after insert the next 4 lines when the user press stop at the END of the verse and then resume
        // the sound it repeats the last played verse
        // HINT: to solve this problem try to use Log.v(LOG_TAG, "before mMediaPlayer.start()") and ...after.. and so on tell to figure out solution
        if (mIsNoSoundState) {
            // SO IMPORTANT >> no point for continue
            return;
        }

        if (mResMediaPlayer != null) {
            mResMediaPlayer.release();
            mResMediaPlayer = null;
        }

        if (!mCloseAfterList) {
            mElapsedTime += mLastVerseDuration;

            if (mElapsedTime > mEndTime) {
                // number of sura nazaat verses 46
                if (mNextVerseNum != 1 && mLastVerseNum <= 46 && mNextVerseNum <= mLastVerseNum) {
                    playNextVerse();
                } else {
                    finish();
                    return;
                }
            } else if (mNextVerseNum <= mLastVerseNum) {
                playNextVerse();
            } else if (mSuraNoList.size() == 1 && mCurrentSuraNo < 114) { // old behavior for the app
                // increase sura number
                mCurrentSuraNo++;

                if (!playNewSura(mCurrentSuraNo)) return; // IMPORTANT: No point to continue

                // === AFTER ADDING PLAY SURA LIST FEATURE ===
                mSuraNoList.remove(0);
                mSuraNoList.add(mCurrentSuraNo);
                updateSuraListLayouts();

            } else if (mSuraNoList.size() == 1) { // old behavior for the app
                finish();
                return;
            } else {
                // === AFTER ADDING SURA PLAYLIST FEATURE ===
                if (mCurrentPlayingListItemIndex == mSuraNoList.size() - 1) {
                    mCurrentPlayingListItemIndex = 0;
                    mCurrentSuraNo = mSuraNoList.get(0);
                } else {
                    mCurrentSuraNo = mSuraNoList.get(++mCurrentPlayingListItemIndex);
                }

                if (!playNewSura(mCurrentSuraNo)) return; // IMPORTANT: No point to continue

                updateSuraListLayouts();
            }
        } else {
            if (mNextVerseNum <= mLastVerseNum) {
                playNextVerse();
            } else if (mSuraNoList.size() == 1) { // old behavior for the app
                finish();
                return;
            } else {
                // === AFTER ADDING SURA PLAYLIST FEATURE ===
                if (mCurrentPlayingListItemIndex == mSuraNoList.size() - 1) {
                    // resetting some fields for the next time the app opens
                    mCurrentPlayingListItemIndex = 0;
                    mCurrentSuraNo = mSuraNoList.get(0);
                    mNextVerseNum = 1;

                    finish();
                    return;
                } else {
                    mCurrentSuraNo = mSuraNoList.get(++mCurrentPlayingListItemIndex);
                    if (!playNewSura(mCurrentSuraNo)) return; // IMPORTANT: No point to continue
                    updateSuraListLayouts();
                }
            }
        }
    }

    /**
     * @param suraNo
     * @return return true if the sura mp3 files exist in the device
     */
    private boolean playNewSura(int suraNo) {
        mLastVerseNum = Utility.SURA_NUM_VERSES_NUM_MAP.get(suraNo);
        // reset some fields to its default value
        mIsSura1Verse1Played = false;
        mNextVerseNum = 1;

        String path = mSheikhDirPath + "/" + suraNo + "/" + mNextVerseNum + ".mp3";
        if (Utility.isFileExist(path)) {
            playNextVerse();
            return true;
        } else {
            String suraName = Utility.getSuraName(this, suraNo);
            Toast.makeText(this, getString(R.string.sura_not_downloaded, suraName),
                    Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
    }

    /**
     * quick setting and playing for media player for playing mp3 from res/raw folder
     *
     * @param resid resource id for playing the sound
     */
    private void setResMediaPlayer(int resid) {
        mResMediaPlayer = MediaPlayer.create(this, resid);
        mResMediaPlayer.setLooping(false);
        mResMediaPlayer.setOnCompletionListener(this);
    }

    /**
     * show error message in error view exist in this application and make it visible
     *
     * @param resid reference to xml string which you want to display
     */
    private void showErrorMessage(int resid) {
        mErrorView.setText(getString(resid));
        mErrorView.setVisibility(View.VISIBLE);
    }

    /**
     * release all media players resources used in this application
     */
    private void releaseAllMediaPlayers() {
        if (mResMediaPlayer != null) {
            mResMediaPlayer.release();
            mResMediaPlayer = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     * disappearing and appearing views based on the user choice for closing the
     * app after finishing sura or not
     */
    private void closeAfterListOptionAdjustViews() {

        Button increaseButton = (Button) findViewById(R.id.increase_button);
        Button decreaseButton = (Button) findViewById(R.id.decrease_button);
        TextView closeAppAfterStrView = (TextView) findViewById(R.id.close_app_after_str_textview);

        if (mCloseAfterList) {
            increaseButton.setVisibility(View.GONE);
            decreaseButton.setVisibility(View.GONE);
            closeAppAfterStrView.setVisibility(View.GONE);
            mTimerView.setVisibility(View.GONE);
        } else {
            increaseButton.setVisibility(View.VISIBLE);
            decreaseButton.setVisibility(View.VISIBLE);
            closeAppAfterStrView.setVisibility(View.VISIBLE);
            mTimerView.setVisibility(View.VISIBLE);
        }

    }

    /**
     * hide views related to continue after last verse option
     */
    private void continueOptionHideViews() {
        findViewById(R.id.continue_after_last_verse_layout).setVisibility(View.GONE);
    }

    /**
     * set values for sura and verse in the app activity
     */
    private void setSuraAndVerseViews(int suraNo, int startFromNo) {
        mChooseSuraSpinner.setSelection(suraNo);
        mStartFromView.setText(Integer.toString(startFromNo));
        // set the cursor at the end of the text in the start from view
        mStartFromView.setSelection(mStartFromView.getText().toString().length());
    }

    /**
     * handling the case of the user changes the values of sura and verse
     * after checking the continue after last verse option
     */
    private void continueOptionHandleMismatchIfExist() {
        if (mContinueAfterLastVerseCheckBox.isChecked()) {
            int suraNo = mChooseSuraSpinner.getSelectedItemPosition();
            int startFromNo = Integer.parseInt(mStartFromView.getText().toString());

            int continueSuraNo = mPref.getInt(CONTINUE_SURA_NO_KEY, INVALID_VALUE);
            int continueVerseNo = mPref.getInt(CONTINUE_VERSE_NO_KEY, INVALID_VALUE);
            // if there is change, we will save the last values entered by the user
            if (suraNo != continueSuraNo || startFromNo != continueVerseNo)
                saveSuraAndVerse();
        }
    }

    /**
     * solving the problem in Lollipop only, the playback cuts out
     * the current verse before finishing it then moving to the next one when the
     * screen off
     */
    private void handleLollipopIssue() {

        if (!Utility.isLollipop()) {
            return;
        }

        mMediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // this handle item click in the add sura spinner only
        if (position > 0) {
            mSuraNoList.add(position);
            Collections.sort(mSuraNoList);

            // next code IMPORTANT because current playing list item index may be changed.
            if (position < mCurrentSuraNo) mCurrentPlayingListItemIndex++;

            updateSuraListLayouts();
            mAddSuraSpinner.setSelection(0);
            if (mSuraNoList.size() == mSuraListLayouts.length)
                mAddSuraSpinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public String getListItemSuraNoKey(int index) {
        if (index >= MAX_NUM_KEY_FOR_LIST_ITEM_SURA_NO)
            throw new IllegalArgumentException("index exceeds the max index (" +
                    (MAX_NUM_KEY_FOR_LIST_ITEM_SURA_NO - 1) + ") the given: ");
        // DON'T MODIFY THE NEXT STRING TO MAKE RESTORE LAST STATE CORRECT
        return "list_item_" + index + "_sura_no_key";
    }

    /**
     * Checks if the app has permission to write to device storage
     * <p>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
