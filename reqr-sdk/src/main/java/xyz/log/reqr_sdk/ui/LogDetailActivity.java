package xyz.log.reqr_sdk.ui;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xyz.log.reqr_sdk.R;
import xyz.log.reqr_sdk.database.NetworkLogDao;
import xyz.log.reqr_sdk.database.NetworkLogDatabase;
import xyz.log.reqr_sdk.model.NetworkLog;

public class LogDetailActivity extends AppCompatActivity {

    private NetworkLogDao logDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reqr_activity_log_detail);

        logDao = NetworkLogDatabase.getDatabase(this).networkLogDao();

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        int logId = getIntent().getIntExtra("LOG_ID", -1);
        if (logId != -1) {
            executor.execute(() -> {
                NetworkLog log = logDao.getLogById(logId);
                if (log != null) {
                    runOnUiThread(() -> bindData(log));
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private void bindData(NetworkLog log) {
        TextView detailMethod = findViewById(R.id.detail_method);
        TextView detailCode = findViewById(R.id.detail_code);
        TextView detailUrl = findViewById(R.id.detail_url);
        TextView detailTime = findViewById(R.id.detail_time);
        
        TextView detailReqHeaders = findViewById(R.id.detail_req_headers);
        TextView detailReqBody = findViewById(R.id.detail_req_body);
        TextView detailResHeaders = findViewById(R.id.detail_res_headers);
        TextView detailResBody = findViewById(R.id.detail_res_body);

        detailMethod.setText(log.method);
        detailCode.setText(String.valueOf(log.responseCode));
        detailUrl.setText(log.url);
        detailTime.setText("⏱ " + log.durationMs + "ms");

        detailReqHeaders.setText(log.requestHeaders == null || log.requestHeaders.isEmpty() ? "{}" : log.requestHeaders);
        detailReqBody.setText(log.requestBody == null || log.requestBody.isEmpty() ? "No body" : log.requestBody);
        
        detailResHeaders.setText(log.responseHeaders == null || log.responseHeaders.isEmpty() ? "{}" : log.responseHeaders);
        detailResBody.setText(log.responseBody == null || log.responseBody.isEmpty() ? "No body" : log.responseBody);
    }
}
