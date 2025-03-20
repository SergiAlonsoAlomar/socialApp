package com.example.socialapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class commentsAdapter extends RecyclerView.Adapter<commentsAdapter.CommentViewHolder> {
    private List<comment> comments;

    public commentsAdapter(List<comment> comments) {
        this.comments = comments;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        comment comment = comments.get(position);
        holder.authorTextView.setText(comment.getAuthor());
        holder.contentTextView.setText(comment.getContent());

        if (comment.getAuthorPhotoUrl() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(comment.getAuthorPhotoUrl())
                    .circleCrop()
                    .into(holder.authorPhotoImageView);
        } else {
            holder.authorPhotoImageView.setImageResource(R.drawable.user);
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    // Función para establecer la lista de comentarios
    public void establecerLista(List<comment> nuevaLista) {
        this.comments = nuevaLista;
        notifyDataSetChanged(); // Notificar al adaptador que los datos han cambiado
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView authorPhotoImageView;
        TextView authorTextView, contentTextView;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorPhotoImageView = itemView.findViewById(R.id.authorPhotoImageView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
        }
    }
}