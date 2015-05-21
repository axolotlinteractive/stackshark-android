package com.axolotlinteractive.android.database;

import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by brycemeyer on 11/2/14.
 */
public class TableStructure
{

    /**
     * The name of this database table
     */
    private String Name;

    /**
     * The array of columns that this table will have once created
     */
    private String[] Columns;

    /**
     * The array of structures that each column will have once created
     * <br/>
     * Note that these need to be in the same order as Columns
     */
    private String[] Structures;

    /**
     * @param name the name of the Database Table
     * @param columns the name of the columns in the database
     * @param structures the structure of the columns in the database
     */
    public TableStructure(String name, String[] columns, String[] structures)
    {
        Name=name;
        if(Name == null)
        {
            throw new NullPointerException("Error while creating Database Table you must specify the name of the table");
        }
        Columns=columns;
        Structures=structures;
        if(Columns.length != Structures.length)
        {
            throw new ArrayIndexOutOfBoundsException("Error while creating Database Table "+Name+" you must pass the same amount of structures as you are passing for columns");
        }
    }

    /**
     * creates the table in the database
     * @param db the database that we are going to create the table in
     */
    public void createTable(SQLiteDatabase db)
    {
        String sql="CREATE TABLE `"+Name+"`(`";
        for(int i=0;i<Columns.length;i++)
        {
            sql+=Columns[i];
            sql+="` ";
            sql+=Structures[i];
            if(i!=Columns.length-1)sql+=", `";
        }
        sql+=");";
        db.execSQL(sql);
    }

    public void updateTable(SQLiteDatabase database, int previousVersion)
    {
        HashMap<String, String> newColumns = getNewColumns(previousVersion);
        if(newColumns != null)
        {
            for(Map.Entry<String, String> entry : newColumns.entrySet())
            {
                String alter;
                alter = "ALTER TABLE `";
                alter+= Name;
                alter+= "` ADD COLUMN `";
                alter+= entry.getKey();
                alter+= "` ";
                alter+= entry.getValue();
                database.execSQL(alter);
            }
        }
    }

    /**
     * gets any new columns that were added in a update override this if there are new columns in a table
     * @param previousVersion the version we are updating from
     * @return returns a list of new columns with the key being the name of the column, and value being the structure of the column
     */
    protected HashMap<String, String> getNewColumns(int previousVersion)
    {
        return null;
    }
}