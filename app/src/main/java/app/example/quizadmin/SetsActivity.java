package app.example.quizadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.os.Bundle;
import android.util.ArrayMap;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static app.example.quizadmin.CategoryActivity.catList;
import static app.example.quizadmin.CategoryActivity.selectedCategoryIndex;

public class SetsActivity extends AppCompatActivity {

    private RecyclerView setsRecyclerView;
    private Button addSetB;
    public static List<String> setIDs = new ArrayList<>();
    private SetAdapter adapter;
    private FirebaseFirestore firestore;
    private Dialog loadingDialog;
    public static int selected_set_index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sets);

        Toolbar toolbar = findViewById(R.id.sa_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Sets");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setsRecyclerView = findViewById(R.id.sets_recycler);
        addSetB = findViewById(R.id.addSetB);

        addSetB.setText("Add New Set");

        loadingDialog = new Dialog(SetsActivity.this);
        loadingDialog.setContentView(R.layout.loading_progressbar);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawableResource(R.drawable.progress_background);
        loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);


        addSetB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNewSet();
            }
        });


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(layoutManager.VERTICAL);
        setsRecyclerView.setLayoutManager(layoutManager);

        firestore = FirebaseFirestore.getInstance();
        loadSets();

    }

    private void loadSets(){

        setIDs.clear();

        loadingDialog.show();

        firestore.collection("quiz").document(catList.get(selectedCategoryIndex).getId())
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                long noOfSets = (long)documentSnapshot.get("SETS");

                for (int i=1; i<=noOfSets; i++){
                    setIDs.add(documentSnapshot.getString("SET" + String.valueOf(i) + "_ID"));
                }

                catList.get(selectedCategoryIndex).setSetCounter(documentSnapshot.getString("COUNTER"));
                catList.get(selectedCategoryIndex).setNoOfSets(String.valueOf(noOfSets));

                adapter = new SetAdapter(setIDs);
                setsRecyclerView.setAdapter(adapter);

                loadingDialog.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Toast.makeText(SetsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();

            }
        });


    }

    private void addNewSet(){

        loadingDialog.show();

        final String currentCatId = catList.get(selectedCategoryIndex).getId();
        final String currentCounter = catList.get(selectedCategoryIndex).getSetCounter();
        Map<String, Object> qData = new ArrayMap<>();
        qData.put("COUNT","0");

        firestore.collection("quiz").document(currentCatId)
                .collection(currentCounter).document("QUESTIONS_LIST")
                .set(qData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        Map<String,Object> catDoc = new ArrayMap<>();
                        catDoc.put("COUNTER",String.valueOf(Integer.valueOf(currentCounter) + 1) );
                        catDoc.put("SET" + String.valueOf(setIDs.size() + 1) + "_ID",currentCounter);
                        catDoc.put("SETS", setIDs.size() + 1);

                        firestore.collection("quiz").document(currentCatId)
                                .update(catDoc)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        Toast.makeText(SetsActivity.this, "Set Added Successfully.", Toast.LENGTH_SHORT).show();

                                        setIDs.add(currentCounter);
                                        catList.get(selectedCategoryIndex).setNoOfSets(String.valueOf(setIDs.size()));
                                        catList.get(selectedCategoryIndex).setSetCounter(String.valueOf(Integer.valueOf(currentCounter) + 1));

                                        adapter.notifyItemInserted(setIDs.size());
                                        loadingDialog.dismiss();

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(SetsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                loadingDialog.dismiss();
                            }
                        });

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(SetsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == android.R.id.home){
            finish();
        }

        return super.onOptionsItemSelected(item);

    }
}