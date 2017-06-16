package rs.readahead.washington.mobile.views.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.FileUtil;


public class AudioRecordActivity extends AppCompatActivity {

    @BindView(R.id.record_audio)
    ImageButton mRecord;
    @BindView(R.id.play_audio)
    ImageButton mPlay;
    @BindView(R.id.stop_audio)
    ImageButton mStop;
    @BindView(R.id.evidence)
    AppCompatButton mEvidence;
    @BindView(R.id.audio_time)
    TextView mTimer;
    @BindView(R.id.audio_seek_bar)
    SeekBar mSeekBar;
    @BindView(R.id.recording_progress)
    ProgressBar mProgressBar;
    @BindView(R.id.recording_info)
    TextView mInfo;

    private MediaRecorder myAudioRecorder;
    private String outputFile = null;
    private Context context = AudioRecordActivity.this;
    private Handler durationHandler;
    private double timeElapsed = 0;
    private MediaPlayer mediaPlayer;
    private String mTimePlaceholder;
    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private double totalTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mStop.setEnabled(false);
        mPlay.setEnabled(false);
        mTimePlaceholder = "%02d : %02d";

        FileUtil.checkFolders(C.FOLDER_AUDIO);
        outputFile = FileUtil.getFolderPath(C.FOLDER_AUDIO) + "/whistler_" + String.valueOf(System.currentTimeMillis()) + ".m4a";

        durationHandler = new Handler();

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    @OnClick({R.id.record_audio, R.id.play_audio, R.id.stop_audio, R.id.evidence})
    public void manageClick(View view) {
        switch (view.getId()) {
            case R.id.record_audio:
                handleRecord();
                break;
            case R.id.play_audio:
                handlePlay();
                break;
            case R.id.stop_audio:
                handleStop();
                break;
            case R.id.evidence:
                returnData();
                break;

        }
    }

    private void handleStop() {

        try {
            myAudioRecorder.stop();
            myAudioRecorder.release();
            myAudioRecorder = null;
        } catch (RuntimeException e) {
            cleanUp();
            return;
        }

        mInfo.setText(getString(R.string.recoding_play_instruction));

        mStop.setEnabled(false);
        mPlay.setEnabled(true);
        mRecord.setEnabled(true);
        mEvidence.setVisibility(View.VISIBLE);

        durationHandler.removeCallbacks(updateProgressBarTime);
        mProgressBar.setVisibility(View.GONE);
        Snackbar.make(mRecord, R.string.recorded_successfully, Snackbar.LENGTH_SHORT).show();
    }

    private void handlePlay() {

        mProgressBar.setVisibility(View.GONE);
        mSeekBar.setVisibility(View.VISIBLE);
        mRecord.setEnabled(false);
        mInfo.setText(getString(R.string.recording_play));
        mEvidence.setEnabled(false);

        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mSeekBar.setMax(mediaPlayer.getDuration());
        mSeekBar.setClickable(false);

        totalTime = mediaPlayer.getDuration();
        durationHandler.postDelayed(updateSeekBarTime, 0);

        mediaPlayer.start();
        Snackbar.make(mRecord, R.string.playing_audio, Snackbar.LENGTH_SHORT).show();

    }

    private void handleRecord() {

        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        myAudioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        myAudioRecorder.setOutputFile(outputFile);
        mInfo.setText(getString(R.string.recording));

        mProgressBar.setVisibility(View.VISIBLE);
        mSeekBar.setVisibility(View.GONE);
        mEvidence.setVisibility(View.GONE);

        try {
            myAudioRecorder.prepare();
            myAudioRecorder.start();
            mRecord.setEnabled(false);
            mStop.setEnabled(true);
            mPlay.setEnabled(false);
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError error) {
            DialogsUtil.showInfoDialog(context, getString(R.string.error), getString(R.string.insufficient_space));
            mPlay.setEnabled(true);
            mRecord.setEnabled(false);
        }

        startTime = SystemClock.uptimeMillis();
        durationHandler.postDelayed(updateProgressBarTime, 0);

        Snackbar.make(mRecord, R.string.recording_started, Snackbar.LENGTH_SHORT).show();
    }

    private Runnable updateSeekBarTime = new Runnable() {
        public void run() {
            timeElapsed = mediaPlayer.getCurrentPosition();
            mSeekBar.setProgress((int) timeElapsed);
            double timeRemaining = totalTime - timeElapsed;
            mTimer.setText(String.format(mTimePlaceholder, TimeUnit.MILLISECONDS.toMinutes((long) timeRemaining),
                    TimeUnit.MILLISECONDS.toSeconds((long) timeRemaining) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) timeRemaining))));
            if (timeRemaining < 100) {
                durationHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRecord.setEnabled(true);
                        mEvidence.setEnabled(true);
                        mSeekBar.setProgress((int) totalTime);
                        mInfo.setText(getString(R.string.recoding_play_instruction));
                    }
                });
                durationHandler.removeCallbacks(this);
            } else {
                durationHandler.postDelayed(this, 0);
            }
        }
    };

    private Runnable updateProgressBarTime = new Runnable() {
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            mTimer.setText(String.format(mTimePlaceholder, TimeUnit.MILLISECONDS.toMinutes(timeInMilliseconds),
                    TimeUnit.MILLISECONDS.toSeconds(timeInMilliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeInMilliseconds))));
            durationHandler.postDelayed(this, 0);
        }
    };

    private void cleanUp() {
        File file = new File(outputFile);
        if (file.exists()) {
            file.delete();
        }
        mStop.setEnabled(false);
        mPlay.setEnabled(false);
        mRecord.setEnabled(true);
        durationHandler.removeCallbacks(updateProgressBarTime);
        mProgressBar.setVisibility(View.GONE);
        mTimer.setText(getString(R.string.start_time));
        Snackbar.make(mRecord, R.string.recorded_unsuccessfully, Snackbar.LENGTH_SHORT).show();
    }

    private void returnData() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("uri", outputFile);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, returnIntent);
        finish();
    }
}
