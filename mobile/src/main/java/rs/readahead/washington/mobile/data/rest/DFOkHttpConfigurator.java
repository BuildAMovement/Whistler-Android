package rs.readahead.washington.mobile.data.rest;

import okhttp3.OkHttpClient;


public class DFOkHttpConfigurator {
    public static OkHttpClient.Builder configure(OkHttpClient.Builder builder) {
        // todo: harden google.com connection?
        return builder;
    }
}
