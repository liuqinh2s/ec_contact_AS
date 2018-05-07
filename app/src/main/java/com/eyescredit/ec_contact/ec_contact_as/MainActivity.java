package com.eyescredit.ec_contact.ec_contact_as;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    protected MyDeleteAsyncTask myDeleteAsyncTask;

    protected ProgressDialog progressDialog;

    protected Button importContactsButton;
    protected Button deleteContactsButton;

    public void importContacts() {
        Intent intent = new Intent(MainActivity.this, ImportContactActivity.class);
        startActivity(intent);
    }

    public void exitContacts() {
        System.exit(0);
    }

    public void clearContacts() {
        Log.d(TAG, "clearContacts() in");
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage("这是一个漫长的过程，请停止其他操作，耐心等待");

        new AlertDialog.Builder(this).setTitle("提示")
                .setMessage("整个过程大概需要2分钟左右，是否继续？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, ""+which);
                        progressDialog.show();

                        myDeleteAsyncTask = new MyDeleteAsyncTask();
                        myDeleteAsyncTask.execute("");
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, ""+which);
                    }
                }).show();
    }

    // 在log里面打印通讯录的内容（与源数据进行比对，看看是否有误）
    public void testReadAll() {
        // 访问raw_contacts表
        Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
        ContentResolver resolver = getContentResolver();
        // 获得_ID属性
        Cursor cursor = resolver.query(uri, new String[]{ContactsContract.Data._ID}, null, null, null);
        while(cursor.moveToNext()){
            StringBuilder buf = new StringBuilder();
            // 获得id，并在data中寻找数据
            int id = cursor.getInt(0);
            buf.append("id="+id);
            uri = Uri.parse("content://com.android.contacts/raw_contacts/"+id+"/data");
            // data1存储各个记录的总数据，mimetype存放记录的类型，如电话、email等
            Cursor cursor1 = resolver.query(uri,
                    new String[]{ContactsContract.Data.DATA1, ContactsContract.Data.MIMETYPE},
                    null, null, null);
            while(cursor1.moveToNext()){
                String data = cursor1.getString(cursor1.getColumnIndex("data1"));
                if(cursor1.getString(cursor1.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/name")){
                    buf.append(",name="+data);
                }else if(cursor1.getString(cursor1.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/phone_v2")){
                    buf.append(",phone="+data);
                }else if(cursor1.getString(cursor1.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/email_v2")){
                    buf.append(",email="+data);
                }else if(cursor1.getString(cursor1.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/postal-address_v2")){
                    buf.append(",address="+data);
                }else if(cursor1.getString(cursor1.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/organization")){
                    buf.append(",organization="+data);
                }
            }
            String str = buf.toString();
            Log.d(TAG, str);
        }
    }

    public void deleteAll() {
        Log.d(TAG, "deleteAll() in");
        Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
        int rows = getContentResolver().delete(uri, "_id!=-1", null);
        Log.d(TAG, "delete rows: " + rows);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        importContactsButton = findViewById(R.id.importContactsButton);
        deleteContactsButton = findViewById(R.id.deleteContactsButton);

        importContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importContacts();
            }
        });

        deleteContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearContacts();
            }
        });
    }

    class MyDeleteAsyncTask extends AsyncTask<String, Integer, List<String>> {
        private MyDeleteAsyncTask() {

        }

        @Override
        protected List<String> doInBackground(String... strings) {
            Log.d(TAG, "doInBackground() in");
            deleteAll();
            return null;
        }

        @Override
        protected void onPostExecute(List<String> strings) {
            super.onPostExecute(strings);
            if(progressDialog!=null){
                progressDialog.cancel();
            }
        }
    }
}
