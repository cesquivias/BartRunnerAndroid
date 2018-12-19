package com.dougkeen.bart.data;

import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dougkeen.bart.R;
import com.dougkeen.bart.model.StationPair;

import java.util.List;

public class StationPairAdapter extends ListAdapter<StationPair, StationPairAdapter.ViewHolder> {
    private static final DiffUtil.ItemCallback<StationPair> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<StationPair>() {
                @Override
                public boolean areItemsTheSame(StationPair oldItem, StationPair newItem) {
                    return oldItem == newItem;
                }
                @Override
                public boolean areContentsTheSame(StationPair oldItem, StationPair newItem) {
                    return oldItem.equals(newItem);
                }
            };

    private final List<StationPair> stationPairs;
    private OnStationPairClickListener onItemClickListener;

    public StationPairAdapter(List<StationPair> stationPairs) {
        super(DIFF_CALLBACK);
        this.stationPairs = stationPairs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View view = inflater.inflate(R.layout.favorite_listing, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {
        final StationPair pair = stationPairs.get(i);
        viewHolder.originText.setText(pair.getOrigin().name);
        viewHolder.destinationText.setText(pair.getDestination().name);
        viewHolder.row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onStationPairClicked(pair);
                }
            }
        });
    }

    public void setOnItemClickListener(OnStationPairClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnStationPairClickListener {
        void onStationPairClicked(StationPair stationPair);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View row;
        public final TextView originText;
        public final TextView destinationText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            row = itemView;
            originText = itemView.findViewById(R.id.originText);
            destinationText = itemView.findViewById(R.id.destinationText);
        }
    }

    @Override
    public int getItemCount() {
        return stationPairs.size();
    }
}
