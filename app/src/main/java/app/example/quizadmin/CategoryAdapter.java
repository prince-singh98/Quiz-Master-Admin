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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<CategoryModel> cat_list;

    public CategoryAdapter(List<CategoryModel> cat_list) {
        this.cat_list = cat_list;
    }

    @NonNull
    @Override
    public CategoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cat_item_layout,viewGroup,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryAdapter.ViewHolder viewHolder, int pos) {

        String title = cat_list.get(pos).getName();

        viewHolder.setData(title, pos, this);

    }

    @Override
    public int getItemCount() {
        return cat_list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView catName;
        private ImageView deleteB;
        private Dialog loadingDialog;
        private Dialog editDialog;
        private EditText tv_editCatName;
        private Button updateCatB;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            catName = itemView.findViewById(R.id.catName);
            deleteB = itemView.findViewById(R.id.catDelB);

            loadingDialog = new Dialog(itemView.getContext());
            loadingDialog.setContentView(R.layout.loading_progressbar);
            loadingDialog.setCancelable(false);
            loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

            editDialog = new Dialog(itemView.getContext());
            editDialog.setContentView(R.layout.edit_category_dialog);
            editDialog.setCancelable(true);
            editDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

            tv_editCatName = editDialog.findViewById(R.id.ec_cat_name);
            updateCatB = editDialog.findViewById(R.id.ec_add_btn);
        }

        private void setData(String title, final int pos, final CategoryAdapter adapter){
            catName.setText(title);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CategoryActivity.selectedCategoryIndex = pos;
                    Intent intent = new Intent(itemView.getContext(),SetsActivity.class);
                    itemView.getContext().startActivity(intent);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    tv_editCatName.setText(cat_list.get(pos).getName());
                    editDialog.show();

                    return false;
                }
            });

            updateCatB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (tv_editCatName.getText().toString().isEmpty()){
                        tv_editCatName.setError("Enter Category Name.");
                        return;
                    }
                    updateCategory(tv_editCatName.getText().toString(), pos,itemView.getContext(), adapter);
                }
            });

            deleteB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    AlertDialog dialog = new AlertDialog.Builder(itemView.getContext())
                            .setTitle("Delete Category")
                            .setMessage("Do you want to delete this category")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    deleteCategory(pos, itemView.getContext(), adapter);
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
        private void deleteCategory(final int id, final Context context, final CategoryAdapter adapter){

            loadingDialog.show();

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();

            Map<String, Object> catDoc = new ArrayMap<>();

            int index = 1;

            for (int i=0; i<cat_list.size(); i++){

                //if the id is no equal to category id that we want to delete then only keep it in catDoc...
                if (i != id){
                    catDoc.put("CAT" + String.valueOf(index) + "_ID", cat_list.get(i).getId());
                    catDoc.put("CAT" + String.valueOf(index) + "_NAME", cat_list.get(i).getName());
                    index++;
                }

            }

            catDoc.put("COUNT", index - 1);

            firestore.collection("quiz").document("Categories")
                    .set(catDoc)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            Toast.makeText(context, "Category deleted successfully", Toast.LENGTH_SHORT).show();

                            CategoryActivity.catList.remove(id);//to delete the item from localList...
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

        private void updateCategory(final String catNewName, final int pos, final Context context, final CategoryAdapter adapter){
            editDialog.dismiss();
            loadingDialog.show();

            Map<String, Object> catData = new ArrayMap<>();
            catData.put("NAME",catNewName);

            final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

            firestore.collection("quiz").document(cat_list.get(pos).getId())
                    .update(catData)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            Map<String,Object> catDoc = new ArrayMap<>();
                            catDoc.put("CAT" + String.valueOf(pos + 1) + "_NAME",catNewName);

                            firestore.collection("quiz").document("Categories")
                                    .update(catDoc)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {

                                            Toast.makeText(context, "Category Name Changed Successfully.", Toast.LENGTH_SHORT).show();
                                            CategoryActivity.catList.get(pos).setName(catNewName);
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

                }
            });
        }
    }
}