package com.chenhao.bingo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chenhao.bingo.databinding.ActivityBingoBinding;
import com.chenhao.bingo.databinding.SingleButtonBinding;
import com.firebase.ui.common.ChangeEventType;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BingoActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int STATUS_INIT = 0;
    public static final int STATUS_CREATED = 1;
    public static final int STATUS_JOINED = 2;
    public static final int STATUS_CREATORS_TURN = 3;
    public static final int STATUS_JOINERS_TURN = 4;
    public static final int STATUS_CREATORS_BINGO = 5;
    public static final int STATUS_JOINERS_BINGO = 6;

    private static final String TAG = BingoActivity.class.getSimpleName().toString();
    private ActivityBingoBinding binding;
    private String roomId;
    private boolean isCreator;
    private FirebaseRecyclerAdapter<Boolean, NumberHolder> adapter;
    boolean myTurn =  false;
    ValueEventListener statusListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            long status = (long)snapshot.getValue();
            switch ((int) status){
                case STATUS_CREATED:
                    binding.info.setText("等待對手進入");
                    break;
                case STATUS_JOINED:
                    binding.info.setText("YA! 對手機加入了");
                    FirebaseDatabase.getInstance().getReference("rooms")
                            .child(roomId)
                            .child("status")
                            .setValue(STATUS_CREATORS_TURN);
                    break;
                case STATUS_CREATORS_TURN:
                    setMyTurn(isCreator);
                    break;
                case STATUS_JOINERS_TURN:
                    setMyTurn(!isCreator);
                    break;
                case STATUS_CREATORS_BINGO:
                    new AlertDialog.Builder(BingoActivity.this)
                            .setTitle("BINGO!")
                            .setMessage(isCreator? "恭喜你,賓果了!":"對方賓果了!")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    endGame();
                                }
                            }).show();
                    break;
                case STATUS_JOINERS_BINGO:
                    new AlertDialog.Builder(BingoActivity.this)
                            .setTitle("BINGO!")
                            .setMessage(!isCreator? "恭喜你,賓果了!":"對方賓果了!")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    endGame();

                                }
                            }).show();
                    break;




            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };

    private void endGame() {
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId)
                .child("status")
                .removeEventListener(statusListener);
        if (isCreator) {
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .removeValue();
        }
        finish();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBingoBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        roomId = getIntent().getStringExtra("ROOM_ID");
        isCreator = getIntent().getBooleanExtra("IS_CREATOR",false);
        Log.d(TAG, "onCreate: " + roomId + "/" + isCreator);

        if(isCreator) {
            for(int i = 0; i < 25 ; i++) {
                FirebaseDatabase.getInstance().getReference("rooms")
                        .child(roomId)
                        .child("numbers")
                        .child(String.valueOf(i + 1))
                        .setValue(false);
            }

            //change room status
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .child("status")
                    .setValue(STATUS_CREATED);

        } else {
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .child("status")
                    .setValue(STATUS_JOINED);
        }

        //map for number to position
        final Map<Integer,Integer> numberMap = new HashMap<>();
        final List<NumberButton> buttons = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            NumberButton button  = new NumberButton(this);
            button.setNumber(i+1);
            buttons.add(button);
        }
        Collections.shuffle(buttons);

        for (int i = 0; i < 25; i++) {
            numberMap.put(buttons.get(i).number,i);
        }



        //RecyclerView Adapter
        Query query = FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId)
                .child("numbers")
                .orderByKey();
        FirebaseRecyclerOptions<Boolean> options = new FirebaseRecyclerOptions.Builder<Boolean>()
                .setQuery(query,Boolean.class)
                .build();
        adapter = new FirebaseRecyclerAdapter<Boolean, NumberHolder>(options){
            @NonNull
            @Override
            public NumberHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                SingleButtonBinding itemBinding = SingleButtonBinding .inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new NumberHolder(itemBinding);
            }

            @Override
            protected void onBindViewHolder(@NonNull NumberHolder holder, int position, @NonNull Boolean model) {
                holder.button.setText(String.valueOf(buttons.get(position).getNumber()));
                holder.button.setNumber(buttons.get(position).getNumber());
                holder.button.setEnabled(!buttons.get(position).isPicked());
                holder.button.setOnClickListener(BingoActivity.this);

            }

            @Override
            public void onChildChanged(@NonNull ChangeEventType type, @NonNull DataSnapshot snapshot, int newIndex, int oldIndex) {
                super.onChildChanged(type, snapshot, newIndex, oldIndex);
                Log.d(TAG, "onChildChanged: " + type + "/" + snapshot.getKey() + "/" + snapshot.getValue());
                if(type == ChangeEventType.CHANGED) {
                    int number = Integer.parseInt(snapshot.getKey());
                    boolean picked = (boolean) snapshot.getValue();
                    int pos = numberMap.get(number);
                    buttons.get(pos).setPicked(picked);
                    NumberHolder numberHolder = (NumberHolder) binding.recyclerBingo.findViewHolderForAdapterPosition(pos);
                    numberHolder.button.setEnabled(!picked);

                    //bingo
                    int[] nums = new int[25];
                    for(int i = 0; i < 25; i++) {
                        nums[i] = buttons.get(i).isPicked() ? 1 : 0;
                    }
                    int bingo = 0;
                    for (int i = 0; i < 5; i++) {
                        int sum = 0;
                        for (int j = 0; j < 5; j++) {
                            sum += nums[i*5 + j];
                        }
                        bingo += (sum == 5) ? 1 : 0;
                        sum = 0;
                        for (int j = 0; j < 5; j++) {
                            sum += nums[j*5 + i];
                        }

                        bingo += (sum == 5) ? 1 : 0;
                    }
                    Log.d(TAG, "onChildChanged: " + bingo);
                    if(bingo > 0) {
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomId)
                                .child("status")
                                .setValue(isCreator ? STATUS_CREATORS_BINGO : STATUS_JOINERS_BINGO);
                    }
                }
            }
        };

        setViews();

    }

    private void setViews() {
        binding.recyclerBingo.setHasFixedSize(true);
        binding.recyclerBingo.setLayoutManager(new GridLayoutManager(this,5));
        binding.recyclerBingo.setAdapter(adapter);

    }

    @Override
    public void onClick(View v) {
        if(myTurn) {
            int number = ((NumberButton)v).getNumber();
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .child("numbers")
                    .child(String.valueOf(number))
                    .setValue(true);
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .child("status")
                    .setValue(isCreator ? STATUS_JOINERS_TURN : STATUS_CREATORS_TURN );
        }

    }


    class NumberHolder extends RecyclerView.ViewHolder {
        NumberButton button ;
        public NumberHolder(@NonNull SingleButtonBinding binding) {
            super(binding.getRoot());
            button = binding.button;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId)
                .child("status")
                .addValueEventListener(statusListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId)
                .child("status")
                .removeEventListener(statusListener);
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
        binding.info.setText(myTurn ? "請選號":"等待對手選號");
    }
}