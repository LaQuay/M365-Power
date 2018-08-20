package laquay.M365.dashboard.main.Requests.SwitchRequests.Recovery;

import java.util.Arrays;

import laquay.M365.dashboard.main.IRequest;
import laquay.M365.dashboard.main.RequestType;
import laquay.M365.dashboard.util.NbCommands;
import laquay.M365.dashboard.util.NbMessage;

public class StrongMode implements IRequest {
    private static int delay = 100;
    private final String requestBit = "7B";
    private final RequestType requestType = RequestType.NOCOUNT;
    private long startTime;

    public StrongMode() {
        this.startTime = System.currentTimeMillis() + delay;
    }

    @Override
    public int getDelay() {
        return delay;
    }

    @Override
    public String getRequestString() {
        return new NbMessage()
                .setDirection(NbCommands.MASTER_TO_M365)
                .setRW(NbCommands.WRITE)
                .setPosition(0x7B)
                .setPayload(0x0002)
                .build();
    }

    @Override
    public String getRequestBit() {
        return requestBit;
    }

    @Override
    public String handleResponse(String[] request) {
        return Arrays.toString(request);
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
