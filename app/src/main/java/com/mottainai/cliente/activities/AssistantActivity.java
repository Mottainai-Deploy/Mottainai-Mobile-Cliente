package com.mottainai.cliente.activities;
import android.os.Bundle;
import com.mottainai.cliente.MainActivity;
public class AssistantActivity extends MainActivity {
    @Override protected void onCreate(Bundle state) { getIntent().putExtra("screen","2055:5379");super.onCreate(state); }
}
