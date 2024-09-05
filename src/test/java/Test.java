import com.github.rob269.Main;
import com.github.rob269.User;
import com.github.rob269.io.DataBaseIO;
import com.github.rob269.rsa.Key;
import com.github.rob269.rsa.RSA;
import com.github.rob269.rsa.RSAKeys;

import java.math.BigInteger;

public class Test {
    public static void main(String[] args) {
        System.out.println(Main.RSA_KEYS.isExist(5, "#TEST1#"));
    }
}