package com.jpstudiosonline.bccsms.bccsms;

/**
 * Created by Sorrows on 7/28/2018.
 */

import com.google.ads.AdRequest;
import com.google.android.gms.ads.AdListener;

import static com.jpstudiosonline.bccsms.bccsms.MainActivity.isAdBlockerPresent;

public abstract class AdBlockerListener extends AdListener {

    public abstract void onAdBlocked();

    public boolean shouldCheckAdBlock(){
        return true;
    }

    public void onReceiveAd(final com.google.android.gms.ads.AdRequest ad) {
    }

    public void onFailedToReceiveAd(final com.google.android.gms.ads.AdRequest ad, final AdRequest.ErrorCode errorCode) {
        //Log.e("AdBlockerListener", "Failed to receive ad, checking if network blocker is installed...");
        if (isAdBlockerPresent(shouldCheckAdBlock())) {
            //Log.e("AdBlockerListener", "Ad blocking seems enabled, tracking attempt");
            onAdBlocked();
        } else {
        //Log.e("AdBlockerListener", "No ad blocking detected, silently fails");
    }
}


    public void onPresentScreen(final com.google.android.gms.ads.AdRequest ad) {
    }

    public void onDismissScreen(final com.google.android.gms.ads.AdRequest ad) {
    }

    public void onLeaveApplication(final com.google.android.gms.ads.AdRequest ad) {
    }
}