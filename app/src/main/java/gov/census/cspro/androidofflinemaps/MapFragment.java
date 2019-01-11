package gov.census.cspro.androidofflinemaps;

import android.support.v4.app.Fragment;

public interface MapFragment {

    Fragment getFragment();

    void setTileSource(String tileSource);

    void showPlace(Place c);

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    interface OnFragmentInteractionListener {
        void onMapPlaceClicked(Place p);
    }

}
