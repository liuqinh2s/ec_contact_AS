package com.eyescredit.ec_contact.ec_contact_as;

import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImportContactActivity extends AppCompatActivity {

    private static final String TAG = "ImportContactActivity";

    protected MyImportAsyncTask task;

    protected RequestQueue requestQueue;

    protected EditText mUrlTextView;
    protected TextView mFilePathTextView;
    // 根据数据录入条数来分析进度
    protected TextView mProgressTextView;
    // 显示消耗的时间
    protected TextView mProgress1TextView;

    protected Button startImportButton;

    private long startTime = 0l;
    private long endTime = 0l;

    private long totalCount = 0l;
    private long finishCount=0l;

    private static Context context;

    private int updateDial = 100;

    private boolean stopFlag = false;

    ProgressDialog dialog;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if(msg.what == updateDial){
                int progress = (Integer)msg.obj;
                dialog.incrementProgressBy(progress);
                Log.d(TAG,"progress:"+progress);
            }
        }
    };

    public void importContact() {
        startTime = new Date().getTime();

        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMessage("正在获取联系人，请稍等。。。");
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                // 是否中断正在执行的 doInBackground 方法
                task.cancel(false);
                dialog.cancel();
                Log.e(TAG, "Progress Cancel");
                stopFlag = true;
                return false;
            }
        });
        dialog.show();

        String url = mUrlTextView.getText().toString();
        try{
            MyStringRequest loginRequest = new MyStringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // 得到数据条数
                    String[] temp = response.split("\n");
                    if (0 == temp.length) {
                        temp = response.split("\r\n");
                    }
                    totalCount = temp.length;

                    // UI
                    dialog.setMessage("正在导入联系人。。。");
                    dialog.setProgress(0);
                    dialog.setMax((int) totalCount);

                    // 后台任务
                    task = new MyImportAsyncTask(dialog);
                    task.execute(response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dialog.dismiss();
                    Toast.makeText(context, "请稍后重试"+error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
//            JsonObjectRequest loginRequest = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
//                @Override
//                public void onResponse(JSONObject response) {
//                    try{
//                        JSONObject jsonObject = new JSONObject(response.toString());
//                        JSONArray jsonArray = jsonObject.getJSONArray("cpname");
//                    }catch (JSONException e){
//                        e.printStackTrace();
//                    }
//                }
//            }, new Response.ErrorListener() {
//                @Override
//                public void onErrorResponse(VolleyError error) {
//
//                }
//            });
            loginRequest.setRetryPolicy(new DefaultRetryPolicy(30*1000, 1, 1.0f));
            requestQueue.add(loginRequest);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    // 单步添加
    public void addContact(String name, String phoneNumber) {
        // 创建一个空的ContentValues
        ContentValues contentValues = new ContentValues();

        // 向RawContacts.CONTENT_URI空值插入
        // 先获取Android系统返回的rawContactId，后面要基于此id插入值
        Uri rawContactUri = getContentResolver().insert(RawContacts.CONTENT_URI, contentValues);
        long rawContactId = ContentUris.parseId(rawContactUri);
        contentValues.clear();

        contentValues.put(Data.RAW_CONTACT_ID, rawContactId);
        // 内容类型
        contentValues.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        // 联系人名字
        contentValues.put(StructuredName.GIVEN_NAME, name);
        // 向联系人Uri添加联系人名字
        getContentResolver().insert(Data.CONTENT_URI, contentValues);
        contentValues.clear();

        contentValues.put(Data.RAW_CONTACT_ID, rawContactId);
        // 内容类型
        contentValues.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        // 联系人电话
        contentValues.put(Phone.NUMBER, phoneNumber);
        // 向联系人Uri添加联系人电话
        getContentResolver().insert(Data.CONTENT_URI, contentValues);
        contentValues.clear();

        // UI相关（展示进度）：
        finishCount++;
        long nowTime = new Date().getTime();
        double costTime = (nowTime-startTime)/1000;
        String str1 = "总数："+totalCount+", 已完成数："+finishCount;
        Log.d(TAG, str1);
        mProgressTextView.setText(str1);

        DecimalFormat df = new DecimalFormat("0.00");
        double finishRate = (finishCount/totalCount)*100.0;
        double rate = finishCount/costTime;
        double leftTime = (totalCount-finishCount)/rate;

        String str2 = "完成率："+df.format(finishRate)+"% , 速率："+df.format(rate)+"个/s , 预估剩下时间："+df.format(leftTime)+"s";
        Log.d(TAG, str2);
        mProgress1TextView.setText(str2);
    }

    // 批量添加
    public static void batchInsertPhone(List<Contact> contactList, Context ctx) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        int rawContentInsertIndex;
        int contactListLength = contactList.size();
        for (int i = 0; i < contactListLength; i++) {
            for(int j=0;j<contactList.get(i).getMdata().size();j++){

                String number = contactList.get(i).getMdata().get(j);
                Log.d(TAG, "batchInsertPhone() number:"+number);

                // 这句话很重要，有了它才可以真正地实现批量添加（疑问：是否要每个循环都更新一下）
                rawContentInsertIndex = ops.size();

                ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                        .withValue(RawContacts.ACCOUNT_TYPE, null)
                        .withValue(RawContacts.ACCOUNT_NAME, null)
                        .withYieldAllowed(true).build());

                ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(Data.RAW_CONTACT_ID, rawContentInsertIndex)
                        .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(StructuredName.DISPLAY_NAME, number)
                        .withYieldAllowed(true).build());

                ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(Data.RAW_CONTACT_ID, rawContentInsertIndex)
                        .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                        .withValue(Phone.NUMBER, number)
                        .withYieldAllowed(true).build());
            }

//            for(int j=0;j<contactList.get(i).getMdata().size();j++){
//                // 解析名字和职位
//                String togetherInfo=contactList.get(i).getMdata().get(j);
//                int togetherInfoSize = contactList.get(i).getMdata().get(j).length();
//                String name;
//                String relationship;
//                int index = togetherInfo.indexOf("#");
//                name = togetherInfo.substring(0, index);
//                relationship = togetherInfo.substring(index, togetherInfoSize);
//            }
        }

        try {
            // 这里才调用的批量添加，前面都是配置过程
            ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_contact);

        mUrlTextView = findViewById(R.id.urlTextView);
        mFilePathTextView = findViewById(R.id.filePathTextView);
        mProgress1TextView = findViewById(R.id.progressTextView);
        mProgress1TextView = findViewById(R.id.progress1TextView);
        context = this;
        requestQueue = Volley.newRequestQueue(this);

        startImportButton = findViewById(R.id.startImportButton);
        startImportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importContact();
            }
        });
    }

    class MyImportAsyncTask extends AsyncTask<String, Integer, List<String>> {

        private MyImportAsyncTask(ProgressDialog dial){

        }

        @Override
        protected List<String> doInBackground(String... strings) {

            // 数据格式：由多条json对象组成，每个json占一行
            // 所以先以换行符分割出每个json
            // 然后使用alibaba的fastjson开源工具进行解析
            String[] tem = strings[0].split("\n");
            if(0 == tem.length){
                tem = strings[0].split("\r\n");
            }
            Log.d(TAG, "Total Count = "+totalCount);

            List<Contact> contactList = new ArrayList<>();

            int pageCount=100;
            // 向上取整
            long page = (totalCount+pageCount-1)/pageCount;
            for(int i=0;i<page;i++){
                if(!stopFlag){
                    for(int j=0;j<pageCount;j++){
                        Contact contact = new Contact();
                        Log.d(TAG, "str="+tem[j+i*pageCount]);
                        contact = JSON.parseObject(tem[j+i*pageCount], Contact.class);
                        String str = JSON.toJSONString(contact);
                        Log.d(TAG, str);
                        contactList.add(contact);
                    }
                    batchInsertPhone(contactList, context);
                    contactList.clear();

                    Message msg = mHandler.obtainMessage();
                    msg.what = updateDial;
                    int progress = pageCount;
                    mHandler.sendMessage(msg);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<String> strings) {
            super.onPostExecute(strings);
            if(dialog!=null){
                dialog.cancel();
            }

            endTime = new Date().getTime();
            double costTime = (endTime-startTime)/1000;
            DecimalFormat df = new DecimalFormat("0.00");
            mProgress1TextView.setText("已完成，总耗时："+df.format(costTime)+"s");

            Toast.makeText(context, "结束导入联系人", Toast.LENGTH_SHORT).show();
        }
    }

    class MyStringRequest extends StringRequest {

        public MyStringRequest(int method, String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
            super(method, url, listener, errorListener);
        }

        @Override
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            String parsed;
            try {
                parsed = new String(response.data, "utf-8");
            } catch (UnsupportedEncodingException var4) {
                parsed = new String(response.data);
            }

            return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
        }
    }
}
