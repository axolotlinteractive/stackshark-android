package com.axolotlinteractive.stackshark.android.reporter.database;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.axolotlinteractive.stackshark.android.reporter.ErrorReporter;

import org.anolis.databases.DatabaseObject;
import org.anolis.databases.TableStructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by brycemeyer on 11/2/14.
 */
public class ErrorObject extends DatabaseObject
{
    public ArrayList<StackObject> stackTrace;
    public String error_id;

    public String type;
    public String message;
    public String offline = "0";
    /**
     * int = whether or not this is synced with the server 0 for no, 1 for yes
     */
    public String synced = "0";
    public String platform_version;
    public String application_version;
    private ErrorObject(HashMap<String, String> data)
    {
        super(data);
    }


    public void setReceived()
    {
        synced = "1";
        ErrorReporter.dbHelper.update("error", "error_id", "" + error_id, "synced", "1");
    }

    public void setFailed()
    {
        //TODO email
        synced = "1";
        ErrorReporter.dbHelper.update("error", "error_id", "" + error_id, "synced", "1");
    }

    public void setNetworkDown()
    {
        offline = "1";
        ErrorReporter.dbHelper.update("error", "error_id", "" + error_id, new String[]{"offline", "synced"}, new String[]{"1", "0"});
    }

    public void loadStack()
    {
        stackTrace = StackObject.fetchStackForError(this);
    }

    public static ErrorObject fetchUnsyncedError()
    {
        List<HashMap<String, String>> rawData = ErrorReporter.dbHelper.getTable("error", "synced = ?", new String[]{"0"});
        if(rawData.size() == 0)
            return null;
        ErrorObject error = new ErrorObject(rawData.get(0));
        error.loadStack();
        return error;
    }

    public static ErrorObject createError(Throwable thrown)
    {
        return ErrorObject.createError(thrown, false);
    }

    public static ErrorObject createError(Throwable thrown, boolean networkError)
    {
        ErrorObject error = new ErrorObject(new HashMap<String, String>());
        error.message = thrown.getMessage();
        error.type = thrown.getClass().getName();
        error.platform_version = "" + Build.VERSION.SDK_INT;
        try {
            PackageInfo pInfo = ErrorReporter.mContext.getPackageManager().getPackageInfo(ErrorReporter.mContext.getPackageName(), 0);
            error.application_version = "" + pInfo.versionCode;
        }
        catch(PackageManager.NameNotFoundException e)
        {
            error.application_version = "0";
            Log.e("SharkStack", "Exception when finding application name", e);
        }
        String[] columns = new String[] {"message", "type", "platform_version", "application_version", "synced"};
        String[] values = new String[]{error.message, error.type, error.platform_version, error.application_version, networkError ? "1" : "0"};
        error.error_id = "" +  ErrorReporter.dbHelper.insert("error", columns, values);
        error.stackTrace = new ArrayList<StackObject>();
        if(thrown.getCause() != null && thrown.getCause().getStackTrace() != null)
            error.stackTrace.addAll(StackObject.createStacks(thrown.getCause().getStackTrace(), error));
        if(thrown.getStackTrace() != null)
            error.stackTrace.addAll(StackObject.createStacks(thrown.getStackTrace(), error));

        return error;
    }

    public static TableStructure getStructure()
    {
        return new TableStructure("error", new String[]{
                "error_id", "message", "offline", "synced", "type", "platform_version", "application_version"
        },new String[]{
                "INTEGER PRIMARY KEY", "TEXT", "INTEGER DEFAULT 0", "INTEGER DEFAULT 1", "TEXT", "INTEGER", "INTEGER"
        });
    }
}
