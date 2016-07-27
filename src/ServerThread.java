import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by lhc35 on 2016/7/26.
 */
public class ServerThread implements Runnable {

    private Socket client = null;
    public ServerThread(Socket client){
        this.client = client;
    }

    @Override
    public void run() {
        PrintStream out = null;
        try {
            //获取Socket的输出流，用来向客户端发送数据
            out = new PrintStream(client.getOutputStream());
            //获取Socket的输入流，用来接收从客户端发送过来的数据
            BufferedReader buf = new BufferedReader(new InputStreamReader(client.getInputStream()));
            while(true){
                //接收从客户端发送过来的数据
                String str =  buf.readLine();
                String[] strTemp = str.split(";");
                String strHomeID = "";
                switch(strTemp[0]) {
                    //家中固定终端发来id数据
                    case "home":
                        strHomeID = strTemp[1];
                        synchronized (this) {
                            //不存在这个homeid，再写入
                            if ( !PublicData.sockHomeMap.containsKey(strHomeID) ) {
                                PublicData.sockHomeMap.put(strHomeID,client);
                                out.println("success");
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
                                PublicData.sockMobileMap.put(strHomeID,client);
                                out.println("success");
                            }
                            else {
                                out.println("exist");
                            }
                        }
                        break;
                    //用户移动终端发来 打开指令
                    case "open":
                        //检测家庭端和移动终端是否都上线了
                        if (PublicData.sockHomeMap.containsKey(strHomeID)
                                && PublicData.sockMobileMap.containsKey(strHomeID)) {
                            //检测指令是不是同一个终端发出
                            if (PublicData.sockMobileMap.get(strHomeID).equals(client)) {
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
                                    }catch (IOException e) {
                                    }
                                }
                                for (;i<65536;i++) {
                                    try {
                                        ServerSocket sockTemp = new ServerSocket(i);
                                        //i端口可用
                                        outTemp.println(String.valueOf(i));
                                        sockTemp.close();
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
                                //通知请求方，线路被占用
                                out.println("busy");
                            }
                        }
                        else {
                            //通知请求方，被请求端未上线
                            out.println("offline");
                        }
                        break;
                    //用户移动终端发来 关闭指令
                    case "close":
                        //获取socket输出流
                        PrintStream outTemp = new PrintStream(PublicData.sockHomeMap.get(strHomeID).getOutputStream());
                        //通知两终端关闭对讲线程
                        outTemp.println("close");
                        out.println("close");
                        break;
                    default:
                        out.println("invalid");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            //异常关闭前先将此socket从map中移除
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

}