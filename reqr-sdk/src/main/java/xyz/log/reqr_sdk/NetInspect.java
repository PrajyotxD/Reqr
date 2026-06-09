package xyz.log.reqr_sdk;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import xyz.log.reqr_sdk.ui.FloatingBubbleService;

public class NetInspect {
    
    private static boolean isInitialized = false;

    public static void init(Context context) {
        boolean isDebuggable = (0 != (context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE));
        
        if (!isDebuggable) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            if (!isInitialized) {
                startBubbleService(context);
                isInitialized = true;
            }
        }
    }

    private static void startBubbleService(Context context) {
        Intent intent = new Intent(context, FloatingBubbleService.class);
        context.startService(intent);
    }
}
