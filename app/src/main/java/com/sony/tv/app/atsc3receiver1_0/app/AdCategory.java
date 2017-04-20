package com.sony.tv.app.atsc3receiver1_0.app;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by valokafor on 4/17/17.
 */

public class AdCategory extends RealmObject{
    @PrimaryKey
    private long id;
    private String name;
    private boolean checked;
    private RealmList<AdContent> ads;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RealmList<AdContent> getAds() {
        return ads;
    }

    public void setAds(RealmList<AdContent> ads) {
        this.ads = ads;
    }


    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
