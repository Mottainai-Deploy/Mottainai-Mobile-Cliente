package com.mottainai.cliente.activities;
import android.os.Bundle;
import com.mottainai.cliente.MainActivity;
public class RegisterClienteActivity extends MainActivity {
    public static final String EXTRA_PROFILE_REQUIRED="profile_required", EXTRA_PROFILE_EMAIL="profile_email";
    @Override protected void onCreate(Bundle state) {getIntent().putExtra("screen","2143:9435");super.onCreate(state);}
}
