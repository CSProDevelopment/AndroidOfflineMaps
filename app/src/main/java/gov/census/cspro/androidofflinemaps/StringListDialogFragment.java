package gov.census.cspro.androidofflinemaps;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     StringListDialogFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 * <p>You activity (or fragment) needs to implement {@link StringListDialogFragment.Listener}.</p>
 */
public class StringListDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_ITEMS = "items";
    private static final String ARG_REQUEST_CODE = "request";
    private Listener mListener;

    public static StringListDialogFragment newInstance(String items[], int requestCode) {
        final StringListDialogFragment fragment = new StringListDialogFragment();
        final Bundle args = new Bundle();
        args.putStringArray(ARG_ITEMS, items);
        args.putInt(ARG_REQUEST_CODE, requestCode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_string_list_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        assert getArguments() != null;
        recyclerView.setAdapter(new StringAdapter(getArguments().getStringArray(ARG_ITEMS)));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Fragment parent = getParentFragment();
        if (parent != null) {
            mListener = (Listener) parent;
        } else {
            mListener = (Listener) context;
        }
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    public interface Listener {
        void onStringChosen(int requestCode, String s);
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        final TextView text;

        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.fragment_string_list_dialog_item, parent, false));
            text = itemView.findViewById(R.id.text);
        }

    }

    private class StringAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final String[] m_items;

        StringAdapter(String[] items) {
            m_items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            holder.text.setText(m_items[position]);
            holder.text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        assert getArguments() != null;
                        mListener.onStringChosen(getArguments().getInt(ARG_REQUEST_CODE),m_items[holder.getAdapterPosition()]);
                        dismiss();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return m_items.length;
        }

    }

}
