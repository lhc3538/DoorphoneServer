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
            switch(strTemp[0]) {
                //家中固定终端发来id数据
                case "home":
                    synchronized (this) {
                        //不存在这个homeid，再写入
                        if ( !PublicData.sockHomeMap.containsKey(strHomeID) ) {
                            strType = strTemp[0];
                            strHomeID = strTemp[1];
                            PublicData.sockHomeMap.put(strHomeID,client);
                            out.println("success");
                            myDebug("初始化成功");
                            dealHome(); //处理家庭端后续操作
                        }
                        else {
                            out.println("exist");
                        }
                    }
                    break;
                //用户移动终端发来id数据
                case "mobile":
                    synchronized (this) {
                        //不存在这个homeid，再写入
                        if ( !PublicData.sockMobileMap.containsKey(strHomeID) ) {
                            strType = strTemp[0];
                            strHomeID = strTemp[1];
                            PublicData.sockMobileMap.put(strHomeID,client);
                            out.println("success");
                            myDebug("初始化成功");
                            dealMobile();   //处理移动端后续操作
                        }
                        else {
                            out.println("exist");
                        }
                    }
                    break;
                //用户移动终端发来 打开指令
                case "open":

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

                case "test":
                    Iterator<Map.Entry<String, Socket>> it = PublicData.sockHomeMap.entrySet().iterator();
                    myDebug("HOME");
                    while (it.hasNext()) {
                        Map.Entry<String, Socket> entry = it.next();
                        myDebug(entry.getKey()+"->"+entry.getValue());
                    }
                    it = PublicData.sockMobileMap.entrySet().iterator();
                    myDebug("MOBILE");
                    while (it.hasNext()) {
                        Map.Entry<String, Socket> entry = it.next();
                        myDebug(entry.getKey()+"->"+entry.getValue());
                    }
                    break;
                default:
                    out.println("invalid");
            }

        } catch (IOException e) {
            e.printStackTrace();
            //异常关闭前先将此socket从map中移除
            myDebug("终止");
            removeSocket();
            out.close();
            try {
                client.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     *
     */
    private void dealHome() {

    }

    private void dealMobile() {
        synchronized (this) {
            //检测家庭端和移动终端是否都上线了
            if ( PublicData.sockHomeMap.get(strHomeID) != null
                    && PublicData.sockMobileMap.get(strHomeID) != null) {
                //获取socket输出流
                PrintStream outTemp = new PrintStream(PublicData.sockHomeMap.get(strHomeID).getOutputStream());
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
            }
        }
    }

    /**
     * 从数据结构中删除当前线程的socket
     */
    private void removeSocket() {
        synchronized (this) {
            Iterator<Map.Entry<String, Socket>> it = PublicData.sockHomeMap.entrySet().iterator();
            while(it.hasNext())
            {
                Map.Entry<String, Socket> entry= it.next();
                if (entry.getValue().equals(client)) {
                    it.remove();
                }
            }
            it = PublicData.sockMobileMap.entrySet().iterator();
            while(it.hasNext())
            {
                Map.Entry<String, Socket> entry= it.next();
                if (entry.getValue().equals(client)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * 输出一行调试信息
     * @param str
     */
    private void myDebug(String str) {
        System.out.println(strType+"-"+strHomeID+":"+str);
    }

}