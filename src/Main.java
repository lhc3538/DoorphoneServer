import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) {
        //服务端在20006端口监听客户端请求的TCP连接
        ServerSocket server = null;
        try {
            server = new ServerSocket(1025);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Socket client = null;
        while(true){
            //等待客户端的连接，如果没有获取连接
            try {
                client = server.accept();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            System.out.println("接收到新连接请求");
            //为每个客户端连接开启一个线程
            new Thread(new ServerThread(client)).start();
        }
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
