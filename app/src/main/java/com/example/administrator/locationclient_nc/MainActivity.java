package com.example.administrator.locationclient_nc;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.DialerKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.R.attr.permission;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;


public class MainActivity extends AppCompatActivity {
    //用于支持C本源代码，导入库以及声明native方法
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    //    public native String stringFromJNI();
//    public native String InitGalois();
//    public native String UninitGalois();
//    public native int ADD(int i, int j);
//    public native byte[] matrixMul(byte[] matrixA, int aRow, int aCol, byte[] matrixB, int bRow, int bCol);
//    public native byte[] InverseMatrix(byte[] Data, int nK);
    public native byte[] NCDecoding(byte[] Data, int nLen);
    public native byte[] randomNC(byte[] Data, int row, int col, int N, int K);

    public int mutexJNI = 1;  //用以互斥使用JNI动态链接库，1表示允许进入，0则等待

    private TextView postionView;
    private LocationManager locationManager;
    private String locationProvider;
    private EditText ServerIP;
    private Location location;
    private TextView addressView;
    private Button button_connect;
    private Button button_sendToParent;
    private Button button_log_in;
    private Button button_autoRun;

    //当前应用包名
    public String packageName = "";
    //配置文件的路径
    public String myFolderPath = "";
    //用于连接父节点
    private boolean flag_getThread = true;
   // private int flag_wait = 1;   //如果只有一个信息，让其等待一次1秒
    private String locationServerIP = "";
    private int locationServerPort = 8260;
    private Socket socket_client = null;
    //private Socket socket=new Socket();
    //private BufferedReader in = null;
    //private DataInputStream in;
    private InputStream in;
    //private PrintWriter out = null;
    private DataOutputStream out = null;
    private int flag_connect = 0;
    //private String CharSetName = "UTF-8";
    private int parentNo = 0;   //父节点的编号，0表示是定位服务器
    //用于处理地址服务器的返回信息
    private static int msgLen = 255;

    //用于连接子节点
    private List<Socket> mList = new ArrayList<Socket>();
    private volatile ServerSocket server = null;
    private ExecutorService mExecutorService = null;   //线程池
    private volatile boolean flag_thread = true; //线程标志位
    private int flag_listener = 0;

    //用以显示流量信息
    private TextView tv_trafficInfor = null;
    //用于控制是否自动执行
    private boolean auto_run = false;
    //用于存储经纬度地址信息
    private String myLocationInfo = "";
    private int ChildNum = 0;
    private int rest_ChildNum = 0;
    private static int MaxChildNum = 10;
    private static int MaxUserNum = 20;

    //用于连接IP服务器
    private String IPServerIP = "172.17.94.1"; //这是一个默认值，真实值从IPConFig.txt读入
    private int IPServerPort = 8261;
    private String strNo;             //本机设备号
    private int myNo = 1;               //默认设备号为1
    private int groupUserNum = 1;
    private String promptInfor = "";

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    //以下用于记录孩子结点的信息，以便返回服务数据
    public class ChildInfo {
        public int childNo = 0;     //用来存子节点的编号，注意0，-1是一个错误值，用户编号从1开始

        public byte[] bt_locationInfo = new byte[msgLen];
        public byte[] bt_addressMsg = new byte[msgLen];

        public ChildInfo() {
            //对两个数组全赋0
            Arrays.fill(bt_locationInfo, (byte) 0);
            Arrays.fill(bt_addressMsg, (byte) 0);
        }

        public void clearInfor() {
            this.childNo = 0;
            Arrays.fill(this.bt_locationInfo, (byte) 0);
            Arrays.fill(this.bt_addressMsg, (byte) 0);
        }
    }

    public class ChildNode {
        public int visit = 1;   //当正在处理数据时，把此位设置为0，禁止修改
        // public boolean _write=false;  //当数据来时，告诉发送进程，正在写入数据，请等待
        // public boolean infNumCount=false;     //用以同步写入与读出
        public String childIP = "";
        public int Total_infNum = 0;
        public ChildInfo[] noAndLocation = new ChildInfo[MaxChildNum];
        public int current_infNum = 0;

        public ChildNode() {
            //初始化，不然会出现异常
            //用来存入最大10个孙结点的信息
            for (int i = 0; i < MaxChildNum; ++i) {
                noAndLocation[i] = new ChildInfo();
            }
        }

        //此方法用于返回地址信息后调用
        public void clearData() {
            //清除IP信息
            //this.childIP="";
            //当地址数据返回后，此位设置为1，可以进入访问
            visit = 1;
            //infNumCount=false;
            for (int i = 0; i < current_infNum; ++i) {
                this.noAndLocation[i].clearInfor();
            }
            //注意：只清空其infor信息，IP和编号都保留着
            this.current_infNum = 0;
            this.Total_infNum = 0;
        }

        //用来设置noAndLocation数组
        void setNoAndLocation(int _no, byte[] infor) {
            int arrayFlag = -1;
            for (int i = 0; noAndLocation[i].childNo != 0 && i < groupUserNum; ++i) {
                if (noAndLocation[i].childNo == _no) {
                    arrayFlag = i;
                    break;
                }
            }
            int len = infor[0];
            if (arrayFlag == -1) {
                //没有则加入
                noAndLocation[current_infNum].childNo = _no;
                //noAndLocation[current_infNum].getLatestInfor = true;
                for (int i = 0; i < len; ++i) {
                    noAndLocation[current_infNum].bt_locationInfo[i] = infor[i];
                }
                //noAndLocation[current_infNum].locationInfo = infor;
                ++current_infNum;
            } else {
                //有则只改信息
                //noAndLocation[arrayFlag].getLatestInfor = true;
                for (int i = 0; i < len; ++i) {
                    noAndLocation[arrayFlag].bt_locationInfo[i] = infor[i];
                }
                //noAndLocation[arrayFlag].locationInfo = infor;
            }
        }

    }

    //存入最大10个子节点的信息
    public ChildNode[] myChild = new ChildNode[MaxChildNum];
    public int currentChildNum = 1;          //用来标记该存哪个下标了，从1开始存 0存自己的信息



    public class ServiceDelay{
        //当得到一组starTime和endTime时，置为TRUE，设置startTime后置为FALSE
        public boolean canSet=true;
        public long startTime = 0;
        public long endTime = 0;
        //用作计算服务延迟的服务次数
        public int serverCount=0;
        //单位ms
        public long avgServerTime=0;
        public void setStartTime(){
            if(canSet){
                startTime=System.currentTimeMillis();
                canSet=false;
            }else{
                //do something
            }
        }
        public void setAvgServerTime(){
            //++serverCount;
            endTime=System.currentTimeMillis();
            long _1serverTime=endTime-startTime;
            if(_1serverTime>30000){
                //如果大于30S 则是统计错误，不统计此次数据
                clearStartTime();
                return;
            }
            long totalServerTime=(this.avgServerTime*this.serverCount)+_1serverTime;
            ++this.serverCount;
            try {
                this.avgServerTime = totalServerTime / this.serverCount;
                this.canSet=true;
            }catch (Exception e){
                clearStartTime();
            }
        }
        //清除计时用于关闭父节点连接时执行
        public void clearStartTime(){
            this.canSet=true;
            this.startTime=0;
            this.endTime=0;
        }
    }
    public ServiceDelay myServerDelay=new ServiceDelay();


    //添加孩子结点的IP信息到myChild数组，返回的是当前孩子在myChild中的下标
    public int setMyChildArray(int totalInfNum, String socketIP) {
        //从1开始搜索，0存的是本机信息
        for (int i = 1; i < currentChildNum; ++i) {
            if (myChild[i].childIP.equals(socketIP)) {
                //存入这个结点包含的设备号数目
                long oldTime = System.currentTimeMillis();
                double waitTime = 0;
                //如果上一次地址数据还未返回，设置最多等待3秒
                while (myChild[i].visit == 0 && waitTime < 3) {
                    //当不可访问时，进入等待，当visit=1时向下执行
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {

                    }
                    long newTime = System.currentTimeMillis();
                    waitTime = (newTime - oldTime) / 1000.0;
                }
                myChild[i].visit = 0;   //不可再进入访问
                //  myChild[i]._write=true;  //不允许再读
                myChild[i].Total_infNum = totalInfNum;
                return i;
            }
        }
        //第一次连接时，剩余未连接子节点数目
        --rest_ChildNum;
        //while(myChild[currentChildNum].visit==0){
        //当不可访问时进入等待
        //}
        //myChild[currentChildNum].visit=0;      //不可再次进入访问
        //myChild[currentChildNum]._write=true;  //不允许再读

        myChild[currentChildNum].childIP = socketIP;
        myChild[currentChildNum].Total_infNum = totalInfNum;
        int k = currentChildNum;
        ++currentChildNum;
        return k;
    }

