package gov.census.cspro.androidofflinemaps;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.myroutes.mbtiles4j.MBTilesReadException;
import com.myroutes.mbtiles4j.MBTilesReader;
import com.myroutes.mbtiles4j.Tile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;

public class MapServer extends NanoHTTPD {

    private static final String TAG = MapServer.class.getSimpleName();
    private static final String ASSETS_FOLDER = "/assets";
    private final Context m_context;
    private final String m_root;

    private HashMap<String, MBTilesReader> m_readers = new HashMap<>();

    MapServer(@NonNull String root, @NonNull String hostname, int port, Context context) {
        super(hostname, port);
        m_context = context;
        m_root = root.endsWith("/") ? root : root + "/";
    }

    @Override
    public Response serve(IHTTPSession session) {
        Log.d(TAG,"SERVE ::  URI "+ session.getUri());

        final Pattern tilesPattern = Pattern.compile("^/maps/(.+\\.mbtiles)/(\\d+)/(\\d+)/(\\d+)\\.(.*)$");
        final Matcher tilesPatternMatcher = tilesPattern.matcher(session.getUri());

        try {
            if (tilesPatternMatcher.matches()) {
                String filename = tilesPatternMatcher.group(1);
                int z = Integer.parseInt(tilesPatternMatcher.group(2));
                int x = Integer.parseInt(tilesPatternMatcher.group(3));
                int y = Integer.parseInt(tilesPatternMatcher.group(4));
                String extension = tilesPatternMatcher.group(5);
                return serveTileFromMbtiles(filename, x, y, z, extension);
            } else if (session.getUri().startsWith(ASSETS_FOLDER)) {
                return serveAssetFile(session.getUri().substring(1));
            } else {
                return serveRegularFile(session.getUri().substring(1));
            }
        } catch (Exception e)
        {
            Log.e(TAG, "Unhandled exception", e);
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
        }
    }

    private Response serveTileFromMbtiles(String filename, int x, int y, int z, String extension)
    {
        try {
            MBTilesReader r = getReader(filename);
            y = (1 << z) - y - 1;
            Tile tile = r.getTile(z, x, y);

            if (isVectorTile(extension)) {
                return serveVectorTile(tile);
            } else {
                return serveRasterTile(extension, tile);
            }

        } catch (MBTilesReadException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, tile not found in tiles file. " + filename);
        }
    }

    @NonNull
    private Response serveRasterTile(String extension, Tile tile) {
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return NanoHTTPD.newChunkedResponse(Response.Status.OK, mimeType, tile.getData());
    }

    @NonNull
    private Response serveVectorTile(Tile tile) {
        String mimeType = "application/x-protobuf;type=mapbox-vector";
        Response response = NanoHTTPD.newChunkedResponse(Response.Status.OK, mimeType, tile.getData());

        // pbf vector tiles are already gziped. Need to tell client that data is gzipped
        // by adding the content-encoding header and tell NanoHttpd NOT to do it's own gziping
        response.addHeader("Content-Encoding", "gzip");
        response.setGzipEncoding(false);

        return response;
    }

    private MBTilesReader getReader(String filename) throws MBTilesReadException
    {
        if (m_readers.containsKey(filename)) {
            return m_readers.get(filename);
        } else {
            MBTilesReader r = new MBTilesReader(new File(m_root, filename));
            m_readers.put(filename, r);
            return r;
        }
    }

    private static boolean isVectorTile(@NonNull String extension) {
        return extension.equals("pbf");
    }

    private @NonNull Response serveRegularFile(@NonNull String path)
    {
        try {
            InputStream is = new FileInputStream(m_root + path);
            return NanoHTTPD.newChunkedResponse(Response.Status.OK, getMimeType(path), is);
        } catch (IOException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
        }
    }

    private @NonNull Response serveAssetFile(@NonNull String path)
    {
        try {
            InputStream is = m_context.getAssets().open(path.substring(ASSETS_FOLDER.length()));
            return NanoHTTPD.newChunkedResponse(Response.Status.OK, getMimeType(path), is);
        } catch (IOException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
        }
    }

    private static String getMimeType(@NonNull String path)
    {
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }
}
