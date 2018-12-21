package com.dougkeen.bart.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.dougkeen.bart.R;
import com.dougkeen.bart.controls.CountdownTextView;
import com.dougkeen.bart.controls.TimedTextSwitcher;
import com.dougkeen.bart.model.StationPair;
import com.dougkeen.bart.model.StationPairDeparture;
import com.dougkeen.bart.model.TextProvider;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class StationPairDepartureAdapter extends ListAdapter<StationPairDeparture, StationPairDepartureAdapter.ViewHolder> {
    private static final DiffUtil.ItemCallback<StationPairDeparture> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<StationPairDeparture>() {
                @Override
                public boolean areItemsTheSame(@NonNull StationPairDeparture oldItem,
                        @NonNull StationPairDeparture newItem) {
                    return oldItem == newItem;
                }
                @Override
                public boolean areContentsTheSame(@NonNull StationPairDeparture oldItem,
                        @NonNull StationPairDeparture newItem) {
                    return newItem.equals(oldItem);
                }
            };

    private final List<StationPairDeparture> stationPairDepartures;
    private OnStationPairClickListener onItemClickListener;

    public StationPairDepartureAdapter(List<StationPairDeparture> stationPairDepartures) {
        super(DIFF_CALLBACK);
        this.stationPairDepartures = stationPairDepartures;
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
        final StationPairDeparture stationPairDeparture = stationPairDepartures.get(i);
        final StationPair pair = stationPairDeparture.getStationPair();
        viewHolder.originText.setText(pair.getOrigin().name);
        viewHolder.destinationText.setText(pair.getDestination().name);
        viewHolder.row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onStationPairClicked(stationPairDeparture);
                }
            }
        });
        viewHolder.row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onStationPairLongClicked(stationPairDeparture);
                    return true;
                }
                return false;
            }
        });

        final Context context = viewHolder.row.getContext();
        initTextSwitcher(viewHolder.uncertaintyTextSwitcher, context);

        if (/*etdListener == null ||*/ stationPairDeparture.getFirstDeparture() == null) {
            viewHolder.uncertaintyTextSwitcher.setCurrentText(pair.getFare());
        } else {
            viewHolder.countdownTextView.setText(stationPairDeparture.getFirstDeparture()
                    .getCountdownText());
            viewHolder.countdownTextView.setTextProvider(new TextProvider() {
                @Override
                public String getText(long tickNumber) {
                    return stationPairDeparture.getFirstDeparture().getCountdownText();
                }
            });

            final String uncertaintyText = stationPairDeparture.getFirstDeparture()
                    .getUncertaintyText();
            if (!StringUtils.isBlank(uncertaintyText)) {
                viewHolder.uncertaintyTextSwitcher.setCurrentText(uncertaintyText);
            } else {
                viewHolder.uncertaintyTextSwitcher.setCurrentText(pair.getFare());
            }
            viewHolder.uncertaintyTextSwitcher.setTextProvider(new TextProvider() {
                @Override
                public String getText(long tickNumber) {
                    final String arrive = stationPairDeparture.getFirstDeparture()
                            .getEstimatedArrivalTimeText(context, true);
                    int mod = StringUtils.isNotBlank(arrive) ? 8 : 6;
                    if (tickNumber % mod <= 1) {
                        return pair.getFare();
                    } else if (tickNumber % mod <= 3) {
                        return "Dep "
                                + stationPairDeparture.getFirstDeparture()
                                .getEstimatedDepartureTimeText(
                                        context, true);
                    } else if (mod == 8 && tickNumber % mod <= 5) {
                        return "Arr " + arrive;
                    } else {
                        return stationPairDeparture.getFirstDeparture()
                                .getUncertaintyText();
                    }
                }
            });
        }
    }

    private void initTextSwitcher(TextSwitcher textSwitcher, final Context context) {
        if (textSwitcher.getInAnimation() == null) {
            textSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
                public View makeView() {
                    return LayoutInflater.from(context).inflate(
                            R.layout.uncertainty_textview, null);
                }
            });

            textSwitcher.setInAnimation(AnimationUtils.loadAnimation(
                    context, android.R.anim.slide_in_left));
            textSwitcher.setOutAnimation(AnimationUtils.loadAnimation(
                    context, android.R.anim.slide_out_right));
        }
    }

    public void setOnItemClickListener(OnStationPairClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnStationPairClickListener {
        void onStationPairClicked(StationPairDeparture stationPair);

        void onStationPairLongClicked(StationPairDeparture stationPair);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View row;
        public final TextView originText;
        public final TextView destinationText;
        public final TimedTextSwitcher uncertaintyTextSwitcher;
        public final CountdownTextView countdownTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            row = itemView;
            originText = itemView.findViewById(R.id.originText);
            destinationText = itemView.findViewById(R.id.destinationText);
            uncertaintyTextSwitcher = itemView.findViewById(R.id.uncertainty);
            countdownTextView = itemView.findViewById(R.id.countdownText);
        }
    }

    @Override
    public int getItemCount() {
        return stationPairDepartures.size();
    }
}
