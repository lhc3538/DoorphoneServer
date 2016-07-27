import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lhc35 on 2016/7/26.
 */
class PublicData {
    static Map<String,Socket> sockHomeMap = new HashMap<>();
    static Map<String,Socket> sockMobileMap = new HashMap<>();
}
