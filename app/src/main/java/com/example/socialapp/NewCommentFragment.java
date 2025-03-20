package com.example.socialapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;

public class NewCommentFragment extends Fragment {

    NavController navController;
    EditText commentEditText;
    Button publishCommentButton;
    Client client;
    Account account;
    AppViewModel appViewModel;
    String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_comment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        commentEditText = view.findViewById(R.id.commentEditText);
        publishCommentButton = view.findViewById(R.id.publishCommentButton);

        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));
        account = new Account(client);
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        // Obtener el usuario actual
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }
                mainHandler.post(() -> {
                    userId = result.getId();
                    String author = result.getName(); // Nombre del usuario
                    String authorPhotoUrl = null; // Puedes cambiar esto si tienes una URL de foto de perfil

                    publishCommentButton.setOnClickListener(v -> publicarComentario(author, authorPhotoUrl));
                });
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    private void publicarComentario(String author, String authorPhotoUrl) {
        String commentContent = commentEditText.getText().toString();
        if (commentContent.isEmpty()) {
            Snackbar.make(requireView(), "El comentario no puede estar vacío", Snackbar.LENGTH_SHORT).show();
            return;
        }

        appViewModel.postSeleccionado.observe(getViewLifecycleOwner(), post -> {
            if (post != null) {
                String postId = post.get("$id").toString();

                Databases databases = new Databases(client);
                Map<String, Object> data = new HashMap<>();
                data.put("postId", postId);
                data.put("author", author);
                data.put("content", commentContent);
                data.put("authorPhotoUrl", authorPhotoUrl);

                try {
                    databases.createDocument(
                            getString(R.string.APPWRITE_DATABASE_ID),
                            getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID),
                            "unique()", // ID único generado automáticamente
                            data,
                            new ArrayList<>(), // Permisos (opcional)
                            new CoroutineCallback<>((result, error) -> {
                                if (error != null) {
                                    error.printStackTrace();
                                    Snackbar.make(requireView(), "Error al publicar el comentario: " + error.toString(), Snackbar.LENGTH_LONG).show();
                                } else {
                                    // Ejecutar en el hilo principal
                                    requireActivity().runOnUiThread(() -> {
                                        Snackbar.make(requireView(), "Comentario publicado", Snackbar.LENGTH_SHORT).show();
                                        navController.popBackStack(); // Volver al fragmento anterior
                                    });
                                }
                            })
                    );
                } catch (AppwriteException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });
    }
}