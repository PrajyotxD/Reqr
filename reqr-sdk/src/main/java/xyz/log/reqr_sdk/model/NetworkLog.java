package xyz.log.reqr_sdk.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "network_logs")
public class NetworkLog {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String method;
    public String url;
    public String host;
    public String path;
    
    public String requestHeaders;
    public String requestBody;
    
    public int responseCode;
    public String responseHeaders;
    public String responseBody;
    
    public long durationMs;
    public long timestamp;
}
