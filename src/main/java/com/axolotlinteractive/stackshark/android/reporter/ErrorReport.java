package com.axolotlinteractive.stackshark.android.reporter;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.axolotlinteractive.stackshark.android.reporter.database.ErrorObject;
import com.axolotlinteractive.stackshark.android.reporter.database.StackObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by brycemeyer on 11/2/14.
 */
public class ErrorReport extends AsyncTask<ErrorObject, Void, Boolean>
{
    public ErrorReport(ErrorObject error)
    {
        super();
        // start compatibility code
        if(Build.VERSION.SDK_INT >= 11)
        {
            executeOnExecutor(THREAD_POOL_EXECUTOR, error);
        }
        else
        {
            execute(error);
        }
        // end compatibility code
    }

    /**
     * creates a http ready piece of data
     *
     * @param projectKey String the key this project is registered to on the server
     * @param error ErrorObject the error that we are building data for
     * @return String the url encoded body for the request
     */
    private String buildPutData(String projectKey, ErrorObject error) {

        String data = "";

        data+= "offline=" + error.offline;
        try {
            data += "&message=" + URLEncoder.encode(error.message, "UTF-8");
        }
        catch(UnsupportedEncodingException e) {
            Log.e("error report", e.getMessage(), e);
        }
        data+= "&application_version=" + error.application_version;
        data+= "&platform_version=" + error.platform_version;
        data+= "&type=" + error.type;
        data+= "&project_key=" + projectKey;

        JSONArray stack = new JSONArray();
        for(StackObject trace : error.stackTrace)
        {
            try {
                JSONObject stackObject = new JSONObject();
                stackObject.put("file", trace.file_name);
                stackObject.put("class", trace.class_name);
                stackObject.put("function", trace.method_name);
                stackObject.put("line", trace.line_number);
                stack.put(stackObject);
            }
            catch(JSONException e) {
                //do nothing
            }
        }

        data+= "&stack=" + stack.toString();

        return data;
    }

    @Override
    protected Boolean doInBackground(ErrorObject[] params)
    {
        if(params.length < 1)
            return false;
        ErrorObject error = params[0];
        try
        {
            URL url = new URL("http://stackshark.com/api/1/errors/");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            try {
                String urlParameters = this.buildPutData(ErrorReporter.ProjectKey, error);

                connection.setDoOutput(true);
                connection.setRequestMethod("PUT");
                connection.setFixedLengthStreamingMode(urlParameters.length());

                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                connection.connect();

                if (connection.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    String response = "";

                    while (in.ready()) {
                        response+= in.readLine();
                    }
                    in.close();

                    JSONObject responseData = new JSONObject(response);


                    if(responseData.getInt("status") == 3101)
                        error.setReceived();
                    else
                        error.setFailed();

                    return true;
                }
                else {
                    error.setNetworkDown();
                    return false;
                }
            }
            finally {
                connection.disconnect();
            }

        }
        catch(Exception e)
        {
            Log.e("stackShark", e.getMessage(), e);
            if(error != null)
                error.setNetworkDown();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean runNext)
    {
        if(runNext)
        {
            ErrorObject cachedError = ErrorObject.fetchUnsyncedError();
            if(cachedError != null)
                new ErrorReport(cachedError);
        }
    }
}
