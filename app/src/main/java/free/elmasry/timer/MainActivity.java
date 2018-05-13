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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;


public class MainActivity extends Activity implements OnClickListener, OnCompletionListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

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

    private static final int INVALID_VALUE = -100;

    // convert minutes to milliseconds
    private static final int TIMER_STEP_IN_MILLI = 10 * 60 * 1000;
    private static final int TIMER_UPPER_LIMIT_IN_MILLI = 70 * 60 * 1000;
    private static final int TIMER_LOWER_LIMIT_IN_MILLI = 10 * 60 * 1000;

    private Spinner mChooseSuraView;
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

    // ==== DON'T MODIFY THESE VALUES =======
    private static final String PREF_NAME = "last_state";
    private static final String TIMER_DURATION_IN_MIN_KEY = "timer_duration";
    private static final String SURA_NO_KEY = "sura_no";
    private static final String START_FROM_KEY = "start_from";
    private static final String CLOSE_AFTER_SURA_KEY = "close_after_sura";
    private static final String CONTINUE_SURA_NO_KEY = "CSNKEY";
    private static final String CONTINUE_VERSE_NO_KEY = "CVNKEY";
    private static final String CONTINUE_AFTER_LAST_VERSE_KEY = "CALVKEY";

    // this is so important to be able to stop sound from playing when the user
    // press pause button IN THE INSTANT OF the verse is already finished and switch to another one
    private boolean mIsNoSoundState = true;

    // make a global variable to save checking close after sura check box many times
    private boolean mCloseAfterSura;

    private int mElapsedTime;
    private int mEndTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // set root view for this activity
        setContentView(R.layout.activity_main);

        // we play only the voice of Alafasy in this version of the application
        mSheikhDirPath = Environment.getExternalStorageDirectory().getPath() + "/quran_android/audio/5";
        // we add another path to work for android 6.0
        mSheikhDirPath2 = "/mnt/m_internal_storage/quran_android/audio/5";

        // get views from the root views of this activity
        mChooseSuraView = (Spinner) findViewById(R.id.choose_sura_spinner);
        mStartFromView = (EditText) findViewById(R.id.start_from_verse_edittext);
        mTimerView = (TextView) findViewById(R.id.timer_textview);
        mErrorView = (TextView) findViewById(R.id.err_msg_textview);
        mErrorView.setVisibility(View.GONE);
        CheckBox closeAfterSuraCheckBox = (CheckBox) findViewById(R.id.close_after_sura_checkbox);
        mContinueAfterLastVerseCheckBox =
                (CheckBox) findViewById(R.id.continue_after_last_verse_checkbox);
        Button increaseButton = (Button) findViewById(R.id.increase_button);
        Button decreaseButton = (Button) findViewById(R.id.decrease_button);

        // get buttons from the root view of this activity
        mPlayButton = (Button) findViewById(R.id.play_button);

        // setting parameters for the choose sura spinner
        String[] surasNameArray = getResources().getStringArray(R.array.suras_names);
        ArrayAdapter<String> surasNamesAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, surasNameArray);
        mChooseSuraView.setAdapter(surasNamesAdapter);


        // set event handlers' for these buttons
        mPlayButton.setOnClickListener(this);
        increaseButton.setOnClickListener(this);
        decreaseButton.setOnClickListener(this);
        closeAfterSuraCheckBox.setOnClickListener(this);
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

    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();

        // You MUST also save state in onDestroy to save
        // last verse played before destroying this activity
        saveLastState();
    }


    private void restoreLastState() {
        int timerDuration = mPref.getInt(TIMER_DURATION_IN_MIN_KEY, TIMER_STEP_IN_MILLI / 1000 / 60);
        // setting choose sura spinner to zero will set the spinner to the first item on it
        int suraNo = mPref.getInt(SURA_NO_KEY, 0);
        int startFromNo = mPref.getInt(START_FROM_KEY, 1);
        setSuraAndVerseViews(suraNo, startFromNo);
        mTimerView.setText(Integer.toString(timerDuration));

        mCloseAfterSura = mPref.getBoolean(CLOSE_AFTER_SURA_KEY, false);
        ((CheckBox) findViewById(R.id.close_after_sura_checkbox)).setChecked(mCloseAfterSura);

        boolean continueAfterLastVerse = mPref.getBoolean(CONTINUE_AFTER_LAST_VERSE_KEY, false);
        mContinueAfterLastVerseCheckBox.setChecked(continueAfterLastVerse);
        handleContinueAfterLastVerse();

        int continueSuraNo = mPref.getInt(CONTINUE_SURA_NO_KEY, INVALID_VALUE);
        int continueVerseNo = mPref.getInt(CONTINUE_VERSE_NO_KEY, INVALID_VALUE);
        if (continueSuraNo == INVALID_VALUE || INVALID_VALUE == continueVerseNo) {
            continueOptionHideViews();
        }

        closeAfterSuraOptionAdjustViews();
    }

    private void saveLastState() {
        Editor ed = mPref.edit();

        boolean continueAfterLastVerse = mContinueAfterLastVerseCheckBox.isChecked();
        ed.putBoolean(CONTINUE_AFTER_LAST_VERSE_KEY, continueAfterLastVerse);

        if (!continueAfterLastVerse) {
            saveSuraAndVerse();
        }

        // detect if the user changed the values of sura and verse
        // after checking continue after last verse option
        continueOptionHandleMismatchIfExist();

        // converting from Milliseconds to minutes
        ed.putInt(TIMER_DURATION_IN_MIN_KEY, mEndTime / 60 / 1000);

        ed.putBoolean(CLOSE_AFTER_SURA_KEY, mCloseAfterSura);

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
                continueSuraNo++;
                continueVerseNo = 1;
            }
            if (continueSuraNo > 114) continueSuraNo = 1;
            ed.putInt(CONTINUE_SURA_NO_KEY, continueSuraNo);
            ed.putInt(CONTINUE_VERSE_NO_KEY, continueVerseNo);
        }

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
        String suraNoStr = Integer.toString(mChooseSuraView.getSelectedItemPosition());

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

            case R.id.close_after_sura_checkbox:
                // saving it in the variable to not access check box many times unnecessary
                mCloseAfterSura = ((CheckBox) findViewById(R.id.close_after_sura_checkbox)).isChecked();
                closeAfterSuraOptionAdjustViews();
                break;

            case R.id.continue_after_last_verse_checkbox:
                handleContinueAfterLastVerse();

                break;
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
                mPlayButton.setText(getString(R.string.str_play));
                break;
            }
            case STATE_READY_TO_PLAY: {
                if (acceptFieldsData()) {
                    int suraNo = mChooseSuraView.getSelectedItemPosition();
                    int startFromNo = Integer.parseInt(mStartFromView.getText().toString());
                    String path = mSheikhDirPath + "/" + suraNo + "/" + startFromNo + ".mp3";
                    // usually the Quran android application download the first verse of first sura by DEFAULT
                    // so we need another check to make sure the first sura is download when the user choose it
                    // and want to play it
                    if (suraNo == 1) path = mSheikhDirPath + "/" + suraNo + "/" + 2 + ".mp3";

                    if (!Utility.isFileExist(path)) {
                        // we use another path to be compatible with android 6
                        path = path.replace(mSheikhDirPath, mSheikhDirPath2);
                        if (!Utility.isFileExist(path)) {
                            showErrorMessage(R.string.msg_err_download_sura);
                            // SO IMPORTANT >> no point for continue
                            return;
                        } else {
                            // now we sure mSheikhDirPath2 is the correct one
                            mSheikhDirPath = mSheikhDirPath2;
                        }
                    }


                    continueOptionHideViews();

                    // detect if the user changed the values of sura and verse
                    // after checking continue after last verse option
                    continueOptionHandleMismatchIfExist();

                    // detect if the user change in edit texts' values or not
                    if (suraNo != mOldSuraNo || startFromNo != mStartFromNo) {
                        // if there is change we start playing as the application is launched again
                        releaseAllMediaPlayersResources();
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

                } else {
                    showErrorMessage(R.string.msg_err_incorrect_inputs);
                    // SO IMPORTANT >> no point for continue
                    return;
                }

                mPlayButtonState = STATE_READY_TO_PAUSE;
                mPlayButton.setText(getString(R.string.str_stop));
                break;
            }
        }
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
            int suraNoFromVw = mChooseSuraView.getSelectedItemPosition();
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
        String suraNoStr = Integer.toString(mChooseSuraView.getSelectedItemPosition());
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
            if (isLollipop()) {
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
                Log.w("Timer", e.getMessage());

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

        if (!mCloseAfterSura) {
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
            } else if (mCurrentSuraNo < 114) {
                // increase sura number
                mCurrentSuraNo++;
                mLastVerseNum = Utility.SURA_NUM_VERSES_NUM_MAP.get(mCurrentSuraNo);
                // reset some fields to its default value
                mIsSura1Verse1Played = false;
                mNextVerseNum = 1;

                String path = mSheikhDirPath + "/" + mCurrentSuraNo + "/" + mNextVerseNum + ".mp3";
                if (Utility.isFileExist(path)) {
                    playNextVerse();
                } else {
                    finish();
                    return;
                }
            } else {
                finish();
                return;
            }
        } else {
            if (mNextVerseNum <= mLastVerseNum) {
                playNextVerse();
            } else {
                finish();
                return;
            }
        }
    }


    // ======================= Helper Methods (NON CORE METHODS) ==========================================

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
    private void releaseAllMediaPlayersResources() {
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
    private void closeAfterSuraOptionAdjustViews() {

        Button increaseButton = (Button) findViewById(R.id.increase_button);
        Button decreaseButton = (Button) findViewById(R.id.decrease_button);
        TextView closeAppAfterStrView = (TextView) findViewById(R.id.close_app_after_str_textview);

        if (mCloseAfterSura) {
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
        mChooseSuraView.setSelection(suraNo);
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
            int suraNo = mChooseSuraView.getSelectedItemPosition();
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

        if (!isLollipop()) {
            return;
        }

        Log.d(LOG_TAG, "handleLollipop method is called");
        mMediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
    }



    private boolean isLollipop() {

        int apiLevel = Build.VERSION.SDK_INT;

        // targeting lollipop and lollipop_mr1
        return apiLevel == Build.VERSION_CODES.LOLLIPOP || apiLevel == 22;
    }

	/*package free.eltayeb.timer;



	import java.io.File;
	import java.io.IOException;

	import android.app.Activity;
	import android.content.Context;
	import android.content.SharedPreferences;
	import android.media.AudioManager;
	import android.media.MediaPlayer;
	import android.media.MediaPlayer.OnCompletionListener;
	import android.os.Bundle;
	import android.os.Environment;
	import android.widget.Button;
	import android.widget.TextView;
	import android.view.View;


	public class MainActivity extends Activity implements View.OnClickListener, OnCompletionListener {

		public static final short LIMIT = 111;
		private Button playBtn;
		private TextView suraNoTxt;
		private TextView startFromTxt;
		private MediaPlayer mediaPlayer;
		private String shiekhDirPath;
		private byte suraNo;
		private short counter, N;
		private int end;
		private SharedPreferences mPrefs;

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);
			mPrefs = getSharedPreferences("setting", Context.MODE_PRIVATE);
			loadView();
			initFields();
			playBtn.setOnClickListener(this);
			shiekhDirPath = Environment.getExternalStorageDirectory().getPath() + "/quran_android/audio/5";
		}

		@Override
		protected void onResume() {
			// TODO Auto-generated method stub
			super.onResume();
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setLooping(false);
		}

		@Override
		protected void onDestroy() {
			// TODO Auto-generated method stub
			super.onDestroy();
			if (mediaPlayer.isPlaying()) setPreferences(suraNo, counter - 1);
			else                         setPreferences(suraNo, counter);
			mediaPlayer.release();
			mediaPlayer = null;
		}

		private void play() {
			mediaPlayer.setOnCompletionListener(this);
			mediaPlayer.setVolume(0.25f, 0.25f);
			if (suraNo != 1 && suraNo !=9 && counter == 1)
				playVerse(mediaPlayer, shiekhDirPath + "/1/1.mp3");
			else
				playVerse(mediaPlayer, shiekhDirPath + "/" + suraNo + "/" + counter++ + ".mp3");
		}

		private void playVerse(MediaPlayer mp, String path) {
			mp.reset();
			try {
				mp.setDataSource(path);
				mp.prepare();
			} catch (IllegalArgumentException | SecurityException
					| IllegalStateException | IOException e) {
				e.printStackTrace();
			}

			mp.start();

		}

		@Override
		public void onCompletion(MediaPlayer mp) {
			// TODO Auto-generated method stub
			if (counter <= N && counter <= end)
				playVerse(mp, shiekhDirPath + "/" + suraNo + "/" + counter++ + ".mp3");
			else {
				if (counter > N) {
					counter = 1;
					if (suraNo < 114) suraNo += 1;
					else              suraNo  = 1;
				}
				finish();
			}
		}

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.playBtn:
				initFields();
				N   = (short) new File(shiekhDirPath + "/" + suraNo).list().length;
				end = counter + LIMIT - 1;

				play();
				break;
			}
		}


		private void loadView() {
			// TODO Auto-generated method stub
			playBtn      = (Button)   findViewById(R.id.playBtn);
			suraNoTxt    = (TextView) findViewById(R.id.suraNoTxt);
			startFromTxt = (TextView) findViewById(R.id.startFromTxt);
			startFromTxt.setText(String.valueOf(mPrefs.getInt("startFrom", 1)));
			suraNoTxt.setText(String.valueOf(mPrefs.getInt("suraNo", 1)));
		}

		private void setPreferences(int suraNo, int startFrom) {
			SharedPreferences.Editor ed = mPrefs.edit();
			ed.putInt("suraNo", suraNo);
			ed.putInt("startFrom", startFrom);
			ed.commit();
		}

		private void initFields() {
			suraNo  = Byte.parseByte(suraNoTxt.getText().toString());
			counter = Short.parseShort(startFromTxt.getText().toString()) ;
		}

	}
	 */

}
