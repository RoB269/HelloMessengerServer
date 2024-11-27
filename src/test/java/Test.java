import com.github.rob269.Main;
import com.github.rob269.Message;
import com.github.rob269.User;
import com.github.rob269.io.DataBaseIO;
import com.github.rob269.rsa.Key;
import com.github.rob269.rsa.RSA;
import com.github.rob269.rsa.RSAKeys;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

public class Test {
    public static void main(String[] args) throws ClassNotFoundException {
        Main.class.getClassLoader().loadClass("com.github.rob269.Main");
        List<String[]> record = Main.MESSAGES.read(2, "Rob269");
        for (String[] line : record) {
            System.out.println(Arrays.toString(line));
        }
    }
}