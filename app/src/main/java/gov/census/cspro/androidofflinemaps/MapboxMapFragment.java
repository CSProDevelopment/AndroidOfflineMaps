package  gov.census.cspro.androidofflinemaps;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.RasterLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.RasterSource;
import com.mapbox.mapboxsdk.style.sources.TileSet;
import com.myroutes.mbtiles4j.MBTilesReadException;
import com.myroutes.mbtiles4j.MBTilesReader;
import com.myroutes.mbtiles4j.model.MetadataBounds;
import com.myroutes.mbtiles4j.model.MetadataEntry;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MapboxMapFragment extends  Fragment implements MapFragment
{
    private static final String TAG = MapboxMapFragment.class.getSimpleName();
    private MapView m_mapView;
    private static final String ARG_INITIAL_TILE_SOURCE = "ARG_INITIAL_TILE_SOURCE";
    private MapFragment.OnFragmentInteractionListener m_listener;
    private String m_tileSource;
    private MapboxMap m_map;
    private FeatureCollection m_placemarkers;

    static public MapboxMapFragment newInstance(String initialTileSource)
    {
        MapboxMapFragment fragment = new MapboxMapFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_INITIAL_TILE_SOURCE, initialTileSource);
        fragment.setArguments(args);
        return fragment;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        Mapbox.getInstance(getContext(), "mapbox key goes here");

        MapboxMapOptions options = new MapboxMapOptions();
        m_mapView = new MapView(getContext(), options);
        m_mapView.onCreate(savedInstanceState);

        return m_mapView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        m_mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {

                m_map = mapboxMap;

                Style.Builder styleBuilder = new Style.Builder();

                if (getArguments() != null) {
                    setTileSource(getArguments().getString(ARG_INITIAL_TILE_SOURCE), styleBuilder);
                }

                m_placemarkers = getPlaceMarkers();
                Bitmap markerIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_place_mark_red);

                styleBuilder.withSource(new GeoJsonSource("marker-source", m_placemarkers))
                    .withImage("my-marker-image", markerIcon)
                    .withLayer(new SymbolLayer("marker-layer", "marker-source")
                        .withProperties(PropertyFactory.iconAllowOverlap(true), PropertyFactory.iconImage("my-marker-image"),
                            PropertyFactory.iconSize(new Float(1.5))));

                m_map.setStyle(styleBuilder);

                m_map.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public boolean onMapClick(@NonNull LatLng point) {
                        PointF screenPoint = m_map.getProjection().toScreenLocation(point);
                        List<Feature> features = m_map.queryRenderedFeatures(screenPoint, "marker-layer");
                        if (!features.isEmpty()) {
                            Feature selectedFeature = features.get(0);
                            String label = selectedFeature.getStringProperty("label");
                           /* List<Feature> featureList = m_placemarkers.features();
                            for (int i = 0; i < featureList.size(); i++) {
                                if (featureList.get(i).getStringProperty("label").equals(label)) {
                                    setSelected(i, true);
                                }
                            }*/
                            selectPlace(label);
                        }
                        return false;
                    }
                });
            }
        });
    }

    private void selectPlace(@NonNull String label) {
        final PlacesViewModel viewModel = ViewModelProviders.of(MapboxMapFragment.this).get(PlacesViewModel.class);
        final List<Place> places = viewModel.getPlaces().getValue();
        if (places != null) {
            for (Place p : places) {
                if (p.label.equals(label)) {
                    if (m_listener != null) {
                        m_listener.onMapPlaceClicked(p);
                    }
                }
            }
        }
    }

    @NonNull
    private FeatureCollection getPlaceMarkers() {
        List<Feature> features = new ArrayList<>();
        final PlacesViewModel viewModel = ViewModelProviders.of(MapboxMapFragment.this).get(PlacesViewModel.class);
        List<Place> places = viewModel.getPlaces().getValue();
        if (places != null) {
            for (Place c : places) {
                Feature f = Feature.fromGeometry(Point.fromLngLat(c.longitude, c.latitude));
                f.addStringProperty("label", c.label);
                features.add(f);
            }
        }
        return FeatureCollection.fromFeatures(features);
    }

    @Override
    public void onStart() {
        super.onStart();
        m_mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        m_mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        m_mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        m_mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        m_mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        m_mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        m_mapView.onSaveInstanceState(outState);
    }

    @Override
    public Fragment getFragment()
    {
        return this;
    }

    @Override
    public void setTileSource(String tileSource)
    {
        setTileSource(tileSource, null);
    }

    private void setTileSource(String tileSource, Style.Builder styleBuilder)
    {
        m_tileSource = tileSource;

        try {
            final MBTilesReader reader = new MBTilesReader(new File(m_tileSource));
            MetadataEntry metadata = reader.getMetadata();
            MetadataEntry.TileMimeType tileMimeType = metadata.getTileMimeType();

            m_map.setMaxZoomPreference(reader.getMaxZoom());
            m_map.setMinZoomPreference(reader.getMinZoom());

            final MetadataBounds tileSetBounds = metadata.getTilesetBounds();
            LatLngBounds mapBounds = new LatLngBounds.Builder()
                .include(new LatLng(tileSetBounds.getBottom(), tileSetBounds.getLeft()))
                .include(new LatLng(tileSetBounds.getTop(), tileSetBounds.getRight()))
                .build();
            m_map.setLatLngBoundsForCameraTarget(mapBounds);

            final String filename = FilenameUtils.getName(m_tileSource);
            final String url = "http://localhost:8084/maps/" + filename + "/{z}/{x}/{y}.png";

            RasterSource rasterSource = new RasterSource("raster-source", new TileSet("tileset", url), 256);
            RasterLayer rasterLayer = new RasterLayer("raster-layer", "raster-source");

            if (styleBuilder == null) {

                assert m_map.getStyle() != null && m_map.getStyle().getLayer("raster-layer") != null;

                // Remove old layer
                m_map.getStyle().removeLayer("raster-layer");
                m_map.getStyle().removeSource("raster-source");

                m_map.getStyle().addSource(rasterSource);
                m_map.getStyle().addLayerBelow(rasterLayer, "marker-layer");
            } else {
                assert m_map.getStyle() == null;

                styleBuilder.withSource(rasterSource).withLayer(rasterLayer);
            }

            m_map.moveCamera(CameraUpdateFactory.newLatLngBounds(mapBounds, 0));

        } catch (MBTilesReadException e)
        {
            Log.e(TAG, "Error reading mbtiles file " + m_tileSource, e);
        }
    }

    @Override
    public void showPlace(Place c) {
        CameraPosition newPosition = new CameraPosition.Builder()
            .target(new LatLng(c.latitude, c.longitude))
            .zoom(m_map.getMaxZoomLevel())
            .build();

        m_map.animateCamera(CameraUpdateFactory
            .newCameraPosition(newPosition), 3000);
    }

}
