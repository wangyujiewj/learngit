package com.example.acer.gesture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Poi;
import com.amap.api.navi.view.PoiInputItemWidget;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.google.gson.Gson;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
//import com.amap.navi.demo.activity.RestRouteShowActivity;

import java.util.ArrayList;
import java.util.List;

public class SearchPoiActivity extends Activity implements AdapterView.OnItemClickListener, View.OnClickListener, SearchView.OnQueryTextListener{
    private SearchView mSearchView;// 输入搜索关键字
    private ImageView mBack;
    private ListView mInputListView;
    private List<Tip> mCurrentTipList;//List中存的是Tip数据

    private String result;
    private SoundPool soundPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi_key_search);
        soundPool= new SoundPool(5, AudioManager.STREAM_ALARM,5);
        final int soundID=soundPool.load(this,R.raw.collide,1);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                soundPool.play(soundID, 0.6f, 0.6f, 1, 0, 1);
            }
        });

        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=5a9f54c8");
        initSearchView();
        mInputListView = (ListView) findViewById(R.id.inputtip_list);
        mInputListView.setOnItemClickListener(this);
        mBack = (ImageView) findViewById(R.id.back);
        mBack.setOnClickListener(this);
        initSpeech(this);



    }


    public void initSpeech(final Context context) {
        //1.创建RecognizerDialog对象
        RecognizerDialog mDialog = new RecognizerDialog(context, null);
        //2.设置accent、language等参数
        mDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        mDialog.setParameter(SpeechConstant.ACCENT, "mandarin");
        //3.设置回调接口
        mDialog.setListener(new RecognizerDialogListener() {
            @Override
            public void onResult(RecognizerResult recognizerResult, boolean isLast) {
                if (!isLast) {
                    //解析语音
                     result = parseVoice(recognizerResult.getResultString());
                    Intent intent = new Intent();
                    intent.putExtra(Constants.KEY_WORDS_NAME, result);//key-value对
                    setResult(NavigationActivity.RESULT_CODE_KEYWORDS, intent);//result_code_keywords同样是用来标识的  回复码
                    finish();
                }


            }

            @Override
            public void onError(SpeechError speechError) {

            }
        });
        mDialog.show();
    }
    /**
     * 解析语音json
     */
    public String parseVoice(String resultString) {
        Gson gson = new Gson();
        SpeechRecognition.Voice voiceBean = gson.fromJson(resultString, SpeechRecognition.Voice.class);

        StringBuffer sb = new StringBuffer();
        ArrayList<SpeechRecognition.Voice.WSBean> ws = voiceBean.ws;
        for (SpeechRecognition.Voice.WSBean wsBean : ws) {
            String word = wsBean.cw.get(0).w;
            sb.append(word);
        }
        return sb.toString();
    }

    /**
     * 语音对象封装
     */
    public class Voice {

        public ArrayList<SpeechRecognition.Voice.WSBean> ws;

        public class WSBean {
            public ArrayList<SpeechRecognition.Voice.CWBean> cw;
        }

        public class CWBean {
            public String w;
        }
    }

    private void initSearchView() {
        mSearchView = (SearchView) findViewById(R.id.keyWord);
        mSearchView.setOnQueryTextListener(this);//表示输入框文字
        //设置SearchView默认为展开显示
        mSearchView.setIconified(false);
        mSearchView.onActionViewExpanded();
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setSubmitButtonEnabled(false);

    }

    /**
     * 输入提示回调
     *
     * @param //tipList
     * @param //rCode
     */
    /*
@Override
    public void onGetInputtips(List<Tip> tipList, int rCode) {
        if (rCode == 1000) {// 正确返回
            mCurrentTipList = tipList;
            List<String> listString = new ArrayList<String>();
            for (int i = 0; i < tipList.size(); i++) {
                listString.add(tipList.get(i).getName());
            }
            mIntipAdapter = new InputTipsAdapter(
                    getApplicationContext(),
                    mCurrentTipList);
            mInputListView.setAdapter(mIntipAdapter);
            mIntipAdapter.notifyDataSetChanged();
        } else {
            ToastUtil.showerror(this, rCode);
        }

    }*/

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (mCurrentTipList != null) {
            Tip tip = (Tip) adapterView.getItemAtPosition(i);
            Intent intent = new Intent();
            intent.putExtra(Constants.EXTRA_TIP,tip);
            setResult(NavigationActivity.RESULT_CODE_INPUTTIPS, intent);//intent携带了数据
            this.finish();
        }

    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.back) {
            this.finish();
        }
    }
//按下确认键触发
    @Override
    public boolean onQueryTextSubmit(String query) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY_WORDS_NAME, query);
        setResult(NavigationActivity.RESULT_CODE_KEYWORDS, intent);
        this.finish();
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }


/*
    //用户输入字符时激发该方法
    @Override
    public boolean onQueryTextChange(String newText) {
        if (!IsEmptyOrNullString(newText)) {
            InputtipsQuery inputquery = new InputtipsQuery(newText, Constants.DEFAULT_CITY);
            Inputtips inputTips = new Inputtips(SearchPoiActivity.this.getApplicationContext(), inputquery);
            inputTips.setInputtipsListener(this);
            inputTips.requestInputtipsAsyn();
        } else {
            if (mIntipAdapter != null && mCurrentTipList != null) {
                mCurrentTipList.clear();
                mIntipAdapter.notifyDataSetChanged();
            }
        }
        return false;
    }*/


    public static boolean IsEmptyOrNullString(String s) {
        return (s == null) || (s.trim().length() == 0);
    }
}