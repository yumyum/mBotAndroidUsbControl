package net.yu_yum.mbotwrapper;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;


/**
 * mBotをコントロールするためのWrapper
 * Created by yu on 2016/10/29.
 */

public class MBotControlWrapper {

    private static final String TAG = "MBotControlWrapper";
    private static final String ACTION_USB_PERMISSION = "net.yu_yum.mbotwrapper.USB_PERMISSION";
    private static final int TIMEOUT = 1000;
    private static final int PID = 29987;
    private static final int VID = 6790;

    public static final int SIDE_RIGHT = 0;
    public static final int SIDE_LEFT = 1;

    private Context mContext;
    private MBotControlWrapperListener mListener;
    private UsbManager mManager;
    private UsbSerialPort mUsb;
    private UsbDeviceConnection mConnection;

    public MBotControlWrapper(Context context, MBotControlWrapperListener listener) {
        mContext = context;
        mListener = listener;
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mContext.registerReceiver(mUsbReceiver, filter);

        mManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : mManager.getDeviceList().values()) {
            if (device.getProductId() == PID && device.getVendorId() == VID) {
                checkPermissionAndInitUSBDevice(device);
            }
        }
    }

    /**
     * 準備できているかどうかを返す
     *
     * @return 準備できていればtrue
     */
    public boolean isReady() {
        return mUsb != null;
    }

    /**
     * LEDを制御する
     *
     * @param r 赤輝度(0～255)
     * @param g 緑輝度(0～255)
     * @param b 青輝度(0～255)
     */
    public void setLed(int r, int g, int b) {
        if (mUsb == null) {
            return;
        }
        byte[] bytes = new byte[12];
        bytes[0] = (byte) 0xff; // RESET
        bytes[1] = 0x55; // START
        bytes[2] = 0x09; // DATA LENGTH
        bytes[3] = 0x00; // ID
        bytes[4] = 0x02; // RUN
        bytes[5] = 0x08; // LED
        bytes[6] = 0x07; // PORT
        bytes[7] = 0x02; // SLOT
        bytes[8] = 0x00; // ID(right:01 left:02)
        bytes[9] = (byte) r;
        bytes[10] = (byte) g;
        bytes[11] = (byte) b;
        try {
            mUsb.write(bytes, TIMEOUT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * モーターを制御する
     *
     * @param side  SIDE_RIGHT/SIDE_LEFT
     * @param speed 速度(-255～255)
     */
    public void controlMotor(int side, int speed) {
        if (mUsb == null) {
            return;
        }
        byte sideB;
        short speedS;
        if (side == SIDE_LEFT) {
            sideB = 0x0a;
            speedS = (short) speed;
        } else {
            sideB = 0x09;
            speedS = (short) -speed;
        }
        byte[] bytes = new byte[9];
        bytes[0] = (byte) 0xff; // RESET
        bytes[1] = 0x55; // START
        bytes[2] = 0x06; // DATA LENGTH
        bytes[3] = 0x00; // ID
        bytes[4] = 0x02; // RUN
        bytes[5] = 0x0a; // SERVO
        bytes[6] = sideB; // PORT
        bytes[7] = (byte) (speedS & 0xff); // Speed high
        bytes[8] = (byte) (speedS >> 8 & 0xff); // Speed low
        try {
            mUsb.write(bytes, TIMEOUT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkPermissionAndInitUSBDevice(UsbDevice device) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        if (mManager.hasPermission(device)) {
            initUSBDevice(device);
        } else {
            mManager.requestPermission(device, permissionIntent);
        }


    }

    private void initUSBDevice(UsbDevice device) {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(mManager);
        if (availableDrivers.isEmpty()) {
            return;
        }
        UsbSerialDriver driver = availableDrivers.get(0);
        mConnection = mManager.openDevice(device);
        mUsb = driver.getPorts().get(0);
        try {
            mUsb.open(mConnection);
            mUsb.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            if (mListener != null) {
                // 接続を通知
                mListener.onConnectionStateChanged(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 直接シリアルポートに書き込む
     *
     * @param bytes 書き込むデータ
     */
    public void rawWrite(byte[] bytes) throws IOException {
        if (mUsb != null) {
            mUsb.write(bytes, TIMEOUT);
        }
    }

    /**
     * 直接シリアルポートから読み込む
     *
     * @param bytes 読み込み先
     */
    public void rawRead(byte[] bytes) throws IOException {
        if (mUsb != null) {
            mUsb.read(bytes, TIMEOUT);
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            switch (action) {
                case ACTION_USB_PERMISSION:
                    synchronized (this) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                initUSBDevice(device);
                            }
                        } else {
                            Log.d(TAG, "permission denied for device " + device);
                        }
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    if (device.getProductId() == PID && device.getVendorId() == VID) {
                        checkPermissionAndInitUSBDevice(device);
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    if (mConnection != null) {
                        mConnection.releaseInterface(device.getInterface(0));
                        mConnection.close();
                        mConnection = null;
                        mUsb = null;
                        if (mListener != null) {
                            // 切断を通知
                            mListener.onConnectionStateChanged(false);
                        }
                    }
                    break;
            }
        }
    };
}
