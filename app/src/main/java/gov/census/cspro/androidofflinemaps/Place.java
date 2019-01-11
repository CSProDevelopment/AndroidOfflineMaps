package gov.census.cspro.androidofflinemaps;

class Place {

    Place(String label, double lat, double lon)
    {
        this.label = label;
        this.latitude = lat;
        this.longitude = lon;
    }

    String label;
    double latitude;
    double longitude;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Place aPlace = (Place) o;

        if (Double.compare(aPlace.latitude, latitude) != 0) return false;
        if (Double.compare(aPlace.longitude, longitude) != 0) return false;
        return label != null ? label.equals(aPlace.label) : aPlace.label == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = label != null ? label.hashCode() : 0;
        temp = Double.doubleToLongBits(latitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
