package gov.census.cspro.androidofflinemaps;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;
import com.myroutes.mbtiles4j.MBTilesReadException;
import com.myroutes.mbtiles4j.MBTilesReader;
import com.myroutes.mbtiles4j.model.MetadataBounds;
import com.myroutes.mbtiles4j.model.MetadataEntry;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class GoogleMapFragment extends SupportMapFragment
    implements MapFragment, OnMapAndViewReadyListener.OnGlobalLayoutAndMapReadyListener {

    private static final String TAG = SupportMapFragment.class.getSimpleName();

    private static final String ARG_INITIAL_TILE_SOURCE = "ARG_INITIAL_TILE_SOURCE";
    private GoogleMap m_map;
    private List<Marker> m_markers;
    private MapFragment.OnFragmentInteractionListener m_listener;
    private String m_tileSource;
    public String m_baseUrl;
    private TileOverlay m_tileOverlay;

    static public GoogleMapFragment newInstance(String initialTileSource)
    {
        GoogleMapFragment fragment = new GoogleMapFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_INITIAL_TILE_SOURCE, initialTileSource);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Fragment getFragment()
    {
        return this;
    }

    @Override
    public void setTileSource(String tileSource) {
        if (m_tileOverlay != null) {
            m_tileOverlay.remove();
            m_tileOverlay = null;
        }

        try {
            MBTilesReader reader = new MBTilesReader(new File(tileSource));
            MetadataEntry metadata = reader.getMetadata();
            m_map.setMaxZoomPreference(reader.getMaxZoom());
            m_map.setMinZoomPreference(reader.getMinZoom());

            MetadataBounds tileSetBounds = metadata.getTilesetBounds();
            LatLngBounds mapBounds = new LatLngBounds(
                new LatLng(tileSetBounds.getBottom(), tileSetBounds.getLeft()),
                new LatLng(tileSetBounds.getTop(), tileSetBounds.getRight()));
            m_map.setLatLngBoundsForCameraTarget(mapBounds);

            String filename = FilenameUtils.getName(tileSource);

            m_baseUrl = "http://localhost:8084/maps/" + filename + "/%d/%d/%d.png";

            TileProvider tileProvider = new UrlTileProvider(256, 256) {
                @Override
                public synchronized URL getTileUrl(int x, int y, int zoom)
                {
                    String s = String.format(Locale.US, m_baseUrl, zoom, x, y);
                    try {
                        return new URL(s);
                    } catch (MalformedURLException e) {
                        Log.e(TAG, "Bad base URL: " + m_baseUrl, e);
                        return null;
                    }
                }
            };

            m_tileOverlay = m_map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
        } catch (MBTilesReadException e) {
            Log.e(TAG, "Failed to read tiles file: " + tileSource, e);
        }

    }

    @Override
    public void showPlace(Place c)
    {
        for (Marker m : m_markers) {
            if (m.getTag() != null && m.getTag().equals(c)) {
                m.showInfoWindow();
                m_map.animateCamera(CameraUpdateFactory.newLatLng(m.getPosition()), 250, null);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            m_tileSource = getArguments().getString(ARG_INITIAL_TILE_SOURCE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        new OnMapAndViewReadyListener(this, this);
       // getMapAsync(this);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        m_map = googleMap;
        m_map.setMyLocationEnabled(true);
        setTileSource(m_tileSource);

        UiSettings uiSettings = m_map.getUiSettings();
        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setZoomControlsEnabled(true);

        final PlacesViewModel viewModel = ViewModelProviders.of(this).get(PlacesViewModel.class);
        List<Place> places = viewModel.getPlaces().getValue();
        if (places != null) {
            m_markers = new ArrayList<>();
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Place c : places) {
                LatLng latLng = new LatLng(c.latitude, c.longitude);
                builder.include(latLng);
                Marker marker = googleMap.addMarker(new MarkerOptions().position(latLng)
                    .title(c.label));
                marker.setTag(c);
                m_markers.add(marker);
            }
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
        }

        m_map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Place c = (Place) marker.getTag();
                if (m_listener != null && marker.getTag() instanceof Place) {
                    m_listener.onMapPlaceClicked(c);
                }
                return false;
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            m_listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        m_listener = null;
    }

}
