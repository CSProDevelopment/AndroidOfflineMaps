package gov.census.cspro.androidofflinemaps;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class MainActivity extends AppCompatActivity implements LeafletMapFragment.OnFragmentInteractionListener,
    PlaceListFragment.OnListFragmentInteractionListener, StringListDialogFragment.Listener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST = 1;
    private static final int TILE_SOURCE_REQUEST = 1;
    private static final int MAP_SDK_REQUEST = 2;
    private static final String STATE_MAP_SDK = "STATE_MAP_SDK";
    private static final String STATE_MAP_PATH = "STATE_MAP_PATH";

    private MapFragment m_mapFragment;
    private PlaceListFragment m_placeListFragment;
    private String m_mapsPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/offlinemaps";
    private MapServer m_mapServer;
    private String m_mapSDK;
    private String m_currentMapPath;
    private boolean m_needPermissions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            m_mapSDK = savedInstanceState.getString(STATE_MAP_SDK);
            m_currentMapPath = savedInstanceState.getString(STATE_MAP_PATH);
        } else {
            m_mapSDK = MapFragmentFactory.getAvailableTypes()[0];
        }

        m_placeListFragment = (PlaceListFragment) getSupportFragmentManager().findFragmentById(R.id.place_list);
        startMapServer();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
            onActivityReadyForMapping();
        } else {
            m_needPermissions = true;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putString(STATE_MAP_SDK, m_mapSDK);
        outState.putString(STATE_MAP_PATH, m_currentMapPath);

        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }

    private void startMapServer()
    {
        try {
            if (m_mapServer == null) {
                String serverRootPath = m_mapsPath;
                m_mapServer = new MapServer(serverRootPath,"localhost", 8084, this);
            }
            m_mapServer.start();

        } catch (IOException e) {
            Log.e(TAG, "Failed to start map server", e);
        }
    }

    @Override
    protected void onDestroy()
    {
        if (m_mapServer != null) {
            m_mapServer.stop();
            m_mapServer = null;
        }

        super.onDestroy();
    }

    @Override
    public void onResume() {

        super.onResume();
        if (m_needPermissions) {
            requestPermissions();
        }
    }

    private String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
    };

    private void requestPermissions() {
        // Permission is not granted
        // Should we show an explanation?
        boolean shouldShowRationale = false;
        for (String permission : PERMISSIONS) {
            shouldShowRationale |= ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
        }

        if (shouldShowRationale) {
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            new AlertDialog.Builder(this)
                .setTitle("Permission")
                .setMessage("This won't work if you don't grant permissions")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Prompt the user once explanation has been shown
                        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS,
                            PERMISSIONS_REQUEST);
                    }
                })
                .create()
                .show();
        } else {
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,PERMISSIONS,
                PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    m_needPermissions = false;
                    onActivityReadyForMapping();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void onActivityReadyForMapping()
    {
        copyMapsToSDCard();
        if (m_currentMapPath == null)
            m_currentMapPath = m_mapsPath + "/" + findFirstMap();
        setMapSDK(m_mapSDK);
    }

    private void setMapSDK(String sdk)
    {
        m_mapSDK = sdk;
        m_mapFragment = MapFragmentFactory.newInstance(sdk, m_currentMapPath);
        getSupportFragmentManager().beginTransaction().replace(R.id.map, m_mapFragment.getFragment()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.options_tile_source:
                StringListDialogFragment.newInstance(findMaps().toArray(new String[] {}), TILE_SOURCE_REQUEST).show(getSupportFragmentManager(), "dialog");
                return true;
            case R.id.options_sdk:
                StringListDialogFragment.newInstance(MapFragmentFactory.getAvailableTypes(), MAP_SDK_REQUEST).show(getSupportFragmentManager(), "dialog");
                return true;
        }
        return false;
    }
    @Override
    public void onMapPlaceClicked(Place c) {
        m_placeListFragment.showPlace(c);
    }

    @Override
    public void onListPlaceSelected(Place c) {
        m_mapFragment.showPlace(c);
    }

    private void copyMapsToSDCard() {

        final String assetsMapDir = "maps";
        try {
            AssetManager assetManager = getAssets();
            String assets[] = assetManager.list(assetsMapDir);
            File mapsDir = new File(m_mapsPath);
            //noinspection ResultOfMethodCallIgnored
            mapsDir.mkdir();

            if (assets != null) {
                for (String asset : assets) {
                    File outputFile = new File(mapsDir, asset);
                    if (!outputFile.exists()) {
                        InputStream in = null;
                        OutputStream out = null;
                        try {
                            in = assetManager.open(assetsMapDir + "/" + asset);
                            out = new FileOutputStream(outputFile);
                            IOUtils.copy(in, out);
                        } catch (IOException ex) {
                            Log.e("tag", "I/O Exception copying file " + asset, ex);
                        } finally {
                            if (in != null)
                                in.close();
                            if (out != null)
                                out.close();
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Log.e("tag", "I/O Exception", ex);
        }
    }

    @Override
    public void onStringChosen(int requestCode, String s)
    {
        switch (requestCode) {
            case TILE_SOURCE_REQUEST:
                m_currentMapPath = m_mapsPath + "/" + s;
                m_mapFragment.setTileSource(m_currentMapPath);
                break;
            case MAP_SDK_REQUEST:
                setMapSDK(s);
                break;
        }
    }

    private List<String> findMaps()
    {
        ArrayList<String> maps = new ArrayList<>();
        File mapsDir = new File(m_mapsPath);
        for (File f : mapsDir.listFiles()) {
            if (f.isFile() && FilenameUtils.getExtension(f.getName()).equals("mbtiles"))
                maps.add(f.getName());
        }

        return maps;
    }

    private String findFirstMap()
    {
        List<String> maps = findMaps();
        return maps.size() > 0 ? maps.get(0) : null;
    }
}
