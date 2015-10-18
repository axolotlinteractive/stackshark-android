package com.axolotlinteractive.stackshark.android.reporter;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.axolotlinteractive.stackshark.android.reporter.database.ErrorObject;
import com.axolotlinteractive.stackshark.android.reporter.database.StackObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

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
        data+= "&message=" + error.message;
        data+= "&application_version=" + error.application_version;
        data+= "&platform_version=" + error.platform_version;
        data+= "%type=" + error.type;
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
        String url = "http://stackshark.com/api/1/errors/";
        if(params.length < 1)
            return false;
        ErrorObject error = params[0];
        try
        {
            HttpPut put = new HttpPut(url);
            ArrayList<NameValuePair> data = new ArrayList<NameValuePair>();

            data.add(new BasicNameValuePair("offline", error.offline));
            data.add(new BasicNameValuePair("message", error.message));
            data.add(new BasicNameValuePair("application_version", error.application_version));
            data.add(new BasicNameValuePair("platform_version", error.platform_version));
            data.add(new BasicNameValuePair("type", error.type));
            data.add(new BasicNameValuePair("project_key", ErrorReporter.ProjectKey));

            JSONArray stack = new JSONArray();
            for(StackObject trace : error.stackTrace)
            {
                JSONObject stackObject = new JSONObject();
                stackObject.put("file", trace.file_name);
                stackObject.put("class", trace.class_name);
                stackObject.put("function", trace.method_name);
                stackObject.put("line", trace.line_number);
                stack.put(stackObject);
            }
            data.add(new BasicNameValuePair("stack", stack.toString()));

            put.setEntity(new UrlEncodedFormEntity(data));

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpConnectionParams.setSoTimeout(httpClient.getParams(), 20000);
            HttpConnectionParams.setConnectionTimeout(httpClient.getParams(),20000);

            HttpResponse response = httpClient.execute(put);
            HttpEntity ent = response.getEntity();
            String rawResponse = EntityUtils.toString(ent);

            Log.d("stackShark", "rawResponse = " + rawResponse);

            JSONObject responseObject = new JSONObject(rawResponse);
            if(responseObject.getInt("status") == 3101)
                error.setReceived();
            else
                error.setFailed();
            return true;

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
