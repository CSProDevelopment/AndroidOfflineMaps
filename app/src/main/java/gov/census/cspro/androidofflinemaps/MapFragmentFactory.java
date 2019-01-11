package gov.census.cspro.androidofflinemaps;

class MapFragmentFactory {

    private static final String TYPE_LEAFLET = "Leaflet";
    private static final String TYPE_GOOGLE = "Google Maps";
    private static final String TYPE_MAPBOX = "Mapbox";
    private static final String[] m_types = new String[] {TYPE_MAPBOX,TYPE_GOOGLE, TYPE_LEAFLET};

    static String[] getAvailableTypes()
    {
        return m_types;
    }

    static MapFragment newInstance(String mapType, String initialTileSource)
    {
        switch (mapType) {
            case TYPE_LEAFLET:
                return LeafletMapFragment.newInstance(initialTileSource);
            case TYPE_GOOGLE:
                return GoogleMapFragment.newInstance(initialTileSource);
            case TYPE_MAPBOX:
                return MapboxMapFragment.newInstance(initialTileSource);
            default:
                return null;
        }
    }
}