    //计算收到的信息个数
    private int getInfoNum() {
        int count = 0;       //对机主信息和子节点信息实现统一化处理
        for (int i = 0; i < currentChildNum; ++i) {
            //如果没正在写，则读入数量
            // if(!myChild[i]._write) {
            //myChild[i]._read=true;
            count += myChild[i].current_infNum;
            //myChild[i].infNumCount=true;
            // }
            //count += myChild[i].getLatestInfoNum();
        }
        return count;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //用于检查存储权限
        int storage_permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (storage_permission != PackageManager.PERMISSION_GRANTED) { // We don't have permission so prompt the user
             ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE );
        }
        //获取当前包名
        packageName = getPackageName();
        //为应用创建一个文件夹
        myFolderPath = createPath("LocationClient_NC");
        if (myFolderPath.equals("")) {
            Toast.makeText(this, "创建文件夹失败", Toast.LENGTH_SHORT).show();
        }
        if (readIPConfig()) {
            //从文件读取IP成功
        }

//        String path=getPackageName();
//        createPath(getPackageName());
        //对myChild数组进行逐个声明，否则会报未声明异常
        //对自定义信息缓存类进行初始化
        for (int i = 0; i < MaxChildNum; ++i) {
            myChild[i] = new ChildNode();
        }

        //用以显示流量
        tv_trafficInfor = (TextView) findViewById(R.id.tv_trafficInfor);
        getTrafficInfor();
        //检查设备是否联网
        ConnectivityManager con = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        boolean wifi = con.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();
        boolean internet = con.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
        if (wifi | internet) {
            //执行相关操作
            Toast.makeText(getApplicationContext(), "已连接网络", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "无网络连接，功能不可用", Toast.LENGTH_SHORT).show();
        }


