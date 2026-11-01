package com.mottainai.cliente.activities;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.mottainai.cliente.MainActivity;
public class OnboardingActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        startActivity(new Intent(this,MainActivity.class));
        finish();
    }
}
