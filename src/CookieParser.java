import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by Yoni on 3/1/2015.
 */
public class CookieParser {
    Hashtable<String, String> table;
    String rawCookies;

    public CookieParser(String cookies) {
        this.rawCookies = cookies;
        table = new Hashtable();
        StringTokenizer tokenizer = new StringTokenizer(cookies, ";");
        while (tokenizer.hasMoreElements()) {
            String[] currentCookie = tokenizer.nextToken().split("=");
            String key = currentCookie[0].trim();
            String value = currentCookie[1].trim();
            table.put(key, value);
        }
    }

    public CookieParser(Hashtable<String, String> table) {
        this.table = table;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : table.entrySet()){
            String cookie = String.format("%1$s=%2$s", entry.getKey(), entry.getValue());
            sb.append(cookie + "; ");
        }
        // Remove the trailing "; " characters.
        sb.trimToSize();
        sb.deleteCharAt(sb.length() - 1);
        sb.deleteCharAt(sb.length() - 1);
        this.rawCookies = sb.toString();
    }

    @Override
    public String toString() {
        return rawCookies;
    }
}
