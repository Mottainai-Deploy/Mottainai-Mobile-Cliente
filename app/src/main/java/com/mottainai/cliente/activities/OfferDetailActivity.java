package com.mottainai.cliente.activities;
import android.os.Bundle;
import com.mottainai.cliente.MainActivity;
public class OfferDetailActivity extends MainActivity {
    @Override protected void onCreate(Bundle state) { getIntent().putExtra("screen","2005:4104");super.onCreate(state); }
}
