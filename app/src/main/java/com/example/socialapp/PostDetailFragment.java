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
import android.widget.Button;
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
import io.appwrite.services.Account;
import io.appwrite.services.Databases;

public class PostDetailFragment extends Fragment {

    NavController navController;
    Client client;
    AppViewModel appViewModel;
    RecyclerView commentsRecyclerView;
    commentsAdapter commentsAdapter;
    Button deletePostButton; // Botón para eliminar el post
    String userId; // ID del usuario actual

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

        // Botón para eliminar el post
        deletePostButton = view.findViewById(R.id.deletePostButton);
        deletePostButton.setVisibility(View.GONE); // Ocultar por defecto

        // Obtener el usuario actual
        obtenerUsuarioActual();

        // Obtener el post seleccionado
        appViewModel.postSeleccionado.observe(getViewLifecycleOwner(), post -> {
            if (post != null) {
                mostrarPost(post);
                verificarYMostrarBotonEliminar(post);
                cargarComentarios(post.get("$id").toString());
            }
        });
    }

    private void obtenerUsuarioActual() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            new Account(client).get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }
                mainHandler.post(() -> {
                    userId = result.getId(); // Guardar el ID del usuario actual
                    // Verificar si el post ya está cargado y actualizar el botón de eliminar
                    if (appViewModel.postSeleccionado.getValue() != null) {
                        verificarYMostrarBotonEliminar(appViewModel.postSeleccionado.getValue());
                    }
                });
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    private void mostrarPost(Map<String, Object> post) {
        ImageView authorPhotoImageView = requireView().findViewById(R.id.authorPhotoImageView);
        TextView authorTextView = requireView().findViewById(R.id.authorTextView);
        TextView contentTextView = requireView().findViewById(R.id.contentTextView);
        ImageView mediaImageView = requireView().findViewById(R.id.mediaImage);

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
    }

    private void verificarYMostrarBotonEliminar(Map<String, Object> post) {
        if (userId != null && userId.equals(post.get("uid").toString())) {
            deletePostButton.setVisibility(View.VISIBLE);
            deletePostButton.setOnClickListener(v -> eliminarPostYComentarios(post.get("$id").toString()));
        } else {
            deletePostButton.setVisibility(View.GONE);
        }
    }

    void cargarComentarios(String postId) {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID),
                    List.of(Query.Companion.equal("postId", postId)),
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al obtener los comentarios: " + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        List<comment> comments = new ArrayList<>();
                        for (Document<Map<String, Object>> doc : result.getDocuments()) {
                            String postIdComment = doc.getData().get("postId") != null ? doc.getData().get("postId").toString() : "";
                            String author = doc.getData().get("author") != null ? doc.getData().get("author").toString() : "";
                            String content = doc.getData().get("content") != null ? doc.getData().get("content").toString() : "";
                            String authorPhotoUrl = doc.getData().get("authorPhotoUrl") != null ? doc.getData().get("authorPhotoUrl").toString() : null;

                            comments.add(new comment(postIdComment, author, content, authorPhotoUrl));
                        }
                        mainHandler.post(() -> commentsAdapter.establecerLista(comments));
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    void eliminarPostYComentarios(String postId) {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // Primero, eliminar todos los comentarios asociados al post
        try {
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID),
                    List.of(Query.Companion.equal("postId", postId)),
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al obtener los comentarios: " + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        for (Document<Map<String, Object>> doc : result.getDocuments()) {
                            databases.deleteDocument(
                                    getString(R.string.APPWRITE_DATABASE_ID),
                                    getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID),
                                    doc.getId(),
                                    new CoroutineCallback<>((resultDeleteComment, errorDeleteComment) -> {
                                        if (errorDeleteComment != null) {
                                            Snackbar.make(requireView(), "Error al eliminar el comentario: " + errorDeleteComment.toString(), Snackbar.LENGTH_LONG).show();
                                        }
                                    })
                            );
                        }

                        // Luego, eliminar el post
                        databases.deleteDocument(
                                getString(R.string.APPWRITE_DATABASE_ID),
                                getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                                postId,
                                new CoroutineCallback<>((resultDeletePost, errorDeletePost) -> {
                                    if (errorDeletePost != null) {
                                        Snackbar.make(requireView(), "Error al eliminar el post: " + errorDeletePost.toString(), Snackbar.LENGTH_LONG).show();
                                    } else {
                                        mainHandler.post(() -> {
                                            Snackbar.make(requireView(), "Post y comentarios eliminados", Snackbar.LENGTH_SHORT).show();
                                            navController.popBackStack(); // Regresar a la pantalla anterior
                                        });
                                    }
                                })
                        );
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }
}