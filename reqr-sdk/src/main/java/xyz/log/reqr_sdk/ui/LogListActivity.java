package xyz.log.reqr_sdk.ui;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xyz.log.reqr_sdk.R;
import xyz.log.reqr_sdk.database.NetworkLogDao;
import xyz.log.reqr_sdk.database.NetworkLogDatabase;

public class LogListActivity extends AppCompatActivity {

    private NetworkLogDao logDao;
    private NetworkLogAdapter adapter;

    private TextView tvReqCount;
    private TextView stat2xx;
    private TextView stat3xx;
    private TextView statErr;
    private TextView statAvg;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reqr_activity_log_list);

        logDao = NetworkLogDatabase.getDatabase(this).networkLogDao();

        tvReqCount = findViewById(R.id.tv_req_count);
        stat2xx = findViewById(R.id.stat_2xx);
        stat3xx = findViewById(R.id.stat_3xx);
        statErr = findViewById(R.id.stat_err);
        statAvg = findViewById(R.id.stat_avg);

        ImageButton btnClear = findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(v -> {
            executor.execute(() -> {
                logDao.deleteAll();
                runOnUiThread(() -> Toast.makeText(LogListActivity.this, "Logs cleared", Toast.LENGTH_SHORT).show());
            });
        });

        RecyclerView recyclerLogs = findViewById(R.id.recycler_logs);
        recyclerLogs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NetworkLogAdapter(this);
        recyclerLogs.setAdapter(adapter);

        logDao.getAllLogs().observe(this, logs -> {
            adapter.setLogs(logs);

            tvReqCount.setText(logs.size() + " requests · session");

            int count2xx = 0;
            int count3xx = 0;
            int countErr = 0;
            long totalMs = 0;

            for (xyz.log.reqr_sdk.model.NetworkLog log : logs) {
                if (log.responseCode >= 200 && log.responseCode < 300) {
                    count2xx++;
                } else if (log.responseCode >= 300 && log.responseCode < 400) {
                    count3xx++;
                } else if (log.responseCode > 0) {
                    countErr++;
                }
                totalMs += log.durationMs;
            }

            long avg = logs.isEmpty() ? 0 : totalMs / logs.size();

            stat2xx.setText(String.valueOf(count2xx));
            stat3xx.setText(String.valueOf(count3xx));
            statErr.setText(String.valueOf(countErr));
            statAvg.setText(String.valueOf(avg));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
