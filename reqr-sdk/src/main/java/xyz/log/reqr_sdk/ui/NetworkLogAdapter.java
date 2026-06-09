package xyz.log.reqr_sdk.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import xyz.log.reqr_sdk.R;
import xyz.log.reqr_sdk.model.NetworkLog;

public class NetworkLogAdapter extends RecyclerView.Adapter<NetworkLogAdapter.LogViewHolder> {

    private List<NetworkLog> logs = new ArrayList<>();
    private final Context context;

    public NetworkLogAdapter(Context context) {
        this.context = context;
    }

    public void setLogs(List<NetworkLog> logs) {
        this.logs.clear();
        this.logs.addAll(logs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.reqr_item_network_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        NetworkLog log = logs.get(position);

        holder.tvMethod.setText(log.method);
        holder.tvStatusCode.setText(String.valueOf(log.responseCode));
        holder.tvUrl.setText(log.host + log.path);
        holder.tvTime.setText("⏱ " + log.durationMs + "ms");

        int colorMethod;
        int colorStatus;
        int colorStripe;

        if (log.responseCode >= 200 && log.responseCode < 300) {
            colorStatus = context.getResources().getColor(R.color.success);
            colorStripe = context.getResources().getColor(R.color.success);
        } else if (log.responseCode >= 300 && log.responseCode < 400) {
            colorStatus = context.getResources().getColor(R.color.warn);
            colorStripe = context.getResources().getColor(R.color.warn);
        } else {
            colorStatus = context.getResources().getColor(R.color.danger);
            colorStripe = context.getResources().getColor(R.color.danger);
        }

        if ("GET".equalsIgnoreCase(log.method)) {
            colorMethod = context.getResources().getColor(R.color.success);
        } else if ("POST".equalsIgnoreCase(log.method)) {
            colorMethod = context.getResources().getColor(R.color.secondary_purple);
        } else if ("PUT".equalsIgnoreCase(log.method)) {
            colorMethod = context.getResources().getColor(R.color.warn);
        } else {
            colorMethod = context.getResources().getColor(R.color.danger);
        }

        holder.tvMethod.setTextColor(colorMethod);
        holder.tvStatusCode.setTextColor(colorStatus);
        holder.statusStripe.setBackgroundColor(colorStripe);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, LogDetailActivity.class);
            intent.putExtra("LOG_ID", log.id);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        View statusStripe;
        TextView tvMethod;
        TextView tvStatusCode;
        TextView tvUrl;
        TextView tvTime;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            statusStripe = itemView.findViewById(R.id.status_stripe);
            tvMethod = itemView.findViewById(R.id.tv_method);
            tvStatusCode = itemView.findViewById(R.id.tv_status_code);
            tvUrl = itemView.findViewById(R.id.tv_url);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }
}
