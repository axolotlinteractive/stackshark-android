package com.axolotlinteractive.android.database;

/**
 * Created by brycemeyer on 11/2/14.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is designed to help with the implementation of
 * databases inside of an android application
 */
public class DatabaseHelper{

    private SQLiteDatabase mDatabase;

    public DatabaseHelper(Context ctx, String databaseName, int databaseVersion, List<TableStructure> tables)
    {
        Helper helper=new Helper(ctx, databaseName, databaseVersion, tables);
        mDatabase = helper.getWritableDatabase();
    }
    /**
     * This method returns an entire table
     *
     * @param table= the table you are querying
     *
     * @return the return value is the entire table in an ArrayList
     * <br>
     * 	every item in the List is a different row
     * 	<br>
     * 	each row is returned inside a HashMap
     */
    public List<HashMap<String,String>> getTable(String table){
        return getTable(table,null,null);
    }
    public List<HashMap<String,String>> getTableWhere(String table,String whereColumn,String whereArg){
        return getTableWhere(table,new String[]{whereColumn},new String[]{whereArg});
    }
    public List<HashMap<String,String>> getTableWhere(String table,String[] whereColumns,String[] whereArgs){
        String where="";
        for(String column:whereColumns){
            where+=column+"=? ";
        }
        return getTable(table,where,whereColumns);
    }
    public	List<HashMap<String,String>> getTable(String table,String where,String[] whereArgs){
        Cursor c=mDatabase.query(table, null,where,whereArgs, null,null, null);

        return toList(c);
    }

    public List<HashMap<String,String>> getTableRaw(String query){
        Cursor c = mDatabase.rawQuery(query, null);

        return toList(c);
    }

    public String getField(String table,String field,String whereColumn,String whereValue){
        Cursor c=mDatabase.query(table, new String[]{field}, whereColumn+"=?", new String[]{whereValue}, null, null, null);
        c.moveToFirst();
        if(c.isAfterLast())return null;
        String returnVal = c.getString(0);
        c.close();
        return returnVal;
    }



    /**
     * Calls an update that updates based on the local id
     * @param table the table we want to update
     * @param whereValue the local id of the row in the table that we want to update
     * @param column the column that we want to update
     * @param value the value we want to update the column with
     */
    public int update(String table,String whereValue,String column,String value){
        return update(table,whereValue,new String[]{column},new String[]{value});
    }

    public int update(String table,String whereValue,String[] column,String[] value){
        return update(table,"id",whereValue,column,value);
    }
    public int update(String table,String whereField,String whereValue,String column,String value)throws SQLException{
        return update(table,whereField,whereValue,new String[]{column},new String[]{value});
    }

    /**
     * build and constructs the update values and executes the update
     * @param table the table we want to update
     * @param whereField the field that will be
     * @param whereValue the value of the row in the table that we want to update
     * @param columns the columns that we want to update
     * @param values the values asociated with the columns
     * @throws SQLException if the values and columns are not the same length then this exception will be thrown
     * @return number of rows effected
     */
    public int update(String table, String whereField, String whereValue, String[] columns, String[] values) throws SQLException {
        if(columns.length!=values.length){
            throw new SQLException("columns and values must be the same amount when update a field");
        }
        ContentValues updateVal=new ContentValues();
        for(int i=0;i<values.length;i++){
            updateVal.put(columns[i],values[i]);
        }
        return mDatabase.update(table, updateVal, whereField+"= ?", new String[]{whereValue});
    }

    public long insert(String table,String column,String value){
        return insert(table,new String[]{column},new String[]{value});
    }
    /**
     * this method builds and executes insert statements
     * @param table = the table you are trying to insert into
     *
     * @param columns = the columns that you have values to insert into
     *
     * @param values = the values for the columns
     */
    public long insert(String table, String[] columns, String[] values){
        if(columns.length!=values.length){
            throw new SQLiteException("values and columns must be the same size");
        }
        ContentValues data=new ContentValues();
        for(int i=0;i<columns.length;i++){
            data.put(columns[i], values[i]);
        }
        return mDatabase.insert(table, null, data);
    }

    public HashMap<String,String> getLastEntry(String table){
        Cursor c=mDatabase.query( table, null, null, null, null, null, "id DESC", "1");

        while(c.moveToNext()){
            HashMap<String,String> data=new HashMap<String,String>();
            for(int i=0;i<c.getColumnCount();i++){
                data.put(c.getColumnName(i), c.getString(i));
            }
            c.close();
            return data;
        }
        c.close();
        return null;
    }


    public HashMap<String,String> getRow(String table, String id){
        return getRow(table, "id", id);
    }

    public HashMap<String,String> getRow(String table, String name, String value){
        return getRow(table, new String[]{name}, new String[]{value});
    }
    public HashMap<String,String> getRow(String table, String[] whereColumns, String[] whereValues){
        Cursor c = mDatabase.query(table, null, getParameterizedWhere(whereColumns), whereValues,null, null, null);

        List<HashMap<String, String>> rows = toList(c);
        return rows.size() > 0 ? rows.get(0) : null;
    }
    /**
     * turns an array into a string with parameters
     * @param columns
     * @return
     */
    private String getParameterizedWhere(String[] columns){
        String column="`";
        for(int i=0;i<columns.length;i++){
            column+=columns[i];
            column+="` = ?";
            if(i!=columns.length-1)column+=" AND `";
        }
        return column;
    }
    public List<HashMap<String,String>> toList(Cursor c){
        List<HashMap<String,String>> r=new ArrayList<HashMap<String,String>>();
        if(c.getCount()==0)return r;
        c.moveToFirst();
        do
        {
            HashMap<String,String> map=new HashMap<String,String>();
            String[] columns=c.getColumnNames();
            for(int i=0;i<columns.length;i++){
                map.put(columns[i], c.getString(i));
            }
            r.add(map);
        }
        while(c.moveToNext());
        c.close();
        return r;
    }

    public boolean delete(String table, String column, String value){
        return mDatabase.delete(table, column+"=?", new String[]{value}) > 0;
    }

    private static final class Helper extends SQLiteOpenHelper
    {

        /**
         * List of all tables inside of this database
         */
        private final List<TableStructure> Tables;
        public Helper(Context ctx, String name,int version,List<TableStructure> tables)
        {
            super(ctx, name, null, version);
            Tables = tables;
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            for(TableStructure table: Tables)
            {
                table.createTable(db);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            for(TableStructure table : Tables)
            {
                table.updateTable(db, oldVersion);
            }
        }
    }
}

