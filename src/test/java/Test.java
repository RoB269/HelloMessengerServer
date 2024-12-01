import com.github.rob269.Main;
import com.github.rob269.Message;
import com.github.rob269.User;
import com.github.rob269.io.DataBaseIO;
import com.github.rob269.rsa.Key;
import com.github.rob269.rsa.RSA;
import com.github.rob269.rsa.RSAKeys;
import com.google.common.hash.Hashing;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class Test {
    public static void main(String[] args) throws ClassNotFoundException {
        System.out.println(Hashing.sha256().hashString("#Rob269Password#", StandardCharsets.UTF_8));
    }
}