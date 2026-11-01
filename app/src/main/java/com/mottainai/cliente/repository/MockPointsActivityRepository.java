package com.mottainai.cliente.repository;

import com.mottainai.cliente.R;
import com.mottainai.cliente.models.PointsActivityEntry;

import java.util.ArrayList;
import java.util.List;

public class MockPointsActivityRepository {

    public List<PointsActivityEntry> getRecentActivity() {
        List<PointsActivityEntry> activity = new ArrayList<>();
        activity.add(new PointsActivityEntry("Compra salva", 120, R.color.card_light_green));
        activity.add(new PointsActivityEntry("Desafio semanal", 80, R.color.card_light_green));
        activity.add(new PointsActivityEntry("Resgate de oferta", -200, R.color.thumb_peach));
        return activity;
    }
}
