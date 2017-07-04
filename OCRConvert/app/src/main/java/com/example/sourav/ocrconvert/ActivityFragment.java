package com.example.sourav.ocrconvert;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.todddavies.components.progressbar.ProgressWheel;

import net.bgreco.DirectoryPicker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * Created by sourav on 7/4/17.
 */

public class ActivityFragment extends Fragment implements View.OnClickListener{

    static final int myPermissionCode = 10;
    private static final String STATE_TASK_RUNNING = "taskRunning";
    String outputFileURL;
    Button browse = null;
    Button openFolder = null;
    ProgressWheel pw = null;
    private Spinner langSpinner, outputFormat;
    private String lang = "";
    private String docType = "";
    OCRWebService webService = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_convert_ocr, container, false);
        browse = (Button) view.findViewById(R.id.browse);
        browse.setOnClickListener(this);

        openFolder = (Button) view.findViewById(R.id.openFolder);
        openFolder.setOnClickListener(this);
        openFolder.setEnabled(false);
        openFolder.setVisibility(View.INVISIBLE);

        // Create spinner or dropdown for languages

        addLanguages(view);
        addOutputFormat(view);
        pw = (ProgressWheel) view.findViewById(R.id.pw_spinner);
        pw.setVisibility(View.GONE);
//        pw.setText("Converting");

        // Set the activity object for Web Service to handle screen orientaion
//        OCRWebService.setActivityObject(this);

        // Check if the task was running so we can restart the spinning wheel
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(STATE_TASK_RUNNING, false)) {
                pw.setVisibility(View.VISIBLE);
                pw.startSpinning();
            }
        }
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.browse:
                // Set Language and output format
                lang = (String) langSpinner.getSelectedItem();
                docType = (String) outputFormat.getSelectedItem();
                Intent intent = new Intent()
                        .setType("*/*")
//                        .setType("image/*|application/pdf")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, "Select a file"), 123);
                break;
            case R.id.openFolder:

                Intent intentOpen = new Intent(getActivity(), DirectoryPicker.class);
                intentOpen.putExtra(DirectoryPicker.START_DIR, Environment.getExternalStorageDirectory());
//                intentOpen.putExtra(DirectoryPicker.ONLY_DIRS,false);
                // optionally set options here
                startActivityForResult(intentOpen, 100);
                break;
            default:
                break;
        }
    }

    public void addLanguages(View view){
        langSpinner = (Spinner) view.findViewById(R.id.langSpinner);
        List<String> list = new ArrayList<String>();
        list.add("english");
        list.add("german");
        list.add("french");
        list.add("italian");
        list.add("japanese");
        list.add("korean");
        list.add("latin");
        list.add("polish");
        list.add("portuguese");
        list.add("russian");
        list.add("serbian");
        list.add("spanish");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        langSpinner.setAdapter(dataAdapter);
    }

    public void addOutputFormat(View view){
        outputFormat = (Spinner) view.findViewById(R.id.outputFormat);
        List<String> list = new ArrayList<String>();
        list.add("txt");
        list.add("pdf");
        list.add("doc");
        list.add("xls");
        list.add("docx");
        list.add("pdfimg");
        list.add("xlsx");


        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        outputFormat.setAdapter(dataAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && resultCode == RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file
            System.out.println("URI "+selectedfile);

            //
//        if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
//            Toast.makeText(this, "External SD card not mounted", Toast.LENGTH_LONG).show();
//        }

            // Assume thisActivity is the current activity
            int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            if(permissionCheck != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        myPermissionCode);
            }

            String path = "";

            webService = new OCRWebService();
            try {
//            pw.setEnabled(true);
                pw.setVisibility(View.VISIBLE);
                pw.startSpinning();
                //
                // path = getPath(this, selectedfile);//
                path = getPathFile(getActivity(), selectedfile);

                //
                // String filePath = selectedfile.getPath();
                // webService.mainOCR(filePath);

                webService.mainOCR(path, this, lang, docType);
//            outputFileURL = webService.mainOCR(path, this);
//            Toast.makeText(this, "File converted", Toast.LENGTH_SHORT).show();
            }

            catch(Exception e){
                e.printStackTrace();
            }

        }

        else if(requestCode == 100 && resultCode == RESULT_OK) {
            // Disable Open Folder button again
            openFolder.setEnabled(false);
            openFolder.setVisibility(View.INVISIBLE);
            pw.setVisibility(View.VISIBLE);
            pw.startSpinning();
            Bundle extras = data.getExtras();
            String path = (String) extras.get(DirectoryPicker.CHOSEN_DIRECTORY);
            System.out.println(path);
            // do stuff with path


            if (outputFileURL != null && !outputFileURL.equals(""))
            {

                try {
                    // Download output file
                    //DownloadConvertedFile(outputFileURL, path);
                    new DownloadFile().execute(outputFileURL, path);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }


    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case myPermissionCode: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    // Get the dir of SD Card
                    File sdCardDir = Environment.getExternalStorageDirectory();

                    // Get The Text file
                    File txtFile = new File(sdCardDir.getAbsolutePath(), "/Download/fin.txt");

                    // Read the file Contents in a StringBuilder Object
                    StringBuilder text = new StringBuilder();

                    try {

                        BufferedReader reader = new BufferedReader(new FileReader(txtFile));

                        String line;

                        while ((line = reader.readLine()) != null) {
                            text.append(line + '\n');
                        }
                        reader.close();
                    } catch (Exception e) {

                        e.printStackTrace();
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void DownloadConvertedFile(String outputFileUrl, String Path) throws IOException
    {
        URL downloadUrl = new URL(outputFileUrl);
        HttpURLConnection downloadConnection = (HttpURLConnection)downloadUrl.openConnection();

        if (downloadConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {

            InputStream inputStream = downloadConnection.getInputStream();



            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream(Path+"/output.txt");

            int bytesRead = -1;
            byte[] buffer = new byte[4096];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
        }

        downloadConnection.disconnect();

    }

    private class DownloadFile extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {

            String outputFileUrl = strings[0];
            String Path = strings[1];
            try {
                URL downloadUrl = new URL(outputFileUrl);
                HttpURLConnection downloadConnection = (HttpURLConnection)downloadUrl.openConnection();

                if (downloadConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                    InputStream inputStream = downloadConnection.getInputStream();



                    // opens an output stream to save into file
                    FileOutputStream outputStream = new FileOutputStream(Path+"/output."+docType);

                    int bytesRead = -1;
                    byte[] buffer = new byte[4096];
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    outputStream.close();
                    inputStream.close();
                }

                downloadConnection.disconnect();
                return true;

            }
            catch (Exception e){
                e.printStackTrace();
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean b){
            pw.setVisibility(View.INVISIBLE);
            pw.stopSpinning();
            Toast.makeText(getActivity().getApplicationContext(), "output.txt File downloaded", Toast.LENGTH_SHORT).show();
        }
    }




    public static String getPathFile(Context context, Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else
            if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    // Static method to get Download URL from on Post Execute in Aysn task

    public void getDownloadLink(String downloadLink){
        openFolder.setEnabled(true);
//        pw.setEnabled(false);
        pw.setVisibility(View.INVISIBLE);
        pw.stopSpinning();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the task is running, save it in our state
        if (isTaskRunning()) {
            outState.putBoolean(STATE_TASK_RUNNING, true);
        }
    }

    private boolean isTaskRunning() {
        return (webService != null && webService.OCRConvertTask != null)
                && (webService.OCRConvertTask.getStatus() == AsyncTask.Status.RUNNING);
    }
}
