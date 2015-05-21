package com.axolotlinteractive.stackshark.android.reporter.database;

import com.axolotlinteractive.stackshark.android.reporter.ErrorReporter;

import org.anolis.databases.DatabaseObject;
import org.anolis.databases.TableStructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by brycemeyer on 11/2/14.
 */
public class StackObject extends DatabaseObject
{

    public String stack_id;
    public String error_id;
    public String file_name;
    public String class_name;
    public String method_name;
    public String line_number;
    private StackObject(HashMap<String, String> data)
    {
        super(data);
    }

    public static ArrayList<StackObject> createStacks(StackTraceElement[] traceElements, ErrorObject error)
    {
        ArrayList<StackObject> stackObjects = new ArrayList<StackObject>();
        for(StackTraceElement element : traceElements)
        {
            StackObject stack = new StackObject(new HashMap<String, String>());
            stack.file_name = element.getFileName();
            stack.class_name = element.getClassName();
            stack.method_name = element.getMethodName();
            stack.line_number = "" + element.getLineNumber();
            stack.stack_id = "" + ErrorReporter.dbHelper.insert("stack", new String[]{
                    "file_name", "class_name", "method_name", "line_number", "error_id"
            }, new String[]{
                    stack.file_name, stack.class_name, stack.method_name, stack.line_number, error.error_id
            });
            stackObjects.add(stack);
        }
        return stackObjects;
    }

    public static ArrayList<StackObject> fetchStackForError(ErrorObject error)
    {
        ArrayList<StackObject> stackObjects = new ArrayList<StackObject>();
        List<HashMap<String, String>> rawData = ErrorReporter.dbHelper.getTable("stack", "error_id = ?", new String[]{error.error_id});
        for(HashMap<String, String> row : rawData)
        {
            stackObjects.add(new StackObject(row));
        }
        return stackObjects;
    }

    public static TableStructure getStructure()
    {
        return new TableStructure("stack", new String[]{
                "stack_id", "file_name", "class_name",
                "method_name", "line_number", "error_id"
        }, new String[]{
                "INTEGER PRIMARY KEY", "TEXT", "TEXT",
                "TEXT", "INTEGER", "INTEGER"
        });
    }

}
