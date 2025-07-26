package com.example.healthcare;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthcare.adapters.ChatAdapter;
import com.example.healthcare.models.ChatMessage;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private static final String API_KEY = "AIzaSyDbV-CsWt9_NUEECgADBGgJ3LG0QR8uOeE";
    private static final String MODEL_NAME = "gemini-1.5-flash";

    private TextInputEditText messageInput;
    private MaterialButton sendButton;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private GenerativeModelFutures model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("AI Workout Assistant");

        // Initialize views
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        // Initialize Gemini with configuration
        GenerativeModel generativeModel = new GenerativeModel(MODEL_NAME, API_KEY);
        model = GenerativeModelFutures.from(generativeModel);

        // Setup RecyclerView
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Add welcome message
        messages.add(new ChatMessage("Hello! I'm your AI workout assistant. How can I help you today?", false));
        chatAdapter.notifyItemInserted(messages.size() - 1);

        // Setup send button
        sendButton.setOnClickListener(v -> {
            Log.d(TAG, "Send button clicked");
            sendMessage();
        });

        // Setup enter key listener
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            Log.d(TAG, "Enter key pressed");
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) {
            Log.d(TAG, "Message is empty, not sending");
            return;
        }

        Log.d(TAG, "Sending message: " + message);

        // Add user message
        messages.add(new ChatMessage(message, true));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messageInput.setText("");

        // Scroll to bottom
        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);

        // Get AI response
        Content content = new Content.Builder()
            .addText(message)
            .build();
            
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Executor executor = Executors.newSingleThreadExecutor();

        Futures.addCallback(
            response,
            new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String responseText = result.getText();
                    Log.d(TAG, "Received response: " + responseText);
                    runOnUiThread(() -> {
                        messages.add(new ChatMessage(responseText, false));
                        chatAdapter.notifyItemInserted(messages.size() - 1);
                        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Error getting response", t);
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        // Add error message to chat
                        messages.add(new ChatMessage("Sorry, I encountered an error. Please try again.", false));
                        chatAdapter.notifyItemInserted(messages.size() - 1);
                        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                    });
                }
            },
            executor
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 