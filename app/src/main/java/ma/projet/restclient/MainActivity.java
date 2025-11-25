package ma.projet.restclient;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ma.projet.restclient.adapter.CompteAdapter;
import ma.projet.restclient.entities.Compte;
import ma.projet.restclient.repository.CompteRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements CompteAdapter.OnDeleteClickListener, CompteAdapter.OnUpdateClickListener {

    private RecyclerView recyclerView;
    private CompteAdapter adapter;
    private RadioGroup formatGroup;
    private FloatingActionButton fabAdd;

    // Par défaut, on commence en JSON
    private String currentFormat = "JSON";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        setupFormatSelection();
        setupAddButton();

        // Chargement initial des données
        loadData(currentFormat);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        formatGroup = findViewById(R.id.formatGroup);
        fabAdd = findViewById(R.id.fabAdd);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // On passe "this" car MainActivity implémente les interfaces de clic
        adapter = new CompteAdapter(this, this);
        recyclerView.setAdapter(adapter);
    }

    private void setupFormatSelection() {
        formatGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // Mise à jour du format selon le bouton coché
            currentFormat = (checkedId == R.id.radioJson) ? "JSON" : "XML";
            Toast.makeText(this, "Format changé en : " + currentFormat, Toast.LENGTH_SHORT).show();
            loadData(currentFormat);
        });
    }

    private void setupAddButton() {
        fabAdd.setOnClickListener(v -> showAddCompteDialog());
    }

    private void loadData(String format) {
        CompteRepository repository = new CompteRepository(format);
        repository.getAllCompte(new Callback<List<Compte>>() {
            @Override
            public void onResponse(Call<List<Compte>> call, Response<List<Compte>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Compte> comptes = response.body();
                    // Mise à jour de l'interface sur le Thread principal
                    runOnUiThread(() -> adapter.updateData(comptes));
                } else {
                    showToast("Erreur lors du chargement des données");
                }
            }

            @Override
            public void onFailure(Call<List<Compte>> call, Throwable t) {
                showToast("Erreur réseau : " + t.getMessage());
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }

    // --- Gestion de l'AJOUT ---

    private void showAddCompteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_compte, null);

        EditText etSolde = view.findViewById(R.id.etSolde);
        RadioGroup typeGroup = view.findViewById(R.id.typeGroup);

        builder.setView(view)
                .setTitle("Ajouter un compte")
                .setPositiveButton("Ajouter", (dialog, which) -> {
                    String soldeStr = etSolde.getText().toString();
                    if(soldeStr.isEmpty()) return; // Validation basique

                    double solde = Double.parseDouble(soldeStr);
                    String type = (typeGroup.getCheckedRadioButtonId() == R.id.radioCourant) ? "COURANT" : "EPARGNE";

                    Compte nouveauCompte = new Compte(null, solde, type, getCurrentDateFormatted());
                    addCompte(nouveauCompte);
                })
                .setNegativeButton("Annuler", null);

        builder.create().show();
    }

    private void addCompte(Compte compte) {
        // Pour l'écriture, on utilise souvent JSON par défaut ou le format courant
        CompteRepository repository = new CompteRepository(currentFormat);
        repository.addCompte(compte, new Callback<Compte>() {
            @Override
            public void onResponse(Call<Compte> call, Response<Compte> response) {
                if (response.isSuccessful()) {
                    showToast("Compte ajouté avec succès !");
                    loadData(currentFormat); // Recharger la liste
                }
            }

            @Override
            public void onFailure(Call<Compte> call, Throwable t) {
                showToast("Erreur lors de l'ajout");
            }
        });
    }

    // --- Gestion de la MODIFICATION ---

    @Override
    public void onUpdateClick(Compte compte) {
        showUpdateCompteDialog(compte);
    }

    private void showUpdateCompteDialog(Compte compte) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_compte, null);

        EditText etSolde = view.findViewById(R.id.etSolde);
        RadioGroup typeGroup = view.findViewById(R.id.typeGroup);

        // Pré-remplir les champs
        etSolde.setText(String.valueOf(compte.getSolde()));
        if ("COURANT".equalsIgnoreCase(compte.getType())) {
            typeGroup.check(R.id.radioCourant);
        } else {
            typeGroup.check(R.id.radioEpargne);
        }

        builder.setView(view)
                .setTitle("Modifier le compte " + compte.getId())
                .setPositiveButton("Modifier", (dialog, which) -> {
                    String soldeStr = etSolde.getText().toString();
                    if(soldeStr.isEmpty()) return;

                    compte.setSolde(Double.parseDouble(soldeStr));
                    compte.setType((typeGroup.getCheckedRadioButtonId() == R.id.radioCourant) ? "COURANT" : "EPARGNE");

                    updateCompte(compte);
                })
                .setNegativeButton("Annuler", null);

        builder.create().show();
    }

    private void updateCompte(Compte compte) {
        CompteRepository repository = new CompteRepository(currentFormat);
        repository.updateCompte(compte.getId(), compte, new Callback<Compte>() {
            @Override
            public void onResponse(Call<Compte> call, Response<Compte> response) {
                if (response.isSuccessful()) {
                    showToast("Compte modifié !");
                    loadData(currentFormat);
                }
            }

            @Override
            public void onFailure(Call<Compte> call, Throwable t) {
                showToast("Erreur modification");
            }
        });
    }

    // --- Gestion de la SUPPRESSION ---

    @Override
    public void onDeleteClick(Compte compte) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("Voulez-vous vraiment supprimer le compte ID " + compte.getId() + " ?")
                .setPositiveButton("Oui", (dialog, which) -> deleteCompte(compte))
                .setNegativeButton("Non", null)
                .show();
    }

    private void deleteCompte(Compte compte) {
        CompteRepository repository = new CompteRepository(currentFormat);
        repository.deleteCompte(compte.getId(), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showToast("Compte supprimé");
                    loadData(currentFormat);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showToast("Erreur suppression");
            }
        });
    }

    // --- Utilitaires ---

    private String getCurrentDateFormatted() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}