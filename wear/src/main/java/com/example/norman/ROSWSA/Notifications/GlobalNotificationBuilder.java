package com.example.norman.ROSWSA.Notifications;

import android.support.v4.app.NotificationCompat;
/**
 * Created by norman on 25/09/17.
 */

public class GlobalNotificationBuilder {
    private static NotificationCompat.Builder mNotificationBuilder = null;

    private GlobalNotificationBuilder(){}

    public static NotificationCompat.Builder getNotificationBuilder(){
        return mNotificationBuilder;
    }

    public static NotificationCompat.Builder setNotificationBuilder(NotificationCompat.Builder notificationBuilder){
        mNotificationBuilder = notificationBuilder;
        return mNotificationBuilder;
    }


}
