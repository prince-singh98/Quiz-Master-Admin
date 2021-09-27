package app.example.quizadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Dialog;
import android.os.Bundle;
import android.util.ArrayMap;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

import static app.example.quizadmin.CategoryActivity.catList;
import static app.example.quizadmin.CategoryActivity.selectedCategoryIndex;
import static app.example.quizadmin.QuestionsActivity.quesList;
import static app.example.quizadmin.SetsActivity.selected_set_index;
import static app.example.quizadmin.SetsActivity.setIDs;

public class QuestionDetailsActivity extends AppCompatActivity {

    private EditText question, optionA, optionB, optionC, optionD, answer;
    private Button addQuesB;
    private String qStr, aStr, bStr, cStr, dStr, ansStr;
    private Dialog loadingDialog;
    private FirebaseFirestore firestore;
    private String action;
    private int qID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_details);

        Toolbar toolbar = findViewById(R.id.q_details_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        question = findViewById(R.id.question);
        optionA = findViewById(R.id.optionA);
        optionB = findViewById(R.id.optionB);
        optionC = findViewById(R.id.optionC);
        optionD = findViewById(R.id.optionD);
        answer = findViewById(R.id.answer);
        addQuesB = findViewById(R.id.addQuesB);

        loadingDialog = new Dialog(QuestionDetailsActivity.this);
        loadingDialog.setContentView(R.layout.loading_progressbar);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawableResource(R.drawable.progress_background);
        loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        firestore = FirebaseFirestore.getInstance();

        action = getIntent().getStringExtra("ACTION");
        if (action.compareTo("EDIT") == 0){

            qID = getIntent().getIntExtra("Q_ID", 0);

            loadData(qID);

            getSupportActionBar().setTitle("QUESTION " + String.valueOf(qID + 1));
            addQuesB.setText("Update");

        }else {

            getSupportActionBar().setTitle("QUESTION " + String.valueOf(quesList.size() + 1));
            addQuesB.setText("Add");

        }

        addQuesB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qStr = question.getText().toString();
                aStr = optionA.getText().toString();
                bStr = optionB.getText().toString();
                cStr = optionC.getText().toString();
                dStr = optionD.getText().toString();
                ansStr = answer.getText().toString();

                if (qStr.isEmpty()){
                    question.setError("Enter Question");
                    return;
                }

                if (aStr.isEmpty()){
                    optionA.setError("Enter optionA");
                    return;
                }

                if (bStr.isEmpty()){
                    optionB.setError("Enter optionB");
                    return;
                }

                if (cStr.isEmpty()){
                    optionC.setError("Enter optionC");
                    return;
                }

                if (dStr.isEmpty()){
                    optionD.setError("Enter optionD");
                    return;
                }

                if (ansStr.isEmpty()){
                    answer.setError("Enter correct answer");
                    return;
                }

                if (action.compareTo("EDIT") == 0){
                    editQuestion();
                }else {
                    addNewQuestion();

                }
            }
        });
    }

    private void addNewQuestion(){
        loadingDialog.show();

        Map<String,Object> quesData = new ArrayMap<>();

        quesData.put("QUESTION", qStr);
        quesData.put("A", aStr);
        quesData.put("B", bStr);
        quesData.put("C", cStr);
        quesData.put("D", dStr);
        quesData.put("ANSWER", ansStr);

        final String docId = firestore.collection("quiz").document(catList.get(selectedCategoryIndex).getId())
                .collection(setIDs.get(selected_set_index)).document().getId();

        firestore.collection("quiz").document(catList.get(selectedCategoryIndex).getId())
                .collection(setIDs.get(selected_set_index)).document(docId)
                .set(quesData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        Map<String, Object> quesDoc = new ArrayMap<>();
                        quesDoc.put("Q" + String.valueOf(quesList.size() + 1) + "_ID", docId);
                        quesDoc.put("COUNT", String.valueOf(quesList.size() + 1));

                        firestore.collection("quiz").document(catList.get(selectedCategoryIndex).getId())
                                .collection(setIDs.get(selected_set_index)).document("QUESTIONS_LIST")
                                .update(quesDoc)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        Toast.makeText(QuestionDetailsActivity.this, "Question added successfully.", Toast.LENGTH_SHORT).show();

                                        quesList.add(new QuestionModel(
                                                docId,
                                                qStr,aStr,bStr,cStr,dStr,
                                                Integer.valueOf(ansStr)
                                        ));

                                        loadingDialog.dismiss();

                                        QuestionDetailsActivity.this.finish();//to go to previous activity after adding question.

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                                Toast.makeText(QuestionDetailsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                loadingDialog.dismiss();
                            }
                        });

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Toast.makeText(QuestionDetailsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();

            }
        });
    }

    private void loadData(int id){

        question.setText(quesList.get(id).getQuestion());
        optionA.setText(quesList.get(id).getOptionA());
        optionB.setText(quesList.get(id).getOptionB());
        optionC.setText(quesList.get(id).getOptionC());
        optionD.setText(quesList.get(id).getOptionD());
        answer.setText(String.valueOf(quesList.get(id).getCorrectAns()));

    }

    private void editQuestion(){
        loadingDialog.show();

        Map<String, Object> quesData = new ArrayMap<>();
        quesData.put("QUESTION", qStr);
        quesData.put("A", aStr);
        quesData.put("B", bStr);
        quesData.put("C", cStr);
        quesData.put("D", dStr);
        quesData.put("ANSWER", ansStr);


        firestore.collection("quiz").document(catList.get(selectedCategoryIndex).getId())
                .collection(setIDs.get(selected_set_index)).document(quesList.get(qID).getQuesID())
                .set(quesData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        Toast.makeText(QuestionDetailsActivity.this, "Question updated successfully", Toast.LENGTH_SHORT).show();

                        //updating our global list
                        quesList.get(qID).setQuestion(qStr);
                        quesList.get(qID).setOptionA(aStr);
                        quesList.get(qID).setOptionB(bStr);
                        quesList.get(qID).setOptionC(cStr);
                        quesList.get(qID).setOptionD(dStr);
                        quesList.get(qID).setCorrectAns(Integer.valueOf(ansStr));

                        loadingDialog.dismiss();
                        QuestionDetailsActivity.this.finish();

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Toast.makeText(QuestionDetailsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
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