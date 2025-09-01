package com.example.app1.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app1.R;
import com.example.app1.data.Entities.Museo;
import java.util.List;

public class MuseoAdapter extends RecyclerView.Adapter<MuseoAdapter.MuseoViewHolder> {

    private List<Museo> museoList;

    public MuseoAdapter(List<Museo> museoList) {
        this.museoList = museoList;
    }

    @NonNull
    @Override
    public MuseoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_museo, parent, false);
        return new MuseoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MuseoViewHolder holder, int position) {
        Museo museo = museoList.get(position);
        holder.bind(museo);
    }

    @Override
    public int getItemCount() {
        return museoList != null ? museoList.size() : 0;
    }

    public void updateData(List<Museo> newMusei) {
        this.museoList = newMusei;
        notifyDataSetChanged();
    }

    public static class MuseoViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView locationTextView;
        private TextView timeTextView;

        public MuseoViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.textViewMuseoName);
            locationTextView = itemView.findViewById(R.id.textViewLocation);
            timeTextView = itemView.findViewById(R.id.textViewTime);
        }

        public void bind(Museo museo) {
            nameTextView.setText(museo.getName());
            locationTextView.setText(String.format("Lat: %.4f, Lon: %.4f",
                    museo.getLatitude(), museo.getLongitude()));
            timeTextView.setText(String.format("Tempo visita: %d min",
                    museo.getEstimatedVisitTime()));
        }
    }
}