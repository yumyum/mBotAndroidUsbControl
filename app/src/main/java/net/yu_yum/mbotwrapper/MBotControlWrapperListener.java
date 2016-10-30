package net.yu_yum.mbotwrapper;

/**
 * mBotとの接続状態変更を通知するリスナー
 * Created by yu on 2016/10/30.
 */

public interface MBotControlWrapperListener {
    void onConnectionStateChanged(boolean connected);
}
