package app.example.quizadmin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static app.example.quizadmin.CategoryActivity.catList;
import static app.example.quizadmin.CategoryActivity.selectedCategoryIndex;
import static app.example.quizadmin.SetsActivity.selected_set_index;

public class SetAdapter extends RecyclerView.Adapter<SetAdapter.ViewHolder> {

    private List<String> setIDs;

    public SetAdapter(List<String> setIDs) {
        this.setIDs = setIDs;
    }

    @NonNull
    @Override
    public SetAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cat_item_layout,viewGroup,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SetAdapter.ViewHolder viewHolder, int i) {

        String setId = setIDs.get(i);

        viewHolder.setData(i, setId, this);
    }

    @Override
    public int getItemCount() {
        return setIDs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView setName;
        private ImageView delSetB;
        private Dialog loadingDialog;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            setName = itemView.findViewById(R.id.catName);
            delSetB = itemView.findViewById(R.id.catDelB);

            loadingDialog = new Dialog(itemView.getContext());
            loadingDialog.setContentView(R.layout.loading_progressbar);
            loadingDialog.setCancelable(false);
            loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        }

        private void setData(final int pos, final String setId, final SetAdapter adapter){

            setName.setText("SET " + String.valueOf(pos + 1));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selected_set_index = pos;
                    Intent intent = new Intent(itemView.getContext(),QuestionsActivity.class);
                    itemView.getContext().startActivity(intent);
                }
            });

            delSetB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    AlertDialog dialog = new AlertDialog.Builder(itemView.getContext())
                            .setTitle("Delete Set")
                            .setMessage("Do you want to delete this set?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    deleteSet(pos, setId, itemView.getContext(), adapter);

                                }
                            }).setNegativeButton("Cancel", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();

                    dialog.getButton(dialog.BUTTON_POSITIVE).setBackgroundColor(Color.RED);
                    dialog.getButton(dialog.BUTTON_NEGATIVE).setBackgroundColor(Color.RED);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0,0,50,0);
                    dialog.getButton(dialog.BUTTON_NEGATIVE).setLayoutParams(params);


                }
            });

        }

        private void deleteSet(final int pos, final String setId, final Context context, final SetAdapter adapter){
            loadingDialog.show();

            final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

            firestore.collection("quiz").document(catList.get(selectedCategoryIndex).getId())
                    .collection(setId).get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                            //WriteBatch deletes all the documents in a collection at a time instead of one by one...
                            WriteBatch batch = firestore.batch();

                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots){

                                batch.delete(doc.getReference());
                            }


                            batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    Map<String,Object> catDoc = new ArrayMap<>();

                                    int index = 1;
                                    for (int i=0; i<setIDs.size(); i++){
                                        if (i != pos){
                                            catDoc.put("SET" + String.valueOf(index) + "_ID", setIDs.get(i));
                                            index++;
                                        }
                                    }
                                    catDoc.put("SETS", index - 1);

                                    firestore.collection("quiz").document(catList.get(selectedCategoryIndex).getId())
                                            .update(catDoc)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {

                                                    Toast.makeText(context, "Set deleted successfully", Toast.LENGTH_SHORT).show();

                                                    SetsActivity.setIDs.remove(pos);

                                                    catList.get(selectedCategoryIndex).setNoOfSets(String.valueOf(SetsActivity.setIDs.size()));

                                                    adapter.notifyDataSetChanged();

                                                    loadingDialog.dismiss();

                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                            Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            loadingDialog.dismiss();

                                        }
                                    });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    loadingDialog.dismiss();
                                }
                            });



                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadingDialog.dismiss();
                }
            });
        }
    }
}
