package laquay.M365.dashboard.device;

public interface ConnectionCallback {
    void onConnectionOK(String data);

    void onConnectionFail(String error);
}
