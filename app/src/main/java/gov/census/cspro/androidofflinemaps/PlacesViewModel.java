package gov.census.cspro.androidofflinemaps;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

class PlacesViewModel extends ViewModel {

    private LiveData<List<Place>> places;

    LiveData<List<Place>> getPlaces() {
        if (places == null) {
            ArrayList<Place> placeList = new ArrayList<>();
            placeList.add(new Place("Census Bureau", 38.84839,-76.931098 ));
            placeList.add(new Place("Metro", 38.844245, -76.932526 ));
            placeList.add(new Place("Popeyes", 38.845582, -76.926732 ));
            placeList.add(new Place("First Cash Pawn", 38.845473, -76.927177 ));
            placeList.add(new Place("Goodyear", 38.844646, -76.928041 ));
            placeList.add(new Place("Number 1 Carry Out", 38.843917, -76.928867 ));
            placeList.add(new Place("Best 1 Convenience", 38.844027, -76.928667 ));
            placeList.add(new Place("Elite Barbers", 38.843991, -76.928742 ));
            placeList.add(new Place("Silver Hill Liquors", 38.843913, -76.928860 ));
            placeList.add(new Place("Number 1 Carry Out", 38.843888, -76.928899 ));
            placeList.add(new Place("Food for Life", 38.846992, -76.922505 ));
            placeList.add(new Place("Royce TV", 38.846796, -76.922261 ));
            placeList.add(new Place("We R One", 38.846932, -76.922382 ));
            placeList.add(new Place("Galaxy Food", 38.847118, -76.922806 ));
            placeList.add(new Place("Silvestre Chicken", 38.847926, -76.924657 ));
            placeList.add(new Place("Exxon", 38.848325, -76.924625 ));
            placeList.add(new Place("Annie Hair Braiding", 38.847970, -76.924807 ));
            placeList.add(new Place("Hunter Memorial Church", 38.846700, -76.925349 ));
            placeList.add(new Place("Sheet Metal Workers International Association", 38.847383, -76.925483 ));
            placeList.add(new Place("Subway", 38.850508, -76.927382 ));
            placeList.add(new Place("Dollar General", 38.850411, -76.927171 ));
            placeList.add(new Place("Post Office", 38.851255, -76.929448 ));
            placeList.add(new Place("Ameritech Tires", 38.851604, -76.929815 ));
            placeList.add(new Place("Shell", 38.852097, -76.930502 ));
            placeList.add(new Place("Census Auto Repairs", 38.852225, -76.931712 ));
            placeList.add(new Place("Bradbury Recreation Center", 38.857865, -76.933541 ));
            placeList.add(new Place("NOAA Satellite Operations Facility", 38.851824, -76.936717 ));
            placeList.add(new Place("National Archives", 38.851247, -76.941770 ));
            placeList.add(new Place("National Maritime Intelligence Center", 38.848949, -76.936282 ));
            placeList.add(new Place("Suitland House", 38.846747, -76.931647 ));
            placeList.add(new Place("Child Care Center", 38.849707, -76.933616 ));

            places = new MutableLiveData<>();
            ((MutableLiveData<List<Place>>) places).setValue(placeList);
        }
        return places;
    }

}
