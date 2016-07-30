/**
 * Created by lhc35 on 2016/4/24.
 */
public class Package {
    public static int BUFLEN = 1024;    //音频数据长度
    public static int SIZE = 1032;    //音频数据长度
    private byte[] packagebuf;

    private long id;
    private byte[] data;

    public Package() {
        id = 0;
        data = new byte[BUFLEN];
        packagebuf = new byte[BUFLEN+8];
    }

    public Package(byte[] buf) {
        id = 0;
        data = new byte[BUFLEN];
        packagebuf = new byte[BUFLEN+8];
        analysisBuf(buf);
    }

    public void analysisBuf(byte[] buf) {
        packagebuf = buf;
        id = bytes2Long(buf);
        for (int i=0;i<BUFLEN;i++) {
            data[i] = buf[i+8];
        }
    }   //解析byte类型的包

    public byte[] getPackageByte() {
        return packagebuf;
    }   //返回包的byte[]

    public long setId(long num) {
        id = num;
        byte[] idbuf = new byte[8];
        idbuf = long2Bytes(num);
        for (int i=0;i<8;i++)
            packagebuf[i] = idbuf[i];
        return id;
    }   //设置包的id并且更新packagebuf

    public byte[] setData(byte[] buf) {
        data = buf;
        for (int i=0;i<BUFLEN;i++)
            packagebuf[i+8] = buf[i];
        return data;
    }   //设置包的音频数据并且更新packagebuf

    public long getId() {
        return id;
    }   //返回包的id

    public byte[] getData() {
        return data;
    }   //返回包中音频数据

    private byte[] long2Bytes(long num) {
        byte[] byteNum = new byte[8];
        for (int ix = 0; ix < 8; ++ix) {
            int offset = 64 - (ix + 1) * 8;
            byteNum[7-ix] = (byte) ((num >> offset) & 0xff);
        }
        //System.out.printf("%d %d %d %d %d %d %d %d\n",byteNum[0],byteNum[1],byteNum[2],byteNum[3],byteNum[4],byteNum[5],byteNum[6],byteNum[7]);
        return byteNum;
    }

    private long bytes2Long(byte[] byteNum) {
        long num = 0;
        for (int ix = 0; ix < 8; ++ix) {
            num <<= 8;
            num |= (byteNum[7-ix] & 0xff);
        }
        return num;
    }
}
