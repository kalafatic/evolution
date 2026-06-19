import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class TestLogin {
    public static void main(String[] args) throws Exception {
        URL url = new URL("http://localhost:48080/api/auth/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonInputString = "{\"username\": \"admin\", \"password\": \"admin\"}";

        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        System.out.println("Response Code: " + code);

        InputStream is;
        if (code >= 200 && code < 300) {
            is = conn.getInputStream();
        } else {
            is = conn.getErrorStream();
        }

        if (is != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
                String response = br.lines().collect(Collectors.joining("\n"));
                System.out.println("Response Body: " + response);
            }
        }
    }
}
