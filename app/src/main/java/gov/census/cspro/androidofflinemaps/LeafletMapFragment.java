package gov.census.cspro.androidofflinemaps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.myroutes.mbtiles4j.MBTilesReadException;
import com.myroutes.mbtiles4j.MBTilesReader;
import com.myroutes.mbtiles4j.model.MetadataBounds;
import com.myroutes.mbtiles4j.model.MetadataEntry;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.List;
import java.util.Locale;


/**
 * A {@link MapFragment} subclass implemented using Leaflet.js in a WebView.
 */
public class LeafletMapFragment extends Fragment implements MapFragment {
    private static final String TAG = LeafletMapFragment.class.getSimpleName();
    private static final String ARG_INITIAL_TILE_SOURCE = "ARG_INITIAL_TILE_SOURCE";

    private MapFragment.OnFragmentInteractionListener m_listener;
    private WebView m_webView;
    private int m_maxZoom;
    private String m_layer;

    public LeafletMapFragment() {
        // Required empty public constructor
    }

    static LeafletMapFragment newInstance(String initialTileSource)
    {
        LeafletMapFragment fragment = new LeafletMapFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_INITIAL_TILE_SOURCE, initialTileSource);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            m_layer = getArguments().getString(ARG_INITIAL_TILE_SOURCE);
        }
    }

    @Override
    public Fragment getFragment()
    {
        return this;
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

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        m_webView = new WebView(getContext());
        WebSettings webSettings = m_webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setGeolocationEnabled(true);

        m_webView.setWebViewClient(new WebViewClient() {

            private boolean shouldOverrideUrlLoading(WebView view, Uri url)
            {
                // Allow loading of local sites but any other sites are opened
                // in new browser activity e.g. clicking on the little leaflet logo
                if (url.getHost() != null && url.getHost() != null && url.getHost().equals("localhost")) {
                    return false;
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, url);
                    view.getContext().startActivity(intent);
                    return true;
                }
            }

            @Override
            @TargetApi(21)
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return shouldOverrideUrlLoading(view, request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // For TargetAPI < 21
                return shouldOverrideUrlLoading(view, Uri.parse(url));
            }

            private WebResourceResponse shouldInterceptRequest(Uri url)
            {
                // For security reasons, and to force it to work offline when testing,
                // don't load resources from anywhere but localhost
                if (url != null && url.getHost() != null && url.getHost().equals("localhost")) {
                    return null;
                } else {
                    return new WebResourceResponse("text/html", "UTF-8", null);
                }
            }

            @Override
            @TargetApi(21)
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                              WebResourceRequest request)
            {
                return shouldInterceptRequest(request.getUrl());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                              String url)
            {
                return shouldInterceptRequest(Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url)
            {
                final PlacesViewModel viewModel = ViewModelProviders.of(LeafletMapFragment.this).get(PlacesViewModel.class);
                List<Place> places = viewModel.getPlaces().getValue();
                if (places != null) {
                    StringBuilder markerJS = new StringBuilder();
                    markerJS.append("addMarkers({\"type\": \"FeatureCollection\",\"features\": [");
                    for (int i = 0; i < places.size(); ++i) {
                        Place c = places.get(i);
                        markerJS.append("{\"type\": \"Feature\",");
                        markerJS.append("\"properties\": {");
                        markerJS.append(String.format(Locale.US, "\"number\": %d,", i));
                        markerJS.append(String.format(Locale.US, "\"name\": \"%s\"", c.label));
                        markerJS.append("},");
                        markerJS.append("\"geometry\": {");
                        markerJS.append("\"type\": \"Point\",");
                        markerJS.append(String.format(Locale.US, "\"coordinates\": [%f,%f]", c.longitude, c.latitude));
                        markerJS.append("}},");
                    }
                    markerJS.append("]})");
                    runJavascript(markerJS.toString());
                }

                setTileSource(m_layer);
            }
        });

        m_webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d(TAG, cm.message() + " -- From line "
                    + cm.lineNumber() + " of "
                    + cm.sourceId() );
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                                                           GeolocationPermissions.Callback callback)
            {
                if (getActivity() != null && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, true);
                }
            }
        });

        m_webView.addJavascriptInterface(new WebAppInterface(getContext()), "Android");
        m_webView.loadUrl("http://localhost:8084/assets/leaflet/index.html");
        return m_webView;
    }

    private void runJavascript(String js) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            m_webView.evaluateJavascript(js, null);
        } else {
            m_webView.loadUrl("javascript:" + js);
        }
    }

    @Override
    public void showPlace(Place c) {
        runJavascript(String.format(Locale.US, "flyTo(%f,%f,%d);openMarkerPopup(\"%s\")", c.latitude, c.longitude, m_maxZoom, c.label));
    }

    @Override
    public void setTileSource(String tileSource)
    {
        m_layer = tileSource;
        try {
            MBTilesReader reader = new MBTilesReader(new File(tileSource));
            MetadataEntry metadata = reader.getMetadata();
            MetadataEntry.TileMimeType tileMimeType = metadata.getTileMimeType();
            MetadataBounds bounds = metadata.getTilesetBounds();
            String attribution = metadata.getAttribution();
            int minZoom = reader.getMinZoom();
            int maxZoom = reader.getMaxZoom();
            String boundsString = String.format(Locale.US, "[[%f,%f],[%f,%f]]",
                bounds.getBottom(), bounds.getLeft(), bounds.getTop(), bounds.getRight());
            String filename = FilenameUtils.getName(tileSource);
            if (tileMimeType == MetadataEntry.TileMimeType.PBF) {
                String url = String.format(Locale.US, "http://localhost:8084/maps/%s/{z}/{x}/{y}.pbf",
                    filename);
                runJavascript(String.format(Locale.US, "addVectorLayer('%s', %s, %d, %d, '%s');",
                    url, boundsString, minZoom, maxZoom, attribution));
            } else {
                String url = String.format(Locale.US, "http://localhost:8084/maps/%s/{z}/{x}/{y}.png",
                    filename);
                runJavascript(String.format(Locale.US, "addRasterLayer('%s', %s, %d, %d, '%s');",
                    url, boundsString, minZoom, maxZoom, attribution));
            }
            m_maxZoom = maxZoom;
        } catch (MBTilesReadException e) {
            Log.e(TAG, "Error reading MBTiles file " + tileSource, e);
        }

    }

    public class WebAppInterface {

        /** Instantiate the interface and set the context */
        WebAppInterface(@SuppressWarnings("UnusedParameters") Context c) {
        }

        @JavascriptInterface
        @SuppressWarnings("unused")
        public void onClickMarker(int markerNumber) {
            if (m_listener != null) {
                final PlacesViewModel viewModel = ViewModelProviders.of(LeafletMapFragment.this).get(PlacesViewModel.class);
                List<Place> places = viewModel.getPlaces().getValue();
                if (places != null && markerNumber >= 0 && markerNumber <= places.size()) {
                    m_listener.onMapPlaceClicked(places.get(markerNumber));
                }
            }
        }
    }

}