        //获取经纬度并显示
        addressView = (TextView) findViewById(R.id.addressView);
        postionView = (TextView) findViewById(R.id.positionView);
        String locationStr = "经度：" + "\n" + "纬度：";       //为经纬度显示设一个初始值
        postionView.setText(locationStr);
        //获取经纬度
        try {
            //判断是否开启GPS定位
            LocationManager alm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            if (alm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                //   Toast.makeText(this, "GPS模块正常" ,Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "请开启GPS！", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);  //打开设置界面
                startActivityForResult(intent, 0); //此为设置完成后返回到获取界面
                Thread.sleep(3000);                //等待3秒后向下执行
            }

            //获取地理位置管理器
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            //获取所有可用的位置提供器
            List<String> providers = locationManager.getProviders(true);
            if (providers.contains(LocationManager.GPS_PROVIDER)) {
                //如果是GPS
                locationProvider = LocationManager.GPS_PROVIDER;
            } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                //如果是Network
                locationProvider = LocationManager.NETWORK_PROVIDER;
            } else {
                Toast.makeText(MainActivity.this, "没有可用的位置提供器，无定位权限", Toast.LENGTH_SHORT).show();
                //return ;
            }
            //获取Location，先要检查权限,注意检查权限时包名不要搞错
            PackageManager pm = getPackageManager();
            boolean permission = (PackageManager.PERMISSION_GRANTED ==
                    pm.checkPermission("android.permission.ACCESS_FINE_LOCATION", packageName));
            if (permission) {
                location = locationManager.getLastKnownLocation(locationProvider);
                if (location != null) {
                    //不为空,显示地理位置经纬度
                    showLocation(location);
                }
                //监视地理位置变化
                //每隔3000毫秒更新一次经纬度
                locationManager.requestLocationUpdates(locationProvider, 3000, 1, locationListener);
            } else {
                Toast.makeText(MainActivity.this, "权限不足无法使用定位", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

//        promptInfo = (TextView) findViewById(promptInfo);     //用于显示IP服务器返回的提示信息
        ServerIP = (EditText) findViewById(R.id.Edit_IP);
        ServerIP.setKeyListener(DialerKeyListener.getInstance());//为IP输入框添加数字键盘监听器
        button_log_in = (Button) findViewById(R.id.button_log_in);
        button_log_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //连接服务器和接收数据线程
                strNo = ServerIP.getText().toString();
                if (strNo.equals("")) {
                    Toast.makeText(MainActivity.this, "请输入设备号（1-19）", Toast.LENGTH_SHORT).show();
                    return;
                }

                int temp = 0;
                try {
                    temp = Integer.parseInt(strNo);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "错误的设备号，请输入设备号（1-19）", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (temp > 19 || temp < 1) {
                    Toast.makeText(MainActivity.this, "错误的设备号，请输入设备号（1-19）", Toast.LENGTH_SHORT).show();
                    return;
                }

                //在设备号改变前先清空子节点缓存和服务延迟缓存，断开与父节点的连接
                if (flag_connect == 1) {
                    //断开与父节点连接
                    button_connect.performClick();  //点击连接父节点
                }
                for (int i = 0; i < currentChildNum; ++i) {
                    myChild[i].clearData();
                }
                currentChildNum = 1;

                //关闭监听端口
                if (flag_listener == 1) {
                    closeListener();
                }

                myNo = temp;
                strNo = "0" + myNo;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Socket socket = null;
                            try {
                                //Socket socket = new Socket("121.40.229.42", 8261);
                                socket = new Socket(IPServerIP, IPServerPort);
                                //socket.setSoTimeout(3000);
                                //socket = new Socket("192.168.0.103", 8261);
                            } catch (Exception e) {
                                //连接失败
                                Message FIPMSG = new Message();
                                FIPMSG.what = FCONNECTIPSERVER;
                                handler.sendMessage(FIPMSG);
                                return;
                            }
                            //连接成功
                            BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream(), "GBK"));        //用于接收信息,指定字符集GBK很重要
                            PrintWriter write = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);   //用发送信息
                            //DataOutputStream send = new DataOutputStream(socket_client.getOutputStream());
                            //byte[] bt_send=new byte[2];
                            //send.write(bt_send);
                            write.println(strNo);
                            String str = "";
                            //int count = 3;      //用来控制接收数据的次数
                            while (true) {
                                if (socket.isConnected()) {
                                    if (!socket.isInputShutdown()) {
                                        if ((str = read.readLine()) != null) {
                                            //把接收到的信息显示到IP控件
                                            String[] split1 = str.split("#");
                                            int flag = 0;
                                            for (String val : split1) {
                                                if (flag == 0) {
                                                    //接收提示信息
                                                    promptInfor = val;
                                                    ++flag;
                                                } else if (flag == 1) {
                                                    //检查父节点是不是地址服务器
                                                    parentNo = Integer.parseInt(val);     //父节点设备号，等于0说明是服务器
                                                    if (parentNo == 0) {
                                                        //CharSetName = "GBK";
                                                    }
                                                    ++flag;
                                                } else if (flag == 2) {
                                                    //存入孩子结点的数目用于控制 打开监听端口按钮
                                                    ChildNum = Integer.parseInt(val);          //存入子节点的数目
                                                    rest_ChildNum = ChildNum;
                                                    if (ChildNum != 0) {
                                                        //若有孩子结点自动点击开启端口
                                                        if (flag_listener == 0) {
                                                            Message HaveChild_OpenListener = new Message();
                                                            HaveChild_OpenListener.what = HAVACHILD_OPENLISTENER;
                                                            handler.sendMessage(HaveChild_OpenListener);
                                                        }
                                                    }
                                                    ++flag;
                                                } else if (flag == 3) {
                                                    //存入实际用户的数目，用于控制NC
                                                    groupUserNum = Integer.parseInt(val);
                                                    ++flag;
                                                } else if (flag == 4) {
                                                    //用以显示IP
                                                    Message GetIP = new Message();
                                                    GetIP.obj = val;
                                                    GetIP.what = GETIP;
                                                    handler.sendMessage(GetIP);
                                                    break;      //跳出循环
                                                }
                                            }
//                                            if (count == 3) {
//                                                count--;
//                                                promptInfor = str;
////                                                Message TransInfo = new Message();
////                                                TransInfo.obj = str;
////                                                TransInfo.what = TRANSINFO;
////                                                handler.sendMessage(TransInfo);
//                                            } else if (count == 2) {
//                                                count--;
//                                                //获取父节点设备号，和子节点数目。之间用，隔开。
//                                                //根据乱码规律写的一个解码，乱码规律前面全0，后面是IP地址数据
//                                                byte[] bt = str.getBytes("GBK");
//                                                int isNumCount = 0;
//                                                //for (int i = bt.length - 1; bt[i] != 0 && i >= 0; --i) {     //从后向前记录非0数据数目
//                                                for (int i = bt.length - 1; bt[i] > 0 && i >= 0; --i) {     //从后向前记录非0数据数目
//                                                    ++isNumCount;
//                                                }
//                                                byte[] b = new byte[isNumCount];
//                                                for (int i = (bt.length - isNumCount), j = 0; i < bt.length; i++, j++) {
//                                                    b[j] = bt[i];
//                                                }
//                                                String ParentAndChildCount = new String(b, "UTF-8");
//                                                String[] split1 = ParentAndChildCount.split("#");
//                                                int flag = 1;
//                                                for (String val : split1) {
//                                                    if (flag == 1) {
//                                                        int parentNo = Integer.parseInt(val);     //父节点设备号，等于0说明是服务器
//                                                        if (parentNo == 0) {
//                                                            CharSetName = "GBK";
//                                                        }
//                                                        ++flag;
//                                                    } else if (flag == 2) {
//                                                        //存入孩子结点的数目用于控制 打开监听端口按钮
//                                                        ChildNum = Integer.parseInt(val);          //存入子节点的数目
//                                                        rest_ChildNum = ChildNum;
//                                                        if (ChildNum != 0) {
//                                                            //若有孩子结点自动点击开启端口
//                                                            if (flag_listener == 0) {
//                                                                Message HaveChild_OpenListener = new Message();
//                                                                HaveChild_OpenListener.what = HAVACHILD_OPENLISTENER;
//                                                                handler.sendMessage(HaveChild_OpenListener);
//                                                            }
//                                                        }
//                                                        ++flag;
//                                                    } else if (flag == 3) {
//                                                        //存入实际用户的数目，用于控制NC
//                                                        groupUserNum = Integer.parseInt(val);
//                                                    }
//                                                }
//                                            } else if (count == 1) {
//                                                //获取父节点IP
//                                                count--;
//                                                //根据乱码规律写的一个解码，乱码规律前面全0，后面是IP地址数据
//                                                byte[] bt = str.getBytes("GBK");
//                                                int isNumCount = 0;
//                                                //为什么接收到的数据会多出很多0，-2？
//                                                //for (int i = bt.length - 1; bt[i] != 0 && i >= 0; --i) {     //从后向前记录非0数据数目
//                                                for (int i = bt.length - 1; bt[i] > 0 && i >= 0; --i) {     //从后向前记录非0数据数目
//                                                    ++isNumCount;
//                                                }
//                                                byte[] b = new byte[isNumCount];
//                                                for (int i = (bt.length - isNumCount), j = 0; i < bt.length; i++, j++) {
//                                                    b[j] = bt[i];
//                                                }
//
//                                                String newStr = new String(b, "UTF-8");
//                                                Message GetIP = new Message();
//                                                GetIP.obj = newStr;
//                                                GetIP.what = GETIP;
//                                                handler.sendMessage(GetIP);
//                                                break;      //跳出循环
//                                        }
                                        } else {

                                        }
                                    }
                                }
                                // }
                                //关闭流，关闭socket
                                write.close();
                                read.close();
                                socket.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            //执行关闭线程
                        }
                    }
                }).start();
            }
        });


        button_connect = (Button) findViewById(R.id.button_connect);
        button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag_connect == 0) {
                    //连接服务器和接收数据线程
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String IPTemp = ServerIP.getText().toString();
//                                String myIP = getHostIP();
//                                if (myIP.equals(IPTemp)) {
//                                    Message ConnectMyIPError = new Message();
//                                    ConnectMyIPError.what = CONNECTMYIPERROR;
//                                    handler.sendMessage(ConnectMyIPError);
//                                    return;
//                                }
                                if (!checkIP(IPTemp)) {
                                    //检查IP是否合法
                                    Message IPInputError = new Message();
                                    IPInputError.what = IPINPUTERROR;
                                    handler.sendMessage(IPInputError);
                                    return;
                                }
                                locationServerIP = IPTemp;
                                //socket = new Socket("121.40.229.42", 8260);
                                socket_client = new Socket(locationServerIP, locationServerPort);
                                //设置输出输入缓存区
                                socket_client.setSendBufferSize(1*1024); //1K
                                //设置socket底层接收缓冲为32k
                               // socket_client.setReceiveBufferSize(32*1024);
                                //关闭Nagle算法.立即发包
                                socket_client.setTcpNoDelay(true);
                                //socket_client.setSoTimeout(3000);
                                //socket.connect(new InetSocketAddress("172.17.235.1", 8260),5000);  //设置5秒连接延迟,此方法会出现无法重连的错误
                                //连接成功
                                Message msg = new Message();
                                msg.what = CONNECTE_SMSG;
                                handler.sendMessage(msg);
                                //声明输入输出流
                                in = socket_client.getInputStream();
                                out = new DataOutputStream(socket_client.getOutputStream());
                                //启动接收父节点信息线程
                                getAddressInfor();
                            } catch (Exception e) {
                                //连接失败
                                Message FMSG = new Message();
                                FMSG.what = CONNECTE_FMSG;
                                handler.sendMessage(FMSG);
                                return;
                            }
                        }
                    }).start();
                } else {
                    //断开父节点连接操作
                    try {
                        //关闭流
                        out.close();
                        in.close();
                        //关闭Socket
                        socket_client.close();
                    } catch (Exception e) {
                        return;
                    }
                    //清除计时
                    myServerDelay.clearStartTime();
                    //用来关闭接收线程
                    flag_getThread = false;
                    button_connect.setText("连接父节点");
                    flag_connect = 0;
                    //Toast.makeText(MainActivity.this, "断开连接", Toast.LENGTH_SHORT).show();
                    button_sendToParent.setEnabled(false);  //发送给父节点不可用
//                    if (auto_run) {
//                        //当是自动执行时，断开连接后，应关闭自动执行
//                        button_autoRun.performClick();
//                    }
                    //若是正在自动执行则关闭
                    if (auto_run) {
                        button_autoRun.performClick();
                    }

                    button_autoRun.setEnabled(false);       //自动执行不可用
                }
            }

        });


        button_sendToParent = (Button) findViewById(R.id.button_sendToParent);   //退出
        button_sendToParent.setEnabled(false);
        button_sendToParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendToParent();
            }
        });


        //执行自动执行
        button_autoRun = (Button) findViewById(R.id.button_AutoRun);
        button_autoRun.setEnabled(false);
        button_autoRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!auto_run) {
                    //自动执行
                    button_sendToParent.setEnabled(false);
                    button_autoRun.setText("手动执行");
                    auto_run = true;
                    AutoRun3();  //开启自动执行
                } else {
                    //手动执行
                    if (flag_connect == 1) {
                        button_sendToParent.setEnabled(true);
                    }
                    button_autoRun.setText("自动执行");
                    auto_run = false;
                }
            }
        });


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    //为了防止主线程阻塞，给发送给子节点新开一个线程
    public void sendToParent() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("这是发送给父节点线程");
                try {
                    if (socket_client.isConnected()) {
                        if (!socket_client.isOutputShutdown()) {
                            //当不是自动执行时
                            if (!auto_run) {
                                //存入信息
                                // myChild[0]._write=true;
                                if (!myLocationInfo.equals("")) {
                                    byte[] bt_infor = myLocationInfo.getBytes();
                                    byte[] bt_myInfor = random_NC(bt_infor, bt_infor.length, myNo);
                                    myChild[0].setNoAndLocation(myNo, bt_myInfor);
                                    myChild[0].Total_infNum = 1;
                                    myChild[0].current_infNum = 1;
                                }
                                // myChild[0]._write=false;
                                //myChild[0].infNumCount=true;
                            }
                            int infnum = getInfoNum();
                            if (infnum == 0) {
                                //没有信息可供发送
                                return;
                            }
//                            } else if (infnum == 1 && ChildNum != 0 && flag_wait == 1 && auto_run) {
//                                //如果只有一个信息，而且这个节点是有孩子节点的，且是自动执行时，则等待一秒钟
//                                flag_wait = 0;
//                                Thread.sleep(1000);
//                                //button_sendToParent.performClick();
//                                sendToParent();
//                                return;  //只执行一次，没有return，会递归执行两次
//                            }
//                            flag_wait = 1;


                            byte[][] sendToParentInfo = new byte[infnum][msgLen];
                            int arrayIndex = 0;
                            //从myChild数组中取出子节点的经纬度信息
                            for (int i = 0; i < currentChildNum; ++i) {

                                if (myChild[i].current_infNum < myChild[i].Total_infNum) {
                                    int restInf = myChild[i].Total_infNum - myChild[i].current_infNum;
                                    String s = "还未收到子节点" + myChild[i].childIP + "的" + restInf + "个数据，该子节点共" + myChild[i].Total_infNum + "个数据。";
                                    //Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                                    Message sentToParMsg = new Message();
                                    sentToParMsg.what = SENDTOPARMSG;
                                    sentToParMsg.obj = s;
                                    handler.sendMessage(sentToParMsg);
                                }
                                for (int j = 0; j < myChild[i].current_infNum; ++j) {
                                    int len = myChild[i].noAndLocation[j].bt_locationInfo[0];
                                    for (int k = 0; k < len; ++k) {
                                        sendToParentInfo[arrayIndex][k] = myChild[i].noAndLocation[j].bt_locationInfo[k];
                                    }
                                    ++arrayIndex;
                                }
                            }

                            //对数据进行再编码
                            System.out.println("对信息进行再编码");
                            byte[][] msgToParent = re_encodeData_NC(sendToParentInfo, infnum);
                            byte[][] locationData = decodeInfor_NC(msgToParent, infnum, msgToParent[0][0]);
                            String[] testDecode=new String[infnum];
                            for(int i=0;i<infnum;++i){
                                testDecode[i]=new String(locationData[i]);
                            }
                            System.out.println("再编码完成");
                            //byte[][] b=decodeInfor_NC(msgToParent,2,44);
                            //String s=new String(b[0]);
                            //String s1=new String(b[1]);
                            //用以实现随机发送的效果，第一次发送的信息随机
                            //如果上一级是服务器,则先发送一个0数组,用于控制一次丢包，不会影响下一个数据
                            if(parentNo==0&&infnum != 1){
                                byte[] ctrlMissPkg=new byte[3];
                                Arrays.fill(ctrlMissPkg,(byte)0);
                                out.write(ctrlMissPkg);
                                //Thread.sleep(100);
                            }
                            int firstSend = 0;
                            if (infnum == 1) {
                                firstSend = 0;
                            } else {
                                int ranNum = infnum;
                                firstSend = new Random().nextInt(ranNum);
                            }
                            //int firstSend = 1;
                            //byte[] b=msgToParent[firstSend].getBytes();
                            if (msgToParent[firstSend][1] == myNo) {
                                //用以统计服务延迟
                                myServerDelay.setStartTime();
                            }
                            out.write(msgToParent[firstSend]);

                            //out.println(b);
                            //延迟0.1秒再执行下次发送，不然会丢失服务器不能全部接收
//                            if (infnum != 1) {
//                              Thread.sleep(100);
//                            }
                            for (int i = 0; i < infnum; ++i) {
                                if (i == firstSend) {
                                    continue;
                                }
                                if (msgToParent[i][1] == myNo) {
                                    myServerDelay.setStartTime();
                                }
                                out.write(msgToParent[i]);
                                //out.println(msgToParent[i]);
                               // Thread.sleep(100);
                            }
                        }
                    }
                } catch (Exception e) {

                }
                //FLAG=0;
            }
        }).start();
    }

    public void getAddressInfor() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                flag_getThread = true;
                //用以处理接收到的地址信息
                byte[] bytes = new byte[msgLen];
                byte[][] totalMsg = new byte[groupUserNum][msgLen];
                int[] userNo = new int[groupUserNum];
                Arrays.fill(userNo, 999);  //设置一个999极大值，用来标记错误
                boolean first_info = true;
                boolean first_childInfoGet = true;
                int infoNum = 0;
                int currentflag = 0;
                int infoLen = 0;
                int childNo = 0;
                //屏蔽偶数次接收执行,为什么接收时会出现   C++服务器发送一次，java客户端两次响应的情况？
                boolean flag_noRev2 = true;
                while (flag_getThread) {
                    if (socket_client.isConnected()) {
                        if (!socket_client.isInputShutdown()) {
                            //判断是否收到数据
                            try {
                                //int msglength=in.read(bytes);
                                if (in.read(bytes, 0, msgLen) > -1 && flag_noRev2) {
                                    System.out.println("这是接收父节点信息线程");
                                    System.out.println("接收到父节点数据");
                                    if (parentNo == 0) {
                                        flag_noRev2 = false;
                                    }
                                    if (first_info) {
                                        if (bytes[0] < 0) {
                                            int abs = Math.abs(bytes[0]);
                                            infoLen = 256 - abs;
                                        } else {
                                            infoLen = bytes[0];
                                        }
                                        infoNum = (int) bytes[2];
                                        first_info = false;
                                    }
                                    //把返回的地址信息存入byte数组，等待传输完成后进行解码
                                    userNo[currentflag] = bytes[1];    //存入编号
                                    for (int i = 0; i < infoLen; ++i) {
                                        totalMsg[currentflag][i] = bytes[i];
                                    }
                                    //用来记录已经收到了几个数据
                                    ++currentflag;
                                    if (currentflag == infoNum) {
                                        //flag_noRev2=false;
                                        //开始解码
                                        Arrays.sort(userNo);    //对编号数组进行排序,排序后顺序对应着addressData的顺序，userNo和addressData捆绑使用
                                        System.out.println("对父节点数据进行解码");
                                        byte[][] addressData = decodeInfor_NC(totalMsg, infoNum, infoLen);   //addressData中存储的是纯数据
                                        //用于调试
                                        String[] addressTemp=new String[infoNum];
                                        for(int i=0;i<infoNum;++i){
                                            addressTemp[i]=new String(addressData[i], "UTF-16LE");
                                        }

                                        if (addressData == null) {
                                            System.out.println("对父节点数据解码出现异常");
                                            continue;
                                        }
                                        System.out.println("解码完成，显示地址");
                                        for (int i = 0; i < infoNum; ++i) {
                                            if (userNo[i] == myNo) {
                                                //String myAddress=new String(addressData[i], "UTF-16LE");
                                                Message Show_Address = new Message();
                                                Show_Address.what = SHOW_ADDRESS;
                                                Show_Address.arg1 = infoLen - 4 - groupUserNum;
                                                Show_Address.obj = new String(addressData[i], "UTF-16LE");  //使用UTF-16LE解码
                                                handler.sendMessage(Show_Address);
                                                System.out.println("已执行完显示");
                                                //清空0下标数据
                                                myChild[0].clearData();
                                            } else {
                                                System.out.println("存入子节点信息");
                                                if (first_childInfoGet) {
                                                    //第一次接收到子节点服务信息时，使发送给子节点可用
                                                    first_childInfoGet = false;

                                                }
                                                //按编号no寻找用户，并存入其地址信息
                                                childNo = userNo[i];
                                                for (int k = 1; k < currentChildNum; ++k) {
                                                    for (int j = 0; j < myChild[k].current_infNum; ++j) {
                                                        if (myChild[k].noAndLocation[j].childNo == childNo) {
                                                            //myChild[k].noAndLocation[j].addressMsg = str_address[i];
                                                            //此处应该存入编码后的数据
                                                            System.out.println("对子节点地址信息随机编码");
                                                            byte[] afterEncode = random_NC(addressData[i], infoLen - 4 - groupUserNum, childNo);
                                                            System.out.printf("对子节点地址信息编码完毕");
                                                            int col = infoLen;
                                                            for (int n = 0; n < col; ++n) {
                                                                myChild[k].noAndLocation[j].bt_addressMsg[n] = afterEncode[n];
                                                            }
                                                            break;
                                                        }
                                                    }
                                                }
                                                if (i == (infoNum - 1)) {
                                                    System.out.println("存完信息，唤醒发送给子节点");
                                                    //表示所有信息已经存入
                                                    //把地址信息发送给子节点
                                                    Message sendtochild = new Message();
                                                    sendtochild.what = SENDTOCHILD;
                                                    handler.sendMessage(sendtochild);
                                                }
                                            }
                                        }
                                        //用于下次接收时使用
                                        first_info = true;
                                        first_childInfoGet = true;
                                        infoNum = 0;
                                        currentflag = 0;
                                        infoLen = 0;
                                        childNo = 0;
                                    }
                                } else {
                                    flag_noRev2 = true;
                                }
                            } catch (Exception e) {
                                //用于下次接收时使用
                                first_info = true;
                                first_childInfoGet = true;
                                infoNum = 0;
                                currentflag = 0;
                                infoLen = 0;
                                childNo = 0;
                            }
                        }
                    }
                }
            }
        }).start();
    }

    public void openListener() {
        flag_listener = 1;
        System.out.println("flag:" + flag_thread);
        ServerThread serverThread = new ServerThread();
        serverThread.start();
    }

    public void closeListener() {
        try {
            flag_listener = 0;
            flag_thread = false;
            server.close();
            for (int p = 0; p < mList.size(); p++) {
                Socket s = mList.get(p);
                s.close();
            }
            mExecutorService.shutdownNow();
            //button_openListener.setText("开启监听端口");
            //button_sendToChild.setEnabled(false);      //使发送给子节点按钮不可用
            //Toast.makeText(MainActivity.this, "监听端口已关闭", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void sendMsgToChild() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("这是发送给子节点线程");
                //用来记下已经返回数据的
                int[] _childArray = new int[groupUserNum];
                Arrays.fill(_childArray, 0);
                for (int p = 0; p < mList.size(); p++) {
                    Socket s = mList.get(p);
                    //从1处开始搜索，0存的是本机信息
                    for (int i = 1; i < currentChildNum; ++i) {
                        //按socket的IP查找匹配相应子节点
                        if (myChild[i].childIP.equals(s.getInetAddress().toString())) {
                            int _inforNum = myChild[i].current_infNum;
                            int _count = 0;   //用来记录实际存入的地址个数
                            byte[][] msgToChild = new byte[_inforNum][msgLen];
                            for (int j = 0; j < myChild[i].current_infNum; ++j) {
                                //取出地址信息
                                int len = myChild[i].noAndLocation[j].bt_addressMsg[0];
                                if (len < 1) {
                                    //len为0证明没有地址信息,使用len<1作判断，是为了避免未知的负数错误
                                    continue;
                                }
                                //byte[] bt_msg = new byte[len];
                                for (int n = 0; n < len; ++n) {
                                    // bt_msg[n] = myChild[i].noAndLocation[j].bt_addressMsg[n];
                                    msgToChild[_count][n] = myChild[i].noAndLocation[j].bt_addressMsg[n];
                                }
                                ++_count;
                            }
                            DataOutputStream pout = null;
                            //对信息进行再编码，后发送给子节点
                            if(_count==0){
                                //证明没有需要返回的地址数据
                                continue;
                            }
                            byte[][] re_msgToChild = re_encodeData_NC(msgToChild, _count);
                            for (int j = 0; j < _count; ++j) {
                                try {
                                    pout = new DataOutputStream(s.getOutputStream());
                                    pout.write(re_msgToChild[j]);
                                    //pout.close();   //加上之后，会出现socket断开
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            //地址返回给子节点后，清空上一次存储的子节点信息
                            //myChild[i].clearData();
                            for (int k = 0; k < groupUserNum; ++k) {
                                if (_childArray[k] == i) {
                                    break;
                                }
                                if (_childArray[k] == 0) {
                                    //把已经返回信息的孩子结点下标存入数据
                                    _childArray[k] = i;
                                    break;
                                }
                            }
                        }
                    }
                }
                System.out.println("发送给自己点信息完毕");
                //清除已经返回信息的孩子结点信息
                for (int i = 0; i < groupUserNum; ++i) {
                    int k = _childArray[i];
                    if (k != 0) {
                        myChild[k].clearData();
                    }
                }
            }
        }).start();
    }


    //点击back键执行home键的功能
    @Override
    public void onBackPressed() {
        //button_exit.performClick();     //点击退出
        //super.onBackPressed();
        //实现Home键效果
        //super.onBackPressed();这句话一定要注掉,不然又去调用默认的back处理方式了
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
    }


    /**
     * 显示地理位置经度和纬度信息
     *
     * @param location
     */
    private void showLocation(Location location) {
        String locationStr = "经度：" + location.getLongitude() + "\n"
                + "纬度：" + location.getLatitude();
        myLocationInfo = location.getLongitude() + "," + location.getLatitude();     //本地经纬度存入数组
        postionView.setText(locationStr);
    }

    /**
     * LocationListern监听器
     * 参数：地理位置提供器、监听位置变化的时间间隔、位置变化的距离间隔、LocationListener监听器
     */

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onStatusChanged(String provider, int status, Bundle arg2) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onLocationChanged(Location location) {
            //如果位置发生变化,重新显示
            showLocation(location);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (locationManager != null) {
            //移除监听器
            PackageManager pm = getPackageManager();
            boolean permission = (PackageManager.PERMISSION_GRANTED ==
                    pm.checkPermission("android.permission.ACCESS_FINE_LOCATION", packageName));
            if (permission) {
                locationManager.removeUpdates(locationListener);
            } else {
                Toast.makeText(this, "权限不足无法使用定位", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //用于线程之间通信的消息
    public static final int SHOW_ADDRESS = 1;
    public static final int CONNECTE_SMSG = 2;
    public static final int CONNECTE_FMSG = 3;
    public static final int SOPEN_LISTENER = 4;
    public static final int CLOSE_CONNECT = 5;
    public static final int TRANSINFO = 7;
    public static final int GETIP = 8;
    public static final int FOPEN_LISTENER = 9;
    public static final int FCONNECTIPSERVER = 10;
    public static final int CHILDONLINE = 11;
    public static final int IPINPUTERROR = 12;
    public static final int HAVACHILD_OPENLISTENER = 13;
    public static final int RECEIVEFINISH = 15;
    public static final int AUTORUN = 16;
    public static final int TRAFFICSTATIS = 17;
    public static final int SENDTOCHILD = 18;
    public static final int SENDTOPARMSG = 19;
    public static final int GETMSGERROR = 20;
    public static final int CONNECTMYIPERROR = 21;
    //用于接收线程的处理结果
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_ADDRESS:
                    //显示10次服务的平均服务时间
                    try {
                        //统计服务延迟
                        myServerDelay.setAvgServerTime();
                        //显示地址
                        String address = msg.obj.toString();
                        boolean _isChinese=isChinese(address);
                        if(_isChinese){
                            //如果是中文字串，则显示
                            addressView.setText(address);
                            changeColor(addressView);
                        }else{
                            //传输或解码出现不是中文的乱码，则重连，只针对地址服务器
                            if(auto_run) {
                                if (flag_connect == 1) {
                                    button_connect.performClick();  //断开父节点
                                    button_connect.performClick();  //连接父节点
                                    //启动自动执行
                                    button_autoRun.performClick();
                                }
                            }
                        }

                        //检查是否显示正确错误则重连父节点
                        // byte[] bt1=(addressView.getText().toString()).getBytes("UTF-16LE");
                        // int length=bt1.length;
                        //以下用来处理解码出的地址出现异常的情况
                        //根据UTF-16LE特点，无论中文英文，每个字符占两个字节
                        //int length = addressView.getText().toString().length() * 2;
//                        if (length != msg.arg1&& auto_run) { //是自动执行时产生错误才会执行下面代码
//                            //重连父节点，用于处理自动执行过程中，解码错误的影响
//                            if (flag_connect == 1) {
//                                button_connect.performClick();  //断开父节点
//                                button_connect.performClick();  //连接父节点
//                                //启动自动执行
//                                button_autoRun.performClick();
//
//                            }
//                        }
                        System.out.printf("显示地址完成");
                    } catch (Exception e) {
                        System.out.printf("统计服务延迟代码或显示地址出现错误");
                    }
                    break;
                case CONNECTE_SMSG:
                    //连接父节点成功
                    button_autoRun.setEnabled(true);
                    button_connect.setText("断开父节点");
                    flag_connect = 1;
                    //Toast.makeText(MainActivity.this, "连接服务器成功", Toast.LENGTH_SHORT).show();
                    if (!auto_run) {
                        //使发送给子节点按钮可用
                        button_sendToParent.setEnabled(true);
                    }
                    break;
                case CONNECTE_FMSG:
                    Toast.makeText(MainActivity.this, "连接父节点失败", Toast.LENGTH_SHORT).show();
                    break;
                case SOPEN_LISTENER:
                    //打开监听端口成功
                    flag_listener = 1;
                    //button_openListener.setText("关闭监听端口");
                    // Toast.makeText(MainActivity.this, "端口开启成功，等待接收数据", Toast.LENGTH_SHORT).show();
                    break;
                case FOPEN_LISTENER:
                    Toast.makeText(MainActivity.this, "端口开启失败", Toast.LENGTH_SHORT).show();
                    break;
                case RECEIVEFINISH:
                    String ip = msg.obj.toString();
                    String showMsg = "接收" + ip + "数据完毕";
                    Toast.makeText(MainActivity.this, showMsg, Toast.LENGTH_SHORT).show();
                    // button_sendToParent.performClick();  //有孩子信息到来激活发送
//                     if(auto_run) {
//                         sendToParent();
//                     }
                    break;
                case CLOSE_CONNECT:
                    button_connect.setText("连接父节点");
                    flag_connect = 0;
                    Toast.makeText(MainActivity.this, "断开父节点", Toast.LENGTH_SHORT).show();
                    break;
                case TRANSINFO:
//                    promptInfo.setText(msg.obj.toString());
                    break;
                case GETIP:
                    //把查询到的父节点IP显示到控件
                    String IPTemp = msg.obj.toString();
//                    String myIP = getHostIP();
//                    if (myIP.equals(IPTemp)) {
//                        Message ConnectMyIPError = new Message();
//                        ConnectMyIPError.what = CONNECTMYIPERROR;
//                        handler.sendMessage(ConnectMyIPError);
//                        return;
//                    }
                    ServerIP.setText(IPTemp);
                    //若没连接父节点，则点击连接
                    if (flag_connect == 0) {
                        button_connect.performClick();  //连接父节点
                    }
                    //若已经是连接状态，则断开重连
                    if (flag_connect == 1) {
                        button_connect.performClick();  //断开父节点
                        button_connect.performClick();  //连接父节点
                    }
                    //注意这个地方
//                    if (auto_run) {
//                        AutoRun3();    //用以每3秒更新一下数据
//                    }
                    //统计流量,单位字节，每3秒更新一次
                    //getTrafficInfor();
                    break;
                case FCONNECTIPSERVER:
                    Toast.makeText(MainActivity.this, "连接IP服务器失败", Toast.LENGTH_SHORT).show();
                    break;
                case CHILDONLINE:
                    Toast.makeText(MainActivity.this, msg.obj + "已连接", Toast.LENGTH_SHORT).show();
                    break;
                case IPINPUTERROR:
                    Toast.makeText(MainActivity.this, "请输入正确的IP地址", Toast.LENGTH_SHORT).show();
                    break;
                case HAVACHILD_OPENLISTENER:
                    //有孩子结点，自动点击开启监听端口按钮
                    //button_openListener.performClick();
                    openListener();   //打开监听端口8260
                    break;
                case AUTORUN:
                    //用以每3秒自动执行一次
                    // button_sendToParent.performClick();
                    try {
                        sendToParent();
                    } catch (Exception e) {
                        System.out.printf("发送给父节点代码异常");
                    }
                    break;
                case TRAFFICSTATIS:
                    //用以统计流量信息
                    tv_trafficInfor.setText(msg.obj.toString());
                    break;
                case SENDTOCHILD:
                    //向孩子发送信息, 屏蔽发送给子节点异常
                    try {
                        sendMsgToChild();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "发送给子节点出现异常", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case SENDTOPARMSG:
                    //显示还有几个信息未收到，正常运行时不会显示，若有显示，接收子节点信息出错
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case GETMSGERROR:
                    //用以处理接收父节点信息出错
                    Toast.makeText(MainActivity.this, "接收父节点信息出错", Toast.LENGTH_SHORT).show();
                    break;
                case CONNECTMYIPERROR:
                    Toast.makeText(MainActivity.this, "不可连接本机IP，请重新获取IP", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    //Server端的主线程
    class ServerThread extends Thread {
        public void stopServer() {
            try {
                if (server != null) {
                    server.close();
                    System.out.println("close task successed");
                }
            } catch (IOException e) {
                System.out.println("close task failded");
            }
        }
        @Override
        public void run() {
            try {
                server = new ServerSocket(locationServerPort);   //父节点的监听端口保持与locationServer的一致
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                //监听端口开启失败
                Message FOpen_Listener = new Message();
                FOpen_Listener.what = FOPEN_LISTENER;
                handler.sendMessage(FOpen_Listener);
                System.out.println("S2: Error");
                e1.printStackTrace();
                return;
            }
            //监听端口开启成功
            Message SOpen_Listener = new Message();
            SOpen_Listener.what = SOPEN_LISTENER;
            handler.sendMessage(SOpen_Listener);

            mExecutorService = Executors.newCachedThreadPool();  //创建一个线程池
            System.out.println("服务器已启动...");
            Socket client = null;
            while (flag_thread) {
                try {
                    System.out.println("S3: Error");
                    client = server.accept();
                    System.out.println("S4: Error");
                    //把客户端放入客户端集合中
                    mList.add(client);
                    mExecutorService.execute(new Service(client)); //启动一个新的线程来处理连接
                } catch (IOException e) {
                    System.out.println("S1: Error");
                    e.printStackTrace();
                }
            }
        }
    }


    //处理与client对话的线程
    class Service implements Runnable {
        private volatile boolean kk = true;
        private Socket socket;
        //private BufferedReader in = null;
        private InputStream in;
        private String msg = "";

        public Service(Socket socket) {
            this.socket = socket;
            try {
                //in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                in = socket.getInputStream();
                //提示哪个IP已连接
                Message ChildOnLine = new Message();
                ChildOnLine.what = CHILDONLINE;
                ChildOnLine.obj = socket.getInetAddress();
                handler.sendMessage(ChildOnLine);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        byte[] bytes = new byte[msgLen];

        public void run() {
            boolean isFirstInfo = true;
            int infor_num = 0;
            int rest_infor_num;
            int iChild = -1;
            int _no = 0;
            int infoLen = 0;
            //需要测试下，看是否和上面接收父几点一样需要 屏蔽第二次接收
            // boolean flag_noRev2=true;
            while (kk) {
                try {
                    if (in.read(bytes, 0, msgLen) > -1) {
                        //flag_noRev2 = false;
                        System.out.println("这是接收子节点信息线程");
                        if (isFirstInfo) {
                            if (bytes[0] < 0) {
                                int abs = Math.abs(bytes[0]);
                                infoLen = 256 - abs;
                            } else {
                                infoLen = bytes[0];
                            }
                            //_no = bytes[1];
                            infor_num = bytes[2];
                            //把IP存入数组用于返回数据
                            String str_IP = socket.getInetAddress().toString();
                            //返回的iChild是该子节点在myChild数组中的下标
                            iChild = setMyChildArray(infor_num, str_IP);
                            isFirstInfo = false;
                        }
                        _no = bytes[1];
                        byte[] b = new byte[infoLen];
                        for (int i = 0; i < infoLen; ++i) {
                            b[i] = bytes[i];
                        }
                        //存入子节点数组
                        myChild[iChild].setNoAndLocation(_no, b);
                        //查看是否接收到该结点的所有数据
                        --infor_num;
                        if (infor_num == 0) {
                            System.out.println("接收子节点信息完毕");
                            isFirstInfo = true;
                            //不显示接收IP数据完毕
//                            Message Receive_Finish = new Message();
//                            Receive_Finish.what = RECEIVEFINISH;
//                            Receive_Finish.obj = socket.getInetAddress();
//                            handler.sendMessage(Receive_Finish);
                        }
                    } else {
                        //flag_noRev2=true;
                    }

                } catch (IOException e) {
                    System.out.println("close");
                    kk = false;
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }

        //向客户端发送信息
        public void sendmsg(String msg) {
            System.out.println(msg);
            PrintWriter pout = null;
            try {
                pout = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);
                pout.println(msg);
                pout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //该函数是对一个数据进行的编码,这里data是原始数据没别的内容

    /**
     * 此方法是对一个数据进行随机编码，非冗余
     *
     * @param data 输入原数据的byte[]数组，
     * @param len  数组长度
     * @param _no  此数据的属主用户编号
     * @return 返回的是组装后的数据 len+_no+1+groupUserNum groupUserNum个编码系数 编码后的数据
     */
    public byte[] random_NC(byte[] data, int len, int _no) {
        int N = 1;
        int K = 1;
        //用于互斥访问JNI
        while (mutexJNI == 0) {
        }
        mutexJNI = 0;
        byte[] encodeMatrix = randomNC(data, 1, len, N, K);
        mutexJNI = 1;

        //0存信息长度，1存no，2存信息数目1，3存groupUserNum，4开始存groupUserNum个字节存入编码系数，后面是数据
        int colNum = 1 + 1 + 1 + 1 + groupUserNum + len;
        byte[] afterEncode = new byte[colNum];
        afterEncode[0] = (byte) colNum;   //长度
        afterEncode[1] = (byte) _no;       //no
        afterEncode[2] = 1;              //信息数目，这里是对1个数据进行编码
        afterEncode[3] = (byte) groupUserNum;
        for (int i = 4; i < groupUserNum + 4; ++i) {
            //i=4对应第1个位置，在第No个位置存入编码系数，其他位置存0
            if ((i - 3) == _no) {
                afterEncode[i] = encodeMatrix[1];
            } else {
                afterEncode[i] = 0;
            }
        }
        for (int i = groupUserNum + 4; i < colNum; ++i) {
            //从encodeMatrix[2]开始读取编码后的数据
            afterEncode[i] = encodeMatrix[i - groupUserNum - 2];
        }
        return afterEncode;
    }

    /**
     * 此方法用于对num个数据进行再编码，非冗余
     *
     * @param data 为已经进行过数据封装的数据（随机编码或在编码后的数据）
     * @param num  数据的行数
     * @return 返回数据的格式和random_NC方法一致
     */
    public byte[][] re_encodeData_NC(byte[][] data, int num) {
        //获取信息的最大长度
        int maxSize = data[0][0] - 4;
        for (int i = 1; i < num; ++i) {
            int temp = data[i][0] - 4;
            if (temp > maxSize) {
                maxSize = temp;
            }
        }

        byte[] matrixForEncode = new byte[num * maxSize];
        Arrays.fill(matrixForEncode, (byte) 0);
        int[] user_no = new int[num];
        //int ii=0;
        for (int i = 0; i < num; ++i) {
            user_no[i] = data[i][1];
            //matrixForEncode[ii++]=(byte)num;
            //从4开始为编码系数和数据
            for (int j = 4; j < data[i][0]; ++j) {
                matrixForEncode[i * maxSize + j - 4] = data[i][j];
            }
        }
        //encodeMatrix是num*（1+num+maxSize)
        //用于互斥访问JNI
        while (mutexJNI == 0) {
        }
        mutexJNI = 0;
        byte[] encodeMatrix = randomNC(matrixForEncode, num, maxSize, num, num);
        mutexJNI = 1;
        int colNum = 1 + 1 + 1 + 1 + maxSize;   //No 加上编码后的数据
        byte[][] encodePack = new byte[num][colNum];
        for (int i = 0; i < num; ++i) {
            encodePack[i][0] = (byte) colNum;
            encodePack[i][1] = (byte) user_no[i];
            encodePack[i][2] = (byte) num;
            encodePack[i][3] = (byte) groupUserNum;
            for (int j = 4; j < colNum; ++j) {
                //encodeMatrix从1+num开始存
                encodePack[i][j] = encodeMatrix[i * (1 + num + maxSize) + num + j - 3];
            }
        }
        return encodePack;
    }

    /**
     * 此方法用于解码
     *
     * @param decodeData 进行过网络编码的数据数组，格式同随机编码和再编码后的数据格式
     * @param row        行数
     * @param col        列数
     * @return 注意：返回的原始数据的byte数组
     */
    public byte[][] decodeInfor_NC(byte[][] decodeData, int row, int col) {
        //复制数据到数组
        try {
            int[] userNo = new int[row];
            byte[][] data = new byte[row][col];
            for (int i = 0; i < row; ++i) {
                userNo[i] = decodeData[i][1];    //存入no
                for (int j = 0; j < col; ++j) {
                    data[i][j] = decodeData[i][j];
                }
            }
            Arrays.sort(userNo);
            //重组encodeData
            int len = data[0][0];
            int infnum = data[0][2];
            int group_userNum = data[0][3];
            int colNum = col - 3 - (group_userNum - infnum);
            byte[] dataForDecode = new byte[row * colNum];
            int ii = 0;   //用作转化为一维数组的下标
            for (int i = 0; i < row; ++i) {
                dataForDecode[ii] = data[i][2];
                ++ii;
                //取出编码系数,从4开始存编码系数和数据
                for (int j = 4; j < group_userNum + 4; ++j) {
                    int flag = 0;
                    for (int k = 0; k < row; ++k) {
                        if (j - 3 == userNo[k]) {
                            flag = 1;
                            break;
                        }
                    }
                    if (flag == 1) {
                        dataForDecode[ii] = data[i][j];
                        ++ii;
                    }
                }
                //取出地址数据
                for (int n = group_userNum + 4; n < len; ++n) {
                    dataForDecode[ii] = data[i][n];
                    ++ii;
                }
            }

            int dataLen = col - group_userNum - 4;   //减去len，no，K,groupUserNum所在的4列
            int num = row;
            //用以互斥访问JNI
            while (mutexJNI == 0) {
            }
            mutexJNI = 0;
            byte[] originData = NCDecoding(dataForDecode, colNum);
            mutexJNI = 1;

            int jj = 0;
            byte[][] twoDim_originData = new byte[num][dataLen];
            for (int i = 0; i < num; ++i) {
                for (int j = 0; j < dataLen; ++j) {
                    twoDim_originData[i][j] = originData[jj];
                    ++jj;
                }
            }

            return twoDim_originData;
        } catch (Exception e) {
            System.out.println("对父节点数据解码出现异常");
            return null;
        }
    }

    //用于改变TextView的字体颜色
    String[] colorValue = {"#FF0000", "#00FF00", "#0000FF"};
    int colorNum = 3;
    int colorFlag = 0;
    public void changeColor(TextView tv) {
        String color = colorValue[colorFlag];
        tv.setTextColor(Color.parseColor(color));
        colorFlag = (colorFlag + 1) % colorNum;
    }


    /**
     * 获取ip地址
     *
     * @return
     */
    public static String getHostIP() {
        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            Log.i("yao", "SocketException");
            e.printStackTrace();
        }
        return hostIp;

    }


    //用以控制每3秒向服务器发送一次定位数据
    public void AutoRun3() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //当正在向父节点发送数据或是正在接收数据或是上次地址还未返回时，不执行
                while (auto_run) {
                    try {
                        //存入本机信息
                        byte[] bt_infor = myLocationInfo.getBytes();
                        byte[] bt_myInfor = random_NC(bt_infor, bt_infor.length, myNo);
                        myChild[0].setNoAndLocation(myNo, bt_myInfor);
                        myChild[0].Total_infNum = 1;
                        myChild[0].current_infNum = 1;
                        // myChild[0].visit = 0;
                        // myChild[0]._write = false;

                        Message AutoMsg = new Message();
                        AutoMsg.what = AUTORUN;
                        handler.sendMessage(AutoMsg);
                        Thread.sleep(3000);
//                        //叶节点停3秒，非叶节点停2秒，保证所有节点至少3秒会发送一次信息,因为非叶节点要等待一秒
//                        if (ChildNum != 0) {
//                            Thread.sleep(2000);
//                        } else {
//                            Thread.sleep(3000);
//                        }
                    } catch (Exception e) {
                        System.out.printf("自动执行代码块出现错误");
                    }
                }

            }
        }).start();
    }


    //用以获取流量数据
    public void getTrafficInfor() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //实现显示已运行时间功能
                //String serverTimeInfor = ",获取" + averageTimeNum + "次地址后,显示平均服务时延。";
                long oldTime = System.currentTimeMillis();
                PackageManager pm;
                ApplicationInfo ai;
                long oldsendTra = 0;
                long oldgetTra = 0;
                try {
                    pm = getPackageManager();
                    ai = pm.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES);
                    oldsendTra = TrafficStats.getUidTxBytes(ai.uid);  //发出的字节数
                    oldgetTra = TrafficStats.getUidRxBytes(ai.uid);   //接收的字节数
                } catch (Exception e) {

                }
                while (true) {
                    try {
                        long newTime = System.currentTimeMillis();
                        long runminute = (newTime - oldTime) / (1000 * 60);
                        //用以显示平均服务延迟
                        int count=myServerDelay.serverCount;
                        long delay=myServerDelay.avgServerTime;
                        String serverTimeInfor = "," + count + "次服务的平均服务延迟为"+ delay+"ms。" ;

                        pm = getPackageManager();
                        ai = pm.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES);
                        long sendTra = TrafficStats.getUidTxBytes(ai.uid) - oldsendTra;  //发出的字节数
                        long getTra = TrafficStats.getUidRxBytes(ai.uid) - oldgetTra;   //接收的字节数
                        long trafficStatis = sendTra + getTra;
                        //转化为KB
                        DecimalFormat format = new DecimalFormat("###.##");
                        String str_sendTra = format.format((float) sendTra / 1024);
                        String str_getTra = format.format((float) getTra / 1024);
                        String str_trafficStatis = format.format((float) trafficStatis / 1024);
                        Message trafficMsg = new Message();
                        trafficMsg.what = TRAFFICSTATIS;
                        trafficMsg.obj = "已运行" + runminute + "分钟" + serverTimeInfor + "\n" + "发出的字节数为" + str_sendTra + "KB," + "接收的字节数为" + str_getTra + "KB。\n" + "总计流量为" + str_trafficStatis + "KB。";
                        handler.sendMessage(trafficMsg);
                        Thread.sleep(3000);  //每三秒执行一次
                    } catch (Exception e) {

                    }
                }
            }
        }).start();
    }


    //private MenuItem autoRun = null;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {       //用于菜单功能
        getMenuInflater().inflate(R.menu.main, menu);
        //需要动态更改的MenuItem，在下面添加
        //autoRun = menu.findItem(R.id.autoRun_item);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setting_item:
                //用以把IP服务器的返回信息传给设置界面，并从设置界面获取信息
                Intent intent_setting = new Intent(MainActivity.this, SettingActivity.class);
                intent_setting.putExtra("IPServerInfor", promptInfor);
                //传入用于连接的IP和端口,显示在设置界面
                String _connectInfor=IPServerIP+'#'+IPServerPort+'#'+locationServerIP+'#'+locationServerPort;
                intent_setting.putExtra("ConnectInfor",_connectInfor);
                startActivityForResult(intent_setting, 1);
                break;
            case R.id.about_item:
                Intent about = new Intent(MainActivity.this, About_Activity.class);
                startActivity(about);
                break;
            case R.id.about_quit:
                //执行退出程序
                if (flag_listener == 1) {
                    closeListener();  //关闭监听端口
                }
                //把IP服务器的IP写入配置文件
                writeToFile();

                //执行退出
                finish();              //先销毁活动窗口，再退出，不会出现闪屏
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);
                Process.killProcess(Process.myPid());
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }


    //用来接收设置界面的返回值
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    String returnedData = data.getStringExtra("data_return");
                    String[] split = returnedData.split("#");
                    int flag = 1;
                    for (String val : split) {
                        if (flag == 1) {
                            if (!val.equals("")) {
                                IPServerIP = val;
                            }
                            ++flag;
                        } else if (flag == 2) {
                            if (!val.equals("")) {
                                IPServerPort = Integer.parseInt(val);
                            }
                            ++flag;
                        } else if (flag == 3) {
                            if (!val.equals("")) {
                                //locationServerIP=val;
                                ServerIP.setText(val);
                            }
                            ++flag;
                        } else {
                            if (!val.equals("")) {
                                locationServerPort = Integer.parseInt(val);
                            }
                        }
                    }
                }
                break;
        }
    }


    /**
     * @param folderName 文件夹名
     * @return 返回文件夹操作路径，后带斜杠
     */
    private static String createPath(String folderName) {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            String mainPath = Environment.getExternalStorageDirectory().getPath() + File.separator + folderName;
            File destDir = new File(mainPath);
            if (!destDir.exists()) {
                //如果不存在则创建
                destDir.mkdirs();//在根创建了文件夹hello
            }
            String folderPath = mainPath + File.separator;
            return folderPath;
        }
        return null;
    }

    public void writeToFile(){
        //把IP服务器的IP写入文件
        String toFile = myFolderPath + "IPConfig.txt";
        File myFile = new File(toFile);
        if (!myFile.exists()) {   //不存在则创建
            try {
                myFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try {
            //传递一个true参数，代表不覆盖已有的文件。并在已有文件的末尾处进行数据续写,false表示覆盖写
            FileWriter fw = new FileWriter(myFile, false);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(IPServerIP);
            //bw.write("测试文本");
            bw.flush();
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从配置文件读取IP地址
     * @return
     */
    public boolean readIPConfig() {
        String toFile = myFolderPath + "IPConfig.txt";
        File myFile = new File(toFile);
        if (!myFile.exists()) {   //不存在则创建
            return false;
        }
        String str_IP = "";
        try {
            FileReader fr = new FileReader(myFile);
            BufferedReader bReader = new BufferedReader(fr);
            str_IP = bReader.readLine();
            bReader.close();
            fr.close();
            System.out.println(str_IP);
            if (!str_IP.equals("") && checkIP(str_IP)) {
                IPServerIP = str_IP;
                return true;
            }
            return false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 简单检查IP是否合法
     * @param IP
     * @return
     */
    public boolean checkIP(String IP) {
        //检查是否包含"."
        if (!IP.contains(".")) {
            return false;
        }
        if (IP.length() > 15 || IP.length() < 7) {
            return false;
        }
        String[] split1 = IP.split("\\.");
        int flag = 1;
        for (String val : split1) {
            if (flag == 1) {
                if (val.equals("")) {
                    return false;
                } else {
                    int temp = Integer.parseInt(val);
                    if (temp < 0 || temp > 255) {
                        return false;
                    }
                    ++flag;
                }
            } else if (flag == 2) {
                if (val.equals("")) {
                    return false;
                } else {
                    int temp = Integer.parseInt(val);
                    if (temp < 0 || temp > 255) {
                        return false;
                    }
                    ++flag;
                }
            } else if (flag == 3) {
                if (val.equals("")) {
                    return false;
                } else {
                    int temp = Integer.parseInt(val);
                    if (temp < 0 || temp > 255) {
                        return false;
                    }
                    ++flag;
                }
            } else if (flag == 4) {
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
        if (flag != 5) {
            return false;
        } else {
            return true;
        }
    }
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };// Check if we have write permission

    /**
     * 判断字符串是否是中文，可以包含有阿拉伯数字，英文，空格等（32到122的字符）
     * @param string
     * @return
     */
    public static boolean isChinese(String string){
        int n=0;
        for(int i=0;i<string.length();++i){
            n=(int)string.charAt(i);
            //32到122  从空格的ASCII码到z的ASCII码
            if(!(19968<=n&&n<40869)&&(!(32<=n&&n<=122))){
                //if(!(19968<=n&&n<40869)){
                return false;
            }
        }
        return true;
    }
    @Override
    public void onStart() {
        super.onStart();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }
}

