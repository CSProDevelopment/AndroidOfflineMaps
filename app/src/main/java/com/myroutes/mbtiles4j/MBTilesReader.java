package com.myroutes.mbtiles4j;

import com.myroutes.mbtiles4j.*;
import com.myroutes.mbtiles4j.TileIterator;
import com.myroutes.mbtiles4j.model.MetadataEntry;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MBTilesReader {

    private File f;
    private Connection connection;

    public MBTilesReader(File f) throws MBTilesReadException {
        try {
            connection = com.myroutes.mbtiles4j.SQLHelper.establishConnection(f);
        } catch (MBTilesException e) {
            throw new MBTilesReadException("Establish Connection to " + f.getAbsolutePath() + " failed", e);
        }
        this.f = f;
    }

    public File close() {
        try {
            connection.close();
        } catch (SQLException e) {
        }
        return f;
    }

    public MetadataEntry getMetadata() throws MBTilesReadException {
        String sql = "SELECT * from metadata;";
        try {
            ResultSet resultSet = com.myroutes.mbtiles4j.SQLHelper.executeQuery(connection, sql);
            MetadataEntry ent = new MetadataEntry();
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String value = resultSet.getString("value");
                ent.addKeyValue(name, value);
            }
            return ent;
        } catch (MBTilesException | SQLException e) {
            throw new MBTilesReadException("Get Metadata failed", e);
        }
    }

    public com.myroutes.mbtiles4j.TileIterator getTiles() throws MBTilesReadException {
        String sql = "SELECT * from tiles;";
        try {
            ResultSet resultSet = com.myroutes.mbtiles4j.SQLHelper.executeQuery(connection, sql);
            return new TileIterator(resultSet);
        } catch (MBTilesException e) {
            throw new MBTilesReadException("Access Tiles failed", e);
        }
    }

    public Tile getTile(int zoom, int column, int row) throws MBTilesReadException {
        String sql = String.format("SELECT tile_data FROM tiles WHERE zoom_level = %d AND tile_column = %d AND tile_row = %d", zoom, column, row);

        try {
            ResultSet resultSet = SQLHelper.executeQuery(connection, sql);
            if (resultSet.next()) {
                InputStream tile_data;
                if (resultSet.getBytes(1) != null) {
                    tile_data = new ByteArrayInputStream(resultSet.getBytes(1));
                } else {
                    tile_data = new ByteArrayInputStream(new byte[]{});
                }

                return new Tile(zoom, column, row, tile_data);
            } else {
                throw new MBTilesReadException(String.format("Could not get Tile for z:%d, column:%d, row:%d", zoom, column, row), null);
            }
        } catch (MBTilesException | SQLException e) {
            throw new MBTilesReadException(String.format("Could not get Tile for z:%d, column:%d, row:%d", zoom, column, row), e);
        }
    }

    public int getMaxZoom() throws MBTilesReadException {
        String sql = "SELECT MAX(zoom_level) FROM tiles";

        try {
            ResultSet resultSet = SQLHelper.executeQuery(connection, sql);
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return -1;
            }
        } catch (MBTilesException | SQLException e) {
            throw new MBTilesReadException("Could not get max zoom", e);
        }
    }

    public int getMinZoom() throws MBTilesReadException {
        String sql = "SELECT MIN(zoom_level) FROM tiles";

        try {
            ResultSet resultSet = SQLHelper.executeQuery(connection, sql);
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return -1;
            }
        } catch (MBTilesException | SQLException e) {
            throw new MBTilesReadException("Could not get min zoom", e);
        }
    }
}
