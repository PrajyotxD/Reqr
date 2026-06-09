package xyz.log.reqr_sdk.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import xyz.log.reqr_sdk.model.NetworkLog;

@Database(entities = {NetworkLog.class}, version = 1, exportSchema = false)
public abstract class NetworkLogDatabase extends RoomDatabase {
    private static volatile NetworkLogDatabase INSTANCE;

    public abstract NetworkLogDao networkLogDao();

    public static NetworkLogDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (NetworkLogDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            NetworkLogDatabase.class, "reqr_network_log_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
