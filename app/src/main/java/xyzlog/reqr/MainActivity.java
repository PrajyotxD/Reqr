package xyzlog.reqr;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.log.reqr_sdk.NetInspect;
import xyz.log.reqr_sdk.interceptor.NetInspectInterceptor;

public class MainActivity extends AppCompatActivity {

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 1. Initialize the Reqr SDK
        NetInspect.init(this);

        // 2. Add NetInspectInterceptor to OkHttpClient
        //    followRedirects(false) so 3xx responses are captured as-is rather than
        //    silently resolved before the interceptor sees them.
        client = new OkHttpClient.Builder()
                .followRedirects(false)
                .addInterceptor(new NetInspectInterceptor(this))
                .build();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 200 — successful GET
        Button btnSuccess = findViewById(R.id.btn_test_success);
        btnSuccess.setOnClickListener(v -> makeGetRequest(
                "https://jsonplaceholder.typicode.com/users/1"));

        // 301 — permanent redirect (captured because followRedirects is disabled)
        Button btnWarning = findViewById(R.id.btn_test_warning);
        btnWarning.setOnClickListener(v -> makeGetRequest(
                "https://httpbingo.org/status/301"));

        // 500 — internal server error
        Button btnError = findViewById(R.id.btn_test_error);
        btnError.setOnClickListener(v -> makeGetRequest(
                "https://httpbingo.org/status/500"));

        // Network exception — invalid host triggers an IOException that the
        // interceptor records with responseCode 0
        Button btnException = findViewById(R.id.btn_test_exception);
        btnException.setOnClickListener(v -> makeGetRequest(
                "https://this.domain.does.not.exist.invalid/test"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check every time we resume so the overlay starts immediately after
        // the user grants the "Display over other apps" permission.
        NetInspect.init(this);
    }

    /** Fire an async GET and let the interceptor handle logging. */
    private void makeGetRequest(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Failure is already captured by NetInspectInterceptor (responseCode = 0)
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Body must be consumed/closed to avoid a resource leak
                if (response.body() != null) {
                    response.body().close();
                }
            }
        });
    }
}
