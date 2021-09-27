package app.example.quizadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.os.Bundle;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CategoryActivity extends AppCompatActivity {

    private RecyclerView cat_recycler_view;
    private Button addCatB;
    public static List<CategoryModel> catList = new ArrayList<>();
    public static int selectedCategoryIndex = 0;
    private FirebaseFirestore firestore;
    private Dialog loadingDialog, addCatDialog;
    private EditText dialogCatName;
    private CategoryAdapter adapter;
    private Button dialogAddB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Categories");

        cat_recycler_view = findViewById(R.id.cat_recycler);
        addCatB = findViewById(R.id.addCatB);

        loadingDialog = new Dialog(CategoryActivity.this);
        loadingDialog.setContentView(R.layout.loading_progressbar);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawableResource(R.drawable.progress_background);
        loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);


        addCatDialog = new Dialog(CategoryActivity.this);
        addCatDialog.setContentView(R.layout.add_category_dialog);
        addCatDialog.setCancelable(true);
        addCatDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        dialogCatName = addCatDialog.findViewById(R.id.ac_cat_name);
        dialogAddB = addCatDialog.findViewById(R.id.ac_add_btn);

        firestore = FirebaseFirestore.getInstance();

        addCatB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogCatName.getText().clear();
                addCatDialog.show();

            }
        });

        dialogAddB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogCatName.getText().toString().isEmpty()){
                    dialogCatName.setError("Enter Category Name.");
                    return;
                }
                addNewCategory(dialogCatName.getText().toString());
            }
        });


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        cat_recycler_view.setLayoutManager(layoutManager);

        loadData();
    }

    private void loadData() {
        loadingDialog.show();

        catList.clear();//to clear already available data if present on start

        firestore.collection("quiz").document("Categories")
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()){
                    //categories document
                    DocumentSnapshot doc = task.getResult();

                    if (doc.exists()){
                        long count = (long)doc.get("COUNT");

                        for (int i=1; i<=count; i++){
                            String catName = doc.getString("CAT" + String.valueOf(i) + "_NAME");
                            String catId = doc.getString("CAT" + String.valueOf(i) + "_ID");

                            catList.add(new CategoryModel(catId,catName,"0","1"));
                        }
                        adapter = new CategoryAdapter(catList);
                        cat_recycler_view.setAdapter(adapter);

                    }else{
                        //that means no category available
                        Toast.makeText(CategoryActivity.this, "No Category, Document exists.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }else{
                    Toast.makeText(CategoryActivity.this, ""+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
                loadingDialog.dismiss();
            }
        });
    }

    private void addNewCategory(final String title) {
        addCatDialog.dismiss();
        loadingDialog.show();

        Map<String,Object> catData = new ArrayMap<>();
        catData.put("NAME",title);
        catData.put("SETS",0);
        catData.put("COUNTER", "1");

        final String docId = firestore.collection("quiz").document().getId();
        firestore.collection("quiz").document(docId)
                .set(catData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        Map<String,Object> catDoc = new ArrayMap<>();
                        catDoc.put("CAT" + String.valueOf(catList.size() + 1) + "_NAME",title);
                        catDoc.put("CAT" + String.valueOf(catList.size() + 1) + "_ID",docId);
                        catDoc.put("COUNT", catList.size() + 1);

                        firestore.collection("quiz").document("Categories")
                                .update(catDoc)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        Toast.makeText(CategoryActivity.this, "Category Added Successfully", Toast.LENGTH_SHORT).show();

                                        //to add to our localList after adding new category
                                        catList.add(new CategoryModel(docId,title,"0","1"));

                                        adapter.notifyItemInserted(catList.size());

                                        loadingDialog.dismiss();

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(CategoryActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                loadingDialog.dismiss();

                            }
                        });

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Toast.makeText(CategoryActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
            }
        });

    }



}
