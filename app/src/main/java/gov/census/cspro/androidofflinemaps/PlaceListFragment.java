package gov.census.cspro.androidofflinemaps;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.arch.lifecycle.ViewModelProviders;
import android.view.animation.BounceInterpolator;


/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class PlaceListFragment extends Fragment {

    private OnListFragmentInteractionListener m_listener;
    private LinearLayoutManager m_layoutManager;
    private PlaceRecyclerViewAdapter m_adapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PlaceListFragment() {
    }

    @SuppressWarnings("unused")
    public static PlaceListFragment newInstance() {
        PlaceListFragment fragment = new PlaceListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_place_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            m_layoutManager = new LinearLayoutManager(context);
            recyclerView.setLayoutManager(m_layoutManager);
            final PlacesViewModel viewModel = ViewModelProviders.of(this).get(PlacesViewModel.class);
            m_adapter = new PlaceRecyclerViewAdapter(viewModel.getPlaces().getValue(), m_listener);
            recyclerView.setAdapter(m_adapter);
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            m_listener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        m_listener = null;
    }

    public void showPlace(@NonNull final Place c) {

        Activity activity = getActivity();
        if (activity != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int index = m_adapter.getItems().indexOf(c);
                    m_layoutManager.scrollToPosition(index);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            TypedValue a = new TypedValue();
                            getActivity().getTheme().resolveAttribute(android.R.attr.windowBackground, a, true);
                            int color;
                            if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                                // windowBackground is a color
                                color = a.data;
                            } else {
                                // windowBackground is not a color, probably a drawable
                                color = Color.parseColor("#FAFAFA");
                            }
                            View itemView = m_layoutManager.findViewByPosition(index);
                            if (itemView != null) {
                                float y = itemView.getTranslationY(), distance = 20F;

                                AnimatorSet s = new AnimatorSet();

                                ObjectAnimator up = ObjectAnimator.ofFloat(itemView, "translationY", y - distance);
                                up.setDuration(200);
                                ObjectAnimator down = ObjectAnimator.ofFloat(itemView, "translationY", y);
                                down.setInterpolator(new BounceInterpolator());
                                down.setDuration(800);
                                s.playSequentially(up, down);
                                s.start();
                                // bounce.setDuration(1000);
                                //bounce.start();
                        /*
                        s.playSequentially(
                            ObjectAnimator.ofFloat(itemView, "translationY", y- distance).setInterpolator(new BounceInterpolator()),
                            ObjectAnimator.ofFloat(itemView, "translationY", y),
                            ObjectAnimator.ofFloat(itemView, "translationY", y- (distance/2)),
                            ObjectAnimator.ofFloat(itemView, "translationY", y));
                        s.setDuration(600);
                        s.start(); */

     /*                    ObjectAnimator toGrey = ObjectAnimator.ofObject(
                            itemView, // Object to animating
                            "backgroundColor", // Property to animate
                            new ArgbEvaluator(), // Interpolation function
                            color, // Start color
                            Color.GRAY // End color
                        ); // Finally, start the anmation
                        ObjectAnimator andBack = ObjectAnimator.ofObject(
                            itemView, // Object to animating
                            "backgroundColor", // Property to animate
                            new ArgbEvaluator(), // Interpolation function
                            Color.GRAY, // Start color
                            color // End color
                        ); // Finally, start the anmation
                        AnimatorSet s = new AnimatorSet();
                        s.playSequentially(toGrey, andBack); */
                            }
                        }
                    }, 500);



                }
            });
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListPlaceSelected(Place c);
    }
}
