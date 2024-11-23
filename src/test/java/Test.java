import com.github.rob269.Main;
import com.github.rob269.Message;
import com.github.rob269.User;
import com.github.rob269.io.DataBaseIO;
import com.github.rob269.rsa.Key;
import com.github.rob269.rsa.RSA;
import com.github.rob269.rsa.RSAKeys;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Test {
    public static void main(String[] args) throws ClassNotFoundException {
        Main.class.getClassLoader().loadClass("com.github.rob269.Main");
        Message message = new Message("Lox", "Hui", "Ia hui, a ty lox.");
        Message.writeToDatabase(message);
    }
}