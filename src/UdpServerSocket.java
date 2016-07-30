
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * udp服务器类
 * Created by lhc35 on 2016/7/30.
 */
public class UdpServerSocket {
    private DatagramSocket sock = null;
    private InetAddress clientIP = null;
    private int clientPort = 0;

    /**
     * 构造函数，绑定主机和端口.
     * @param port 端口
     * @throws Exception
     */
    public UdpServerSocket(int port) throws Exception {
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", port);
        sock = new DatagramSocket(socketAddress);
        System.out.println("udp服务端启动!");
    }

    /**
     * 接收数据包，该方法会造成线程阻塞.
     * @return 返回接收的数据串信息
     * @throws IOException
     */
    public byte[] receive() throws IOException {
        byte[] buffer = new byte[Package.SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        sock.receive(packet);
        if (clientIP == null)
            clientIP = packet.getAddress();
        if (clientPort == 0)
            clientPort = packet.getPort();
        return buffer;
    }

    /**
     * 将响应包发送给请求端.
     * @throws IOException
     */
    public void response(byte[] buffer) throws IOException {
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length, clientIP,clientPort);
        dp.setData(buffer);
        sock.send(dp);
    }

    /**
     * 获得发送回应的IP地址.
     * @return 返回回应的IP地址
     */
    public final InetAddress getResponseAddress() {
        return clientIP;
    }

    /**
     * 获得回应的主机的端口.
     * @return 返回回应的主机的端口.
     */
    public final int getResponsePort() {
        return clientPort;
    }

    /**
     * 关闭udp监听口.
     */
    public final void close() {
        try {
            sock.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}