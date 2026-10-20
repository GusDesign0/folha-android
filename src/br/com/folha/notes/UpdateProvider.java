package br.com.folha.notes;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** One read-only APK URI, granted temporarily to Android's installer. */
public final class UpdateProvider extends ContentProvider {
    static final Uri URI=Uri.parse("content://br.com.folha.notes.updates/Folha.apk");
    static final String TYPE="application/vnd.android.package-archive";
    static File apk(Context c){return new File(c.getFilesDir(),"updates/Folha.apk");}
    static void requireUri(Uri uri){if(!URI.equals(uri))throw new IllegalArgumentException("Arquivo indisponível");}
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){requireUri(uri);return TYPE;}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {requireUri(uri);if(!"r".equals(mode))throw new SecurityException("Somente leitura");return ParcelFileDescriptor.open(apk(getContext()),ParcelFileDescriptor.MODE_READ_ONLY);}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){requireUri(uri);String[] columns=projection!=null?projection:new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE};MatrixCursor c=new MatrixCursor(columns);Object[] row=new Object[columns.length];for(int i=0;i<columns.length;i++){if(OpenableColumns.DISPLAY_NAME.equals(columns[i]))row[i]="Folha.apk";if(OpenableColumns.SIZE.equals(columns[i]))row[i]=apk(getContext()).length();}c.addRow(row);return c;}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
