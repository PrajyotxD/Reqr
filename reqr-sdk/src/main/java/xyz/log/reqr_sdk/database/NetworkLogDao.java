package xyz.log.reqr_sdk.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import xyz.log.reqr_sdk.model.NetworkLog;

@Dao
public interface NetworkLogDao {
    @Insert
    void insert(NetworkLog log);

    @Query("SELECT * FROM network_logs ORDER BY timestamp DESC")
    LiveData<List<NetworkLog>> getAllLogs();
    
    @Query("SELECT * FROM network_logs WHERE id = :id LIMIT 1")
    NetworkLog getLogById(int id);

    @Query("DELETE FROM network_logs")
    void deleteAll();
}
