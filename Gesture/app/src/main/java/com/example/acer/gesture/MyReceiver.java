package com.example.acer.gesture;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.acer.gesture.CIsUtils;
//蓝牙广播机制的接受
public class MyReceiver extends BroadcastReceiver {
    String pin = "1234";  //此处为你要连接的蓝牙设备的初始密钥，一般为1234或0000

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        // throw new UnsupportedOperationException("Not yet implemented");
        String action = intent.getAction(); //得到action
        Log.e("action1=", action);
        BluetoothDevice btDevice=null;  //创建一个蓝牙device对象
        // 从Intent中获取设备对象
        btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if(BluetoothDevice.ACTION_FOUND.equals(action)){  //发现设备
            Log.e("发现设备:", "["+btDevice.getName()+"]"+":"+btDevice.getAddress());

            if(btDevice.getName().contains("FBT06"))//HC-05设备如果有多个，第一个搜到的那个会被尝试。
            {
                if (btDevice.getBondState() == BluetoothDevice.BOND_NONE) {

                    Log.e("ywq", "attemp to bond:"+"["+btDevice.getName()+"]");
                    try {
                        //通过工具类ClsUtils,调用createBond方法
                        CIsUtils.createBond(btDevice.getClass(), btDevice);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }else
                Log.e("error", "Is faild");
        }else if(action.equals("android.bluetooth.device.action.PAIRING_REQUEST")) //再次得到的action，会等于PAIRING_REQUEST
        {
            Log.e("action2=", action);
            if(btDevice.getName().contains("FBT06"))
            {
                Log.e("here", "OKOKOK");

                try {

                    //1.确认配对
                    CIsUtils.setPairingConfirmation(btDevice.getClass(), btDevice, true);
                    //2.终止有序广播
                    Log.i("order...", "isOrderedBroadcast:"+isOrderedBroadcast()+",isInitialStickyBroadcast:"+isInitialStickyBroadcast());
                    abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                    //3.调用setPin方法进行配对...
                    boolean ret = CIsUtils.setPin(btDevice.getClass(), btDevice, pin);

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }else
                Log.e("提示信息", "这个设备不是目标蓝牙设备");

        }
    }
}