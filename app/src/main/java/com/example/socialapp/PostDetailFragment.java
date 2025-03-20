package com.example.socialapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class PostDetailFragment extends Fragment {

    NavController navController;
    Client client;
    AppViewModel appViewModel;
    RecyclerView commentsRecyclerView;
    commentsAdapter commentsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        commentsRecyclerView = view.findViewById(R.id.commentsRecyclerView);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        commentsAdapter = new commentsAdapter(new ArrayList<>());
        commentsRecyclerView.setAdapter(commentsAdapter);

        // Botón para añadir comentarios
        FloatingActionButton addCommentButton = view.findViewById(R.id.addCommentButton);
        addCommentButton.setOnClickListener(v -> {
            navController.navigate(R.id.newCommentFragment);
        });

        // Obtener el post seleccionado
        appViewModel.postSeleccionado.observe(getViewLifecycleOwner(), post -> {
            if (post != null) {
                // Mostrar el post
                ImageView authorPhotoImageView = view.findViewById(R.id.authorPhotoImageView);
                TextView authorTextView = view.findViewById(R.id.authorTextView);
                TextView contentTextView = view.findViewById(R.id.contentTextView);
                ImageView mediaImageView = view.findViewById(R.id.mediaImage);

                if (post.get("authorPhotoUrl") == null) {
                    authorPhotoImageView.setImageResource(R.drawable.user);
                } else {
                    Glide.with(requireView()).load(post.get("authorPhotoUrl").toString()).circleCrop()
                            .into(authorPhotoImageView);
                }
                authorTextView.setText(post.get("author").toString());
                contentTextView.setText(post.get("content").toString());

                if (post.get("mediaUrl") != null) {
                    mediaImageView.setVisibility(View.VISIBLE);
                    if ("audio".equals(post.get("mediaType").toString())) {
                        Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(mediaImageView);
                    } else {
                        Glide.with(requireView()).load(post.get("mediaUrl").toString()).centerCrop().into(mediaImageView);
                    }
                } else {
                    mediaImageView.setVisibility(View.GONE);
                }

                // Cargar comentarios solo cuando se entra en el detalle del post
                cargarComentarios(post.get("$id").toString());
            }
        });
    }

    void cargarComentarios(String postId) {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID), // databaseId
                    getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID), // collectionId
                    List.of(Query.Companion.equal("postId", postId)), // Consulta corregida
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al obtener los comentarios: " + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        System.out.println(result.toString());
                        List<comment> comments = new ArrayList<>();
                        for (Document<Map<String, Object>> doc : result.getDocuments()) {
                            // Verificar si los campos son null antes de acceder a ellos
                            String postIdComment = doc.getData().get("postId") != null ? doc.getData().get("postId").toString() : "";
                            String author = doc.getData().get("author") != null ? doc.getData().get("author").toString() : "";
                            String content = doc.getData().get("content") != null ? doc.getData().get("content").toString() : "";
                            String authorPhotoUrl = doc.getData().get("authorPhotoUrl") != null ? doc.getData().get("authorPhotoUrl").toString() : null;

                            comments.add(new comment(
                                    postIdComment,
                                    author,
                                    content,
                                    authorPhotoUrl
                            ));
                        }
                        mainHandler.post(() -> commentsAdapter.establecerLista(comments)); // Actualizar el adaptador
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }
}