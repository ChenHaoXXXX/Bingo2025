package com.chenhao.bingo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chenhao.bingo.databinding.ActivityMainBinding;
import com.chenhao.bingo.databinding.RowRoomBinding;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener, View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    private FirebaseAuth auth;
    int[] avatarIds = {R.drawable.avatar_0,R.drawable.avatar_1,R.drawable.avatar_2,R.drawable.avatar_3,R.drawable.avatar_4,R.drawable.avatar_5,R.drawable.avatar_6};
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                @Override
                public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                    onSignInResult(result);
                }
            }
    );
    private Member member;
    private FirebaseRecyclerAdapter<GameRoom, RoomHolder> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
          //  Insets stateBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();


    }

    private void setView() {
        binding.nickname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNickDialog(binding.nickname.getText().toString());
            }
        });

        binding.groupAvatars.setVisibility(View.GONE);
        binding.avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.groupAvatars.setVisibility(
                        binding.groupAvatars.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            }
        });

        binding.avatar0.setOnClickListener(this);
        binding.avatar1.setOnClickListener(this);
        binding.avatar2.setOnClickListener(this);
        binding.avatar3.setOnClickListener(this);
        binding.avatar4.setOnClickListener(this);
        binding.avatar5.setOnClickListener(this);
        binding.avatar6.setOnClickListener(this);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = new EditText(MainActivity.this);
                editText.setText("Welcome");
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Game Room")
                        .setMessage("Please enter your room title ")
                        .setView(editText)
                        .setPositiveButton("OK", (dialog, which) -> {
                            String roomTitle = editText.getText().toString();
                            GameRoom room = new GameRoom(roomTitle,member);
                            FirebaseDatabase.getInstance()
                                    .getReference("rooms")
                                    .push().setValue(room, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                            if(error == null) {
                                                String roomId = ref.getKey();
                                                Log.d(TAG, "onComplete: " + roomId);
                                                FirebaseDatabase.getInstance().getReference("rooms")
                                                        .child(roomId)
                                                        .child("id")
                                                        .setValue(roomId);
                                                Intent intent = new Intent(MainActivity.this,BingoActivity.class);
                                                intent.putExtra("ROOM_ID",roomId);
                                                intent.putExtra("IS_CREATOR",true);
                                                startActivity(intent);
                                            }

                                        }
                                    });
                        })
                        .setNegativeButton("Cancel",null)
                        .show();
            }
        });

        //Recycler
        binding.recyclerRoom.setHasFixedSize(true);
        binding.recyclerRoom.setLayoutManager(new LinearLayoutManager(this));

        Query query = FirebaseDatabase.getInstance()
                .getReference("rooms")
                .limitToLast(30);
        FirebaseRecyclerOptions<GameRoom> options =
                new FirebaseRecyclerOptions.Builder<GameRoom>()
                        .setQuery(query,GameRoom.class)
                        .build();
        adapter = new FirebaseRecyclerAdapter<GameRoom, RoomHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RoomHolder holder, int position, @NonNull GameRoom model) {
                holder.imgRoom.setImageResource(avatarIds[model.init.avatarId]);
                holder.textTitle.setText(model.title);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this,BingoActivity.class);
                        intent.putExtra("ROOM_ID",model.id);
                        startActivity(intent);
                    }
                });

            }

            @NonNull
            @Override
            public RoomHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                RowRoomBinding itemBinding = RowRoomBinding .inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new RoomHolder(itemBinding);
            }
        };

        binding.recyclerRoom.setAdapter(adapter);


    }

    public class RoomHolder extends RecyclerView.ViewHolder {
        private ImageView imgRoom;
        private TextView textTitle;
        public RoomHolder(@NonNull RowRoomBinding binding) {
            super(binding.getRoot());
            imgRoom = binding.roomImage;
            textTitle = binding.roomTitle;

        }
    }



    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(this);
       // adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseAuth.getInstance().removeAuthStateListener(this);
        if(adapter != null) {
            adapter.stopListening();
        }

    }


    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
        FirebaseUser user = auth.getCurrentUser();
        if (user== null) {
            // Create and launch sign-in intent
            Intent signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                    .setIsSmartLockEnabled(false)
                    .build();
            signInLauncher.launch(signInIntent);
        } else {
            setView();
            adapter.startListening();
            Log.d(TAG, "onAuthStateChanged: "  +  auth.getCurrentUser().getEmail() + "/" + auth.getCurrentUser().getUid());
            String displayName = user.getDisplayName();
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("displayName")
                    .setValue(displayName);

            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("uid")
                    .setValue(auth.getUid());

            FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getUid())
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            member = snapshot.getValue(Member.class);
                            if(member != null) {
                                if(member.nickname != null) {
                                    binding.nickname.setText(member.nickname);
                                }else {
                                    setNickDialog(displayName);
                                    binding.nickname.setText(snapshot.getKey());
                                }

                                binding.avatar.setImageResource(avatarIds[member.avatarId]);
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


//            FirebaseDatabase.getInstance()
//                    .getReference("users")
//                    .child(user.getUid())
//                    .child("nickname")
//                    .addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            if(snapshot.getValue() != null) {
//                                String nickname = snapshot.getKey();
//                            }else {
//                                setNickDialog(displayName);
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//
//                        }
//                    });
        }

    }

    private void setNickDialog(String displayName) {
        EditText editText = new EditText(this);
        editText.setText(displayName);
        new AlertDialog.Builder(this)
                .setTitle("Your nickname")
                .setMessage("Please enter your nickname ")
                .setView(editText)
                .setPositiveButton("OK", (dialog, which) -> {
                    String nickname = editText.getText().toString();
                    FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(auth.getUid())
                            .child("nickname")
                            .setValue(nickname);
                }).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_menu_signout) {
            FirebaseAuth.getInstance().signOut();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }




    @Override
    public void onClick(View view) {
        if(view instanceof ImageView) {
            int selectId = 0;
            int viewId = view.getId();
            if(viewId == binding.avatar1.getId()) {
                selectId = 1;
            } else if(viewId == binding.avatar2.getId()){
                selectId = 2;
            }else if(viewId == binding.avatar3.getId()){
                selectId = 3;
            }else if(viewId == binding.avatar4.getId()){
                selectId = 4;
            }else if(viewId == binding.avatar5.getId()){
                selectId = 5;
            }else if(viewId == binding.avatar6.getId()){
                selectId = 6;
            }


            binding.groupAvatars.setVisibility(View.GONE);
            FirebaseDatabase.getInstance().getReference("users")
                    .child(auth.getUid())
                    .child("avatarId")
                    .setValue(selectId);

        }

    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            // Successfully signed in
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            // ...
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
        }
    }
}