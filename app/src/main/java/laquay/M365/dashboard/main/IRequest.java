package laquay.M365.dashboard.main;

public interface IRequest {
    int getDelay();

    String getRequestString();

    //get RequestBit to identify
    String getRequestBit();

    //expected to update the textviews and the statistic class
    String handleResponse(String[] request);

    RequestType getRequestType();
}
