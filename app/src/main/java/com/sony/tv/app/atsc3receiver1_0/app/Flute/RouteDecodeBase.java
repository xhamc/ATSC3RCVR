package com.sony.tv.app.atsc3receiver1_0.app.Flute;

/**
 * Interface to support Qualcomm and NAB flavors of RouteDecodeQu
 * Created by xhamc on 4/3/17.
 */

public interface RouteDecodeBase {


    void fileName(String fileName);
    int tsi();
    int toi();
    int arrayPosition();
    String fileName();
    int contentLength();
    int efdt_toi();
    boolean valid();

}
