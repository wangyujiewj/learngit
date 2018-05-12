package com.example.acer.gesture;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.LocationSource;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.AMapNaviView;
import com.amap.api.navi.AMapNaviViewListener;
import com.amap.api.navi.enums.NaviType;
import com.amap.api.navi.model.AMapLaneInfo;
import android.view.KeyEvent;
import com.amap.api.navi.model.AMapModelCross;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviInfo;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.services.core.LatLonPoint;
import com.autonavi.tbt.TrafficFacilityInfo;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static com.example.acer.gesture.MainActivity.port;



public class WalikingNavi extends AppCompatActivity implements AMapNaviViewListener, AMapNaviListener,AMapLocationListener {
    private static final String TAG = "NavigationActivity";
    private LocationSource.OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private AMapNaviView mAMapNaviView;
    private AMapNavi mAMapNavi;
    private TTSController mTtsManager;
    private InitApplication mAppInstance;
    private final List<NaviLatLng> sList = new ArrayList<NaviLatLng>();
    private final List<NaviLatLng> eList = new ArrayList<NaviLatLng>();
    private List<NaviLatLng> mWayPointList = new ArrayList<>();
    KqwSpeechCompound mKqwSpeechCompound;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mAppInstance = (InitApplication)getApplication();
        mKqwSpeechCompound = new KqwSpeechCompound(this);//语音合成对象
        //new Thread(networkTask).start();//创建一个新的socket线程
        ActionBar actionbar = getSupportActionBar();
        if(actionbar!=null){
            actionbar.hide();
        }
        setContentView(R.layout.activity_waliking_navi);
        mAMapNaviView = (AMapNaviView) findViewById(R.id.navi_view);
        mAMapNaviView.onCreate(savedInstanceState);
        mAMapNaviView.setAMapNaviViewListener(this);
        mAMapNavi = AMapNavi.getInstance(this);//创建一个AMapNavi的导航对象
        mAMapNavi.addAMapNaviListener(this);
        mTtsManager = TTSController.getInstance(getApplicationContext());
        mTtsManager.init();
        mAMapNavi.addAMapNaviListener(mTtsManager);
        //Log.i("")
        mAMapNavi.startNavi(NaviType.GPS);//导航位置变化驱动类型  GPS导航数值1，模拟导航数值2，巡航模式数值3



        //LatLonPoint point1=mAMapNavi.
    }

    //socket通信
    Runnable networkTask = new Runnable() {
        @Override
        public void run() {
            try {
                Socket socket = ((InitApplication)getApplication()).getSocket();
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //send output msg
                String outMsg = "TCP connecting to " + port + System.getProperty("line.separator");
                out.write(outMsg);//发送数据
                out.flush();
                Log.i("TcpClient", "sent: " + outMsg);
                //accept server response
                String inMsg = in.readLine() + System.getProperty("line.separator");//得到服务器返回的数据

                if(inMsg.length()>0)
                {
                    mKqwSpeechCompound.speaking("您已偏离盲道".trim());
                }
                Log.i("TcpClient", "received: " + inMsg);
                //close connection
                //socket.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public Resources getResources(){
        return getBaseContext().getResources();
    }
    protected void onResume() {
        super.onResume();
        mAMapNaviView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAMapNaviView.onPause();

//        仅仅是停止你当前在说的这句话，一会到新的路口还是会再说的
        mTtsManager.stopSpeaking();
//
//        停止导航之后，会触及底层stop，然后就不会再有回调了，但是讯飞当前还是没有说完的半句话还是会说完

    }

    @Override
    protected void onDestroy() {
       // Intent gpsintent = new Intent(this,MainActivity.class);//直接退到主界面
        //startActivity(gpsintent);
        super.onDestroy();
//        mAMapNaviView.onDestroy();
        //since 1.6.0 不再在naviview destroy的时候自动执行AMapNavi.stopNavi();请自行执行
        mAMapNavi.stopNavi();
//        mAMapNavi.destroy();
        mTtsManager.destroy();

    }
    /*
    //回调的匿名内部类
    private BluetoothGattCallback mGattCallback= new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState== BluetoothProfile.STATE_DISCONNECTED)//如果新的状态为断开连接
            {
                  flag
            }
        }
    };*/
    @Override
    public void onInitNaviFailure() {
    }
    //@Override

    public void onInitNaviSuccess() {


    }

    @Override
    public void onStartNavi(int i) {

    }

    @Override
    public void onTrafficStatusUpdate() {

    }

    @Override
    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {

    }

    @Override
    public void onGetNavigationText(int i, String s) {

    }

    @Override
    public void onGetNavigationText(String s) {

    }

    @Override
    public void onEndEmulatorNavi() {

    }

    @Override
    public void onArriveDestination() {

    }

    @Override
    public void onCalculateRouteFailure(int i) {

    }

    @Override
    public void onReCalculateRouteForYaw() {

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {

    }

    @Override
    public void onArrivedWayPoint(int i) {

    }

    @Override
    public void onGpsOpenStatus(boolean b) {

    }

    @Override
    public void onNaviInfoUpdate(NaviInfo naviInfo) {

    }

    @Override
    public void onNaviInfoUpdated(AMapNaviInfo aMapNaviInfo) {

    }

    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapNaviCameraInfos) {

    }

    @Override
    public void updateIntervalCameraInfo(AMapNaviCameraInfo aMapNaviCameraInfo, AMapNaviCameraInfo aMapNaviCameraInfo1, int i) {

    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] aMapServiceAreaInfos) {

    }

    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {

    }

    @Override
    public void hideCross() {

    }

    @Override
    public void showModeCross(AMapModelCross aMapModelCross) {

    }

    @Override
    public void hideModeCross() {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] aMapLaneInfos, byte[] bytes, byte[] bytes1) {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo aMapLaneInfo) {

    }

    @Override
    public void hideLaneInfo() {

    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {
        mAMapNavi.startNavi(NaviType.GPS);

    }

    @Override
    public void notifyParallelRoad(int i) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] aMapNaviTrafficFacilityInfos) {

    }

    @Override
    public void OnUpdateTrafficFacility(TrafficFacilityInfo trafficFacilityInfo) {

    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat aimLessModeStat) {

    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo aimLessModeCongestionInfo) {

    }

    @Override
    public void onPlayRing(int i) {

    }

    @Override
    public void onNaviSetting() {

    }

    @Override
    public void onNaviCancel() {
        //     NavigationActivity.start(this);
        finish();
    }

    @Override
    public boolean onNaviBackClick() {
        return false;
    }

    @Override
    public void onNaviMapMode(int i) {

    }

    @Override
    public void onNaviTurnClick() {

    }

    @Override
    public void onNextRoadClick() {

    }

    @Override
    public void onScanViewButtonClick() {

    }

    @Override
    public void onLockMap(boolean b) {

    }

    @Override
    public void onNaviViewLoaded() {

    }
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {//按返回键时就退出
            finish();
            System.exit(0);
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {

    }
/*
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        startList.add(new NaviLatLng(amapLocation.getLatitude(),amapLocation.getLongitude()));
    }*/
}



