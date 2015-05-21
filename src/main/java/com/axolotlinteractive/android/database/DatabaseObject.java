package com.axolotlinteractive.android.database;


import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by brycemeyer on 11/2/14.
 */
public abstract class DatabaseObject
{
    public DatabaseObject(HashMap<String, String> data)
    {
        Class<?> objectType = this.getClass();

        for(Map.Entry<String, String> entry : data.entrySet())
        {

            try
            {
                Field f = objectType.getField(entry.getKey());
                f.setAccessible(true);
                f.set(this, entry.getValue());
            }
            catch(NoSuchFieldException e)
            {
                throw new NoSuchFieldError("Please declare the field " + entry.getKey() + " in your database object " + objectType);
            }
            catch(IllegalAccessException e)
            {
                throw new IllegalAccessError("Unable to access the field " + entry.getKey() + " in your database object " + objectType);
            }
        }
    }
}
