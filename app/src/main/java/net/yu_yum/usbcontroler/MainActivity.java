package net.yu_yum.usbcontroler;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import net.yu_yum.mbotwrapper.MBotControlWrapper;
import net.yu_yum.mbotwrapper.MBotControlWrapperListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MBotControlWrapperListener {
    private static final String TAG = "MyActivity";

    private MBotControlWrapper mBotControl;
    private TextView mTextState;
    private SeekBar mSeekBarRed;
    private SeekBar mSeekBarGreen;
    private SeekBar mSeekBarBlue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 接続状態表示
        mTextState = (TextView) findViewById(R.id.textState);

        // コントロールオブジェクト
        mBotControl = new MBotControlWrapper(this, this);

        // 移動制御系
        findViewById(R.id.buttonForward).setOnClickListener(this);
        findViewById(R.id.buttonBack).setOnClickListener(this);
        findViewById(R.id.buttonLeft).setOnClickListener(this);
        findViewById(R.id.buttonRight).setOnClickListener(this);
        findViewById(R.id.buttonStop).setOnClickListener(this);

        // LED制御系
        mSeekBarRed = (SeekBar) findViewById(R.id.seekBarRed);
        mSeekBarGreen = (SeekBar) findViewById(R.id.seekBarGreen);
        mSeekBarBlue = (SeekBar) findViewById(R.id.seekBarBlue);
        findViewById(R.id.buttonLedOn).setOnClickListener(this);
        findViewById(R.id.buttonLedOff).setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.buttonForward:
                mBotControl.controlMotor(MBotControlWrapper.SIDE_RIGHT, 100);
                mBotControl.controlMotor(MBotControlWrapper.SIDE_LEFT, 100);
                break;
            case R.id.buttonBack:
                mBotControl.controlMotor(MBotControlWrapper.SIDE_RIGHT, -100);
                mBotControl.controlMotor(MBotControlWrapper.SIDE_LEFT, -100);
                break;
            case R.id.buttonLeft:
                mBotControl.controlMotor(MBotControlWrapper.SIDE_RIGHT, 100);
                mBotControl.controlMotor(MBotControlWrapper.SIDE_LEFT, -100);
                break;
            case R.id.buttonRight:
                mBotControl.controlMotor(MBotControlWrapper.SIDE_RIGHT, -100);
                mBotControl.controlMotor(MBotControlWrapper.SIDE_LEFT, 100);
                break;
            case R.id.buttonStop:
                mBotControl.controlMotor(MBotControlWrapper.SIDE_RIGHT, 0);
                mBotControl.controlMotor(MBotControlWrapper.SIDE_LEFT, 0);
                break;
            case R.id.buttonLedOn:
                mBotControl.setLed(mSeekBarRed.getProgress(), mSeekBarGreen.getProgress(), mSeekBarBlue.getProgress());
                break;
            case R.id.buttonLedOff:
                mBotControl.setLed(0, 0, 0);
                break;
        }
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        final String newState;
        if (connected) {
            newState = "Connected";
        } else {
            newState = "Disconnected";
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextState.setText(newState);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "YUM onNewIntent");
    }
}
