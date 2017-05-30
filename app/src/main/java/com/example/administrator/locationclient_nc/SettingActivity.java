package com.example.administrator.locationclient_nc;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.DialerKeyListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SettingActivity extends AppCompatActivity {
    private Button button_back;
    private TextView tv_promptInfo;
    private EditText IPServerIP;
    private EditText IPServerPort;
    private EditText LocationServerIP;
    private EditText LocationServerPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        //显示IP服务器返回的提示信息
        tv_promptInfo=(TextView)findViewById(R.id.IP_promptInfo);

        IPServerIP=(EditText)findViewById(R.id.edit_IPServerIP);
        IPServerIP.setKeyListener(DialerKeyListener.getInstance());
        IPServerPort=(EditText)findViewById(R.id.edit_IPServerPort);
        LocationServerIP=(EditText)findViewById(R.id.edit_LocationServerIP);
        LocationServerIP.setKeyListener(DialerKeyListener.getInstance());
        LocationServerPort=(EditText)findViewById(R.id.edit_LocationServerPort);
        //显示上个活动传过来的数据
        showRevData();
        button_back=(Button)findViewById(R.id.button_back);
        button_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String IP1=IPServerIP.getText().toString();
                String port1=IPServerPort.getText().toString();
                String IP2=LocationServerIP.getText().toString();
                String port2=LocationServerPort.getText().toString();
                String strReturnData="";
                if(!IP1.equals("")||!port1.equals("")||!IP2.equals("")||!port2.equals("")){
                    if(!IP1.equals("")){
                        if(!checkIP(IP1)){
                            Toast.makeText(SettingActivity.this, "设置IPServer的IP不合法", Toast.LENGTH_SHORT).show();
                            IPServerIP.setText("");
                            return;
                        }
                    }
                    if(!IP2.equals("")){
                        if(!checkIP(IP2)){
                            Toast.makeText(SettingActivity.this, "设置LocationServer的IP不合法", Toast.LENGTH_SHORT).show();
                            LocationServerIP.setText("");
                            return;
                        }
                    }
                    if(!port1.equals("")){
                        int temp=Integer.parseInt(port1);
                        if(temp<1024||temp>65535){
                            Toast.makeText(SettingActivity.this, "设置的IPServer端口号不合法,合法范围（1024-65535）", Toast.LENGTH_SHORT).show();
                            IPServerPort.setText("");
                            return;
                        }
                    }
                    if(!port2.equals("")){
                        int temp=Integer.parseInt(port2);
                        if(temp<1024||temp>65535){
                            Toast.makeText(SettingActivity.this, "设置的LocationServer端口号不合法,合法范围（1024-65535）", Toast.LENGTH_SHORT).show();
                            LocationServerPort.setText("");
                            return;
                        }
                    }
                    strReturnData=IP1+"#"+port1+"#"+IP2+"#"+port2;
                }
                Intent intent=new Intent();
                intent.putExtra("data_return",strReturnData);
                setResult(RESULT_OK,intent);
                finish();
            }
        });
    }

    /**
     * 用于把上个活动传过来的数据显示到控件
     */
    public void showRevData(){
        tv_promptInfo=(TextView)findViewById(R.id.IP_promptInfo);
        Intent intent=getIntent();
        String IP_infor=intent.getStringExtra("IPServerInfor");
        if(!IP_infor.equals("")){
            tv_promptInfo.setText(IP_infor);
        }
        String _connectInfor=intent.getStringExtra("ConnectInfor");
        String[] split = _connectInfor.split("#");
        int flag = 1;
        for (String val : split) {
            if (flag == 1) {
                if (!val.equals("")) {
                    IPServerIP.setText(val);
                }
                ++flag;
            } else if (flag == 2) {
                if (!val.equals("")) {
                    IPServerPort.setText(val);
                }
                ++flag;
            } else if (flag == 3) {
                if (!val.equals("")) {
                    LocationServerIP.setText(val);
                }
                ++flag;
            } else if (flag == 4){
                if (!val.equals("")) {
                    LocationServerPort.setText(val);
                }
            }
        }
    }
    @Override
    public void onBackPressed() {
        button_back.performClick();
        //super.onBackPressed();
    }
    //检查IP是否合法
    private boolean checkIP(String IP){
        //检查是否包含"."
        if(!IP.contains(".")){
            return false;
        }
        String[] split1=IP.split("\\.");
        int flag=1;
        for(String val:split1){
            if(flag==1){
                if(val.equals("")){
                    return false;
                }else{
                    int temp=Integer.parseInt(val);
                    if(temp<0||temp>255){
                        return false;
                    }
                    ++flag;
                }
            }else if(flag==2) {
                if (val.equals("")) {
                    return false;
                } else {
                    int temp = Integer.parseInt(val);
                    if (temp < 0 || temp > 255) {
                        return false;
                    }
                    ++flag;
                }
            }else if(flag==3) {
                if (val.equals("")) {
                    return false;
                } else {
                    int temp = Integer.parseInt(val);
                    if (temp < 0 || temp > 255) {
                        return false;
                    }
                    ++flag;
                }
            }else if(flag==4) {
                if (val.equals("")) {
                    return false;
                } else {
                    int temp = Integer.parseInt(val);
                    if (temp < 0 || temp > 255) {
                        return false;
                    }
                }
                ++flag;
            }
        }
        if(flag!=5){
            return false;
        }else {
            return true;
        }
    }
}
