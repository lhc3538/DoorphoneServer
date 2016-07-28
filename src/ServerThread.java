import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * Created by lhc35 on 2016/7/26.
 */
public class ServerThread implements Runnable {

    private Socket client = null;
    private String strHomeID = "";  //家庭标识
    private String strType = "";    //此线程链接的终端类型
    PrintStream out = null; //socket输出流（用于发送）
    BufferedReader in = null;  //socket输入流（用于接收）

    public ServerThread(Socket client){
        this.client = client;
    }

    @Override
    public void run() {
        try {
            //获取Socket的输出流，用来向客户端发送数据
            out = new PrintStream(client.getOutputStream());
            //获取Socket的输入流，用来接收从客户端发送过来的数据
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            //接收从客户端发送过来的数据，接收身份辨识数据
            String str =  in.readLine();
            String[] strTemp = str.split(";");
            strType = strTemp[0];
            strHomeID = strTemp[1];
            switch(strType) {
                //家中固定终端发来id数据
                case "home":
                    dealHome(); //处理家庭端操作
                    break;
                //用户移动终端发来id数据
                case "mobile":
                    dealMobile();   //处理移动端操作
                    break;
                //用户移动终端发来 关闭指令
                case "close":
                    synchronized (this) {
                        if (PublicData.sockHomeMap.get(strHomeID) != null) {
                            //获取socket输出流
                            PrintStream outTemp = new PrintStream(PublicData.sockHomeMap.get(strHomeID).getOutputStream());
                            //通知两终端关闭对讲线程
                            outTemp.println("close");
                        }
                    }
                    out.println("close");
                    myDebug("即将关闭");
                    break;
                default:
                    out.println("invalid");
            }

        } catch (IOException e) {
            e.printStackTrace();
            //异常关闭前先将此socket从map中移除
            myDebug("终止");
        }
        unloadThread(); //卸载线程
    }

    /**
     * 处理家庭端的连接
     *
     */
    private void dealHome() {
        boolean existHomeID;
        synchronized (this) {
            existHomeID = PublicData.sockHomeMap.containsKey(strHomeID);
            if (!existHomeID)   //不存在这个homeid，再写入
                PublicData.sockHomeMap.put(strHomeID, client);
        }
        if (!existHomeID) {
            out.println("success");
            myDebug("家庭端初始化成功");
        }
        else {
            out.println("exist");
            return;
        }
        //等待，直到家庭端退出
        while(true) {
            try {
                String str = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                //家庭端已退出
                myDebug("家庭端异常终止");
                //检测对应的移动端是否在线
                synchronized (this) {
                    existHomeID = PublicData.sockMobileMap.containsKey(strHomeID);
                    if (existHomeID) {
                        try {
                            //获取socket输出流
                            PrintStream outTemp = new PrintStream(PublicData.sockMobileMap.get(strHomeID).getOutputStream());
                            //通知移动端关闭
                            outTemp.println("close");
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                if (existHomeID) {
                    //关闭udp转发线程
                }
                return;
            }
        }
    }

    /**
     * 处理移动端的连接
     *
     */
    private void dealMobile() {
        boolean existHomeID;
        synchronized (this) {
            existHomeID = PublicData.sockMobileMap.containsKey(strHomeID);
            if (!existHomeID)   //不存在这个homeid，再写入
                PublicData.sockMobileMap.put(strHomeID, client);
        }
        if (!existHomeID) {
            out.println("success");
            myDebug("移动端初始化成功");
        }
        else {
            out.println("exist");
            return;
        }

        //获取socket输出流
        PrintStream outTemp = null;
        //检测家庭端是否都上线了
        synchronized (this) {
            existHomeID = PublicData.sockHomeMap.containsKey(strHomeID);
            if (existHomeID) {
                try {
                    outTemp = new PrintStream(PublicData.sockHomeMap.get(strHomeID).getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (existHomeID) {
            //通知移动和家庭终端开启 对讲线程
            int i;
            for (i=1026;i<65536;i++) {
                try {
                    ServerSocket sockTemp = new ServerSocket(i);
                    //i端口可用
                    out.println(String.valueOf(i));
                    sockTemp.close();
                    myDebug("移动终端udp端口号为"+String.valueOf(i));
                    break;
                }catch (IOException e) {
                    myDebug("移动端udp端口号分发失败");
                }
            }
            for (i++;i<65536;i++) {
                try {
                    ServerSocket sockTemp = new ServerSocket(i);
                    //i端口可用
                    outTemp.println(String.valueOf(i));
                    sockTemp.close();
                    myDebug("家庭终端udp端口号为"+String.valueOf(i));
                    break;
                }catch (IOException e) {
                    myDebug("家庭端udp端口号分发失败");
                }
            }
            //端口全被占用
            if (i==65536) {
                out.println("jam");
                outTemp.println("jam");
            }
        }
        else {
            //通知请求方，被请求端未上线
            out.println("offline");
            return;
        }

        while(true) {
            try {
                String str = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                myDebug("移动端异常终止");
                //检测对应的家庭端是否在线
                synchronized (this) {
                    existHomeID = PublicData.sockHomeMap.containsKey(strHomeID);
                    if (existHomeID) {
                        //通知家庭端关闭
                        if (outTemp != null) {
                            outTemp.println("close");
                        }
                    }
                }
                if (existHomeID) {
                    //关闭udp转发线程
                }
                return;
            }
        }
    }

    /**
     * 从数据结构中删除当前线程的socket
     */
    private void removeSocket() {
        if (strType.equals("home")) {
            synchronized (this) {
                Iterator<Map.Entry<String, Socket>> it = PublicData.sockHomeMap.entrySet().iterator();
                while(it.hasNext())
                {
                    Map.Entry<String, Socket> entry= it.next();
                    if (entry.getValue().equals(client)) {
                        it.remove();
                    }
                }
            }
        }
        else if (strType.equals("mobile")) {
            synchronized (this) {
                Iterator<Map.Entry<String, Socket>> it = PublicData.sockMobileMap.entrySet().iterator();
                while(it.hasNext())
                {
                    Map.Entry<String, Socket> entry= it.next();
                    if (entry.getValue().equals(client)) {
                        it.remove();
                    }
                }
            }
        }
    }

    private void unloadThread() {
        removeSocket();
        out.close();
        try {
            in.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        myDebug("卸载完成，退出");
    }

    /**
     * 输出一行调试信息
     * @param str
     */
    private void myDebug(String str) {
        System.out.println(strType+"-"+strHomeID+":"+str);
    }

}