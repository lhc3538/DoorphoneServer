import java.io.IOException;
import java.net.DatagramSocket;

/**
 * 转发线程控制类
 * Created by lhc35 on 2016/7/30.
 */
public class TransmitThread{
    private Thread threadHomeToMobile = null;
    private Thread threadMobileToHome = null;
    private UdpServerSocket sockHome = null;
    private UdpServerSocket sockMobile = null;

    /**
     * 开启转发线程
     */
    public  void run(int home_port,int mobile_port) {
        try {
            sockHome = new UdpServerSocket(home_port);
            sockMobile = new UdpServerSocket(mobile_port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        threadHomeToMobile = new Thread(runnableHomeToMobile);
        threadHomeToMobile.start();
        threadMobileToHome = new Thread(runnableMobileToHome);
        threadMobileToHome.start();
    }

    /**
     * 从家庭端到移动端转发任务
     */
    private Runnable runnableHomeToMobile = new Runnable() {
        @Override
        public void run() {
            byte[] buffer;
            try {
                buffer = sockHome.receive();
                sockMobile.response(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 从移动端到家庭端转发任务
     */
    private Runnable runnableMobileToHome = new Runnable() {
        @Override
        public void run() {
            byte[] buffer;
            try {
                buffer = sockMobile.receive();
                sockHome.response(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 停止转发
     */
    public void stop() {
        if (threadHomeToMobile != null)
            threadHomeToMobile.stop();
        if (threadMobileToHome != null)
            threadMobileToHome.stop();
        sockHome.close();
        sockMobile.close();
    }
}
