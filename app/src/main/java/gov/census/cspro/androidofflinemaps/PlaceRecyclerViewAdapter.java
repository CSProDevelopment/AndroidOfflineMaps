package gov.census.cspro.androidofflinemaps;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import gov.census.cspro.androidofflinemaps.PlaceListFragment.OnListFragmentInteractionListener;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Place} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class PlaceRecyclerViewAdapter extends RecyclerView.Adapter<PlaceRecyclerViewAdapter.ViewHolder> {

    private final List<Place> m_places;
    private final OnListFragmentInteractionListener m_listener;

    PlaceRecyclerViewAdapter(List<Place> places, OnListFragmentInteractionListener listener) {
        m_places = places;
        m_listener = listener;
    }

    List<Place> getItems()
    {
        return m_places;
    }

    @Override
    public @NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.fragment_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final @NonNull ViewHolder holder, int position) {
        holder.m_place = m_places.get(position);
        holder.m_labelView.setText(m_places.get(position).label);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != m_listener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    m_listener.onListPlaceSelected(holder.m_place);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return m_places.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView m_labelView;
        Place m_place;

        ViewHolder(View view) {
            super(view);
            mView = view;
            m_labelView = view.findViewById(R.id.label);
        }
     }
}
