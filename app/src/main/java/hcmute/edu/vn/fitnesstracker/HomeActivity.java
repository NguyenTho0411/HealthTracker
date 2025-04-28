package hcmute.edu.vn.fitnesstracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import hcmute.edu.vn.fitnesstracker.buymedicine.BuyMedicineActivity;
import hcmute.edu.vn.fitnesstracker.eventmedia.EventMediaActivity;
import hcmute.edu.vn.fitnesstracker.findDoctor.Find_Doctor_Activity;
import hcmute.edu.vn.fitnesstracker.healthArticles.HealthArticlesActivity;
import hcmute.edu.vn.fitnesstracker.labtest.LabTestActivity;
import hcmute.edu.vn.fitnesstracker.medicalrecords.MedicalRecordsActivity;

public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SharedPreferences sharedPreferences = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");
        if (!username.isEmpty()) {
            Toast.makeText(this, "Welcome " + username, Toast.LENGTH_SHORT).show();
        }

        findViewById(R.id.card_exit).setOnClickListener(v -> {
            sharedPreferences.edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class));
        });

        findViewById(R.id.cardfind_doctor).setOnClickListener(v -> startActivity(new Intent(this, Find_Doctor_Activity.class)));
        findViewById(R.id.cardtest).setOnClickListener(v -> startActivity(new Intent(this, LabTestActivity.class)));
        findViewById(R.id.card_order_detail).setOnClickListener(v -> startActivity(new Intent(this, OrderDetailActivity.class)));
        findViewById(R.id.cardbuymedicine).setOnClickListener(v -> startActivity(new Intent(this, BuyMedicineActivity.class)));
        findViewById(R.id.cardhealh_doctor).setOnClickListener(v -> startActivity(new Intent(this, HealthArticlesActivity.class)));
        findViewById(R.id.card_step).setOnClickListener(v -> startActivity(new Intent(this, pedometerActivity.class)));
        findViewById(R.id.card_medical_records).setOnClickListener(v -> startActivity(new Intent(this, MedicalRecordsActivity.class)));
        findViewById(R.id.card_event_media).setOnClickListener(v -> startActivity(new Intent(this, EventMediaActivity.class)));
    }
}