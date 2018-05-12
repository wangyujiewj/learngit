package com.example.acer.gesture;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import android.bluetooth.*;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.content.Intent;
import android.view.MotionEvent;
import android.widget.AdapterView;
import android.widget.TextView;

import java.io.InputStreamReader;
import java.io.OutputStream;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Poi;
import com.amap.api.navi.AmapNaviPage;
import com.amap.api.navi.AmapNaviParams;
import com.amap.api.navi.AmapNaviType;
import com.amap.api.navi.INaviInfoCallback;
import com.amap.api.navi.model.AMapNaviLocation;
import android.widget.ArrayAdapter;
import com.google.gson.Gson;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.CacheRequest;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import android.bluetooth.*;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUtility;
import java.util.List;
import android.widget.ListView;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import java.util.Timer;
import java.util.UUID;
import android.widget.Toast;
import java.util.ArrayList;
import android.os.Handler;
import android.os.Message;
import java.io.InputStream;
import java.util.UUID;
//主界面
public class MainActivity extends AppCompatActivity implements INaviInfoCallback {

    private GestureDetector mGestureDetector;
    private InitApplication mAppInstance;
    private String s1="";//将传来的字节转化成字符串
    //static final String host = "192.168.43.56";//我自己的ip 改成你自己的
    private final String text = "请注意，前方有障碍物";//超声波检测到障碍的语音提示
     KqwSpeechCompound mKqwSpeechCompound;
    static final int port = 9995;//python的port要和这个一致
    //Socket socket = null;
    //BufferedReader in = null;
    //BufferedWriter out = null;
    private Date now;
    private long between ;

    private String ReceiveData = "";

    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//蓝牙通用串口不用改

    private static String address = "80:DA:B2:72:D5:D6"; // <==要连接的目标蓝牙设备MAC地址


    private ReceiveThread rThread = null;  //数据接收线程
    private BluetoothSocket btSocket = null;

    private OutputStream outStream = null;

    private InputStream inStream = null;
      BluetoothAdapter mBluetoothAdapter = null;//蓝牙适配器



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAppInstance = (InitApplication)getApplication();
        mKqwSpeechCompound = new KqwSpeechCompound(this);//语音合成对象
        new Thread(networkTask).start();//开启socket通信（pc端）线程

        InitBluetooth();
        //flag =  mBluetoothAdapter.getState();
        //handler = new MyHandler();

        //判断蓝牙是否打开
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        mBluetoothAdapter.startDiscovery();


        //创建连接
        new ConnectTask().execute(address);



        // 将“12345678”替换成您申请的 APPID,104到130手势识别
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=5a9f54c8");
        //1 初始化  手势识别器
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                   float velocityY) {// e1: 第一次按下的位置   e2   当手离开屏幕 时的位置  velocityX  沿x 轴的速度  velocityY： 沿Y轴方向的速度
                if ((e2.getRawY() - e1.getRawY()) > 200) {// 表示向下滑动调用navigation

                    Speech();
                    return true;
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });

    }
    public void InitBluetooth()
    {
        //得到一个蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if(mBluetoothAdapter == null) {
            mAppInstance.flag=0;
            finish();
            return;
        }
    }
    //连接蓝牙设备的异步任务
    class ConnectTask extends AsyncTask<String,String,String>
    {


        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(params[0]);

            try {

                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);


                btSocket.connect();

                Log.e("error", "ON RESUME: BT connection established, data transfer link open.");

            } catch (IOException e) {

                try {
                    btSocket.close();
                    return "Socket 创建失败";

                } catch (IOException e2) {

                    Log .e("error","ON RESUME: Unable to close socket during connection failure", e2);
                    return "Socket 关闭失败";
                }

            }
            //取消搜索
            mBluetoothAdapter.cancelDiscovery();

            try {
                outStream = btSocket.getOutputStream();

                // inStream = btSocket.getInputStream();

            } catch (IOException e) {
                Log.e("error", "ON RESUME: Output stream creation failed.", e);
                return "Socket 流创建失败";
            }


            return "蓝牙连接正常,Socket 创建成功";
        }

        @Override    //这个方法是在主线程中运行的，所以可以更新界面
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub

            //连接成功则启动监听
            rThread=new ReceiveThread();

            rThread.start();



            super.onPostExecute(result);
        }



    }
    //发送数据到蓝牙设备的异步任务
    class SendInfoTask extends AsyncTask<String,String,String>
    {

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);



        }

        @Override
        protected String doInBackground(String... arg0) {
            // TODO Auto-generated method stub

            if(btSocket==null)
            {
                return "还没有创建连接";
            }

            if(arg0[0].length()>0)//不是空白串
            {
                //String target=arg0[0];

                byte[] msgBuffer = arg0[0].getBytes();

                try {
                    //  将msgBuffer中的数据写到outStream对象中
                    outStream.write(msgBuffer);

                } catch (IOException e) {
                    Log.e("error", "ON RESUME: Exception during write.", e);
                    return "发送失败";
                }

            }

            return "发送成功";
        }

    }
    //从蓝牙接收信息的线程
    class ReceiveThread extends Thread
    {

        @Override
        public void run() {

            while (btSocket != null)//一个接受信息的循环
            {

                //定义一个存储空间buff
                byte[] buff = new byte[1024];
                try {
                    inStream = btSocket.getInputStream();

                    System.out.println("waitting for instream");

                    inStream.read(buff); //读取数据存储在buff数组中
//
                    try {
                        processBuffer(buff, 1024);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //System.out.println("receive content:"+ReceiveData);
                } catch (IOException e) {
                    try{
                        mKqwSpeechCompound.speaking("超声波已断开");
                        Thread.sleep(3500);
                    }catch (Exception ae){
                        ae.printStackTrace();
                    }
                    System.out.println("123456789123456789");
                    e.printStackTrace();//当try中语句出现异常时，会执行catch语句，在命令行打印异常信息在程序中出错的位置及原因
                    break;
                }

            }
        }

        private void processBuffer(byte[] buff,int size) throws InterruptedException {
            int length=0;
            for(int i=0;i<size;i++)
            {
//				if(buff[i]>'\0')
                if(buff[i]>'\0')
                {
                    length++;
                }
                else
                {
                    break;
                }
            }

//			System.out.println("receive fragment size:"+length);

            byte[] newbuff=new byte[length];  //newbuff字节数组，用于存放真正接收到的数据

            for(int j=0;j<length;j++)
            {
                newbuff[j]=buff[j];
            }
//            ReceiveData = ReceiveData + new String(newbuff);
//            if (ReceiveData == "")
//                mKqwSpeechCompound.speaking("超声波传感器断开连接".trim());
//            for(int i = 0; i<ReceiveData.length(); i++)
//            {
//                if (newbuff[i] >= 49)
//                    mKqwSpeechCompound.speaking(text.trim());
//
//            }
            s1=s1+new String(newbuff);
            if(s1.length()>0)
            {
                mKqwSpeechCompound.speaking(text.trim());
                Thread.sleep(3500);

            }
//			ReceiveData=new String(newbuff);

//			System.out.println("result :"+ReceiveData);

//			Message msg=new Message();  //by ywq
            Message msg=Message.obtain();
            msg.what=1;
            handler.sendMessage(msg);  //发送消息:系统会自动调用handleMessage( )方法来处理消息
        }
    }
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();


        try {
            if(rThread!=null)
            {

                btSocket.close();
                btSocket=null;

                rThread.join();
            }

            this.finish();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
/*
页面更新用的Handle 不需要更新主界面的UI就不需要
 */
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String val = data.getString("value");

        }
    };

    Runnable networkTask = new Runnable() {
        @Override
        public void run() {

                try {
                    Socket socket = new Socket("10.20.89.177", 9002);
                    System.out.println("客户端启动成功");
                    //BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    PrintStream write = new PrintStream((socket.getOutputStream()));
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String info = "hello";
//                    readline = br.readLine(); // 从系统标准输入读入一字符串
                    //write.write(info);
                    String inmsg = null;
                    while (!info.equals("end")) {
                        write.println(info);
                        inmsg=in.readLine();
                        write.flush();
                        //Log.i("message",inmsg);

                        if(inmsg!=null)
                        {

                            mKqwSpeechCompound.speaking("障碍物在前方".trim());

                       }
                       // else if(inmsg=="2")
                       // {
                       //     mKqwSpeechCompound.speaking("小心，前方有障碍".trim());
                     //   }

//
                       // inmsg = in.readLine(); // 从系统标准输入读入一字符串
                    } // 继续循环

                    write.close(); // 关闭Socket输出流
                    in.close(); // 关闭Socket输入流
                    socket.close(); // 关闭Socket
                } catch (Exception e) {
                    try{
                        mKqwSpeechCompound.speaking("摄像头已断开".trim());
                        Thread.sleep(3500);
                    }catch(Exception ae)
                    {
                        ae.printStackTrace();
                    }
                    System.out.println("can not listen to:" + e);// 出错，打印出错信息


                }
                   /* socket = new Socket(host, port);//注意host改成你服务器的hostname或IP地址
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    //send output msg
                    ((InitApplication) getApplication()).setSocket(socket);//首次初始化后赋值给全局
                    String outMsg = "TCP connecting to " + port + System.getProperty("line.separator");
                    out.write(outMsg);//发送数据
                    out.flush();
                    Log.i("TcpClient", "sent: " + outMsg);
                    //accept server response
                    String inMsg = in.readLine() + System.getProperty("line.separator");//得到服务器返回的数据


                    if (inMsg.length() > 0) {
                        mKqwSpeechCompound.speaking("您已偏离盲道".trim());//目前只是为了检测到是不是能接受到数据
                    }
                    Log.i("TcpClient", "received: " + inMsg);
                    //close connection
                    // socket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
            }

    };

    private void Speech() {
        Intent intent = new Intent(this,NavigationActivity.class); //构建intent，intent指向要跳转的activity
        startActivity(intent);

    }
    public boolean onTouchEvent(MotionEvent event) {
        //2.让手势识别器生效
        mGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    public void onInitNaviFailure() {

    }

    @Override
    public void onGetNavigationText(String s) {

    }

    @Override
    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {

    }

    @Override
    public void onArriveDestination(boolean b) {

    }

    @Override
    public void onStartNavi(int i) {

    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {

    }

    @Override
    public void onCalculateRouteFailure(int i) {

    }

    @Override
    public void onStopSpeaking() {

    }

    @Override
    public void onReCalculateRoute(int i) {

    }

    @Override
    public void onExitPage(int i) {

    }

}

