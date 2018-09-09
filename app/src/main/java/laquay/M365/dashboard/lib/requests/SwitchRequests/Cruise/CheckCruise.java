package laquay.M365.dashboard.lib.requests.SwitchRequests.Cruise;

import laquay.M365.dashboard.lib.NbCommands;
import laquay.M365.dashboard.lib.NbMessage;
import laquay.M365.dashboard.lib.Statistics;
import laquay.M365.dashboard.lib.requests.IRequest;
import laquay.M365.dashboard.lib.requests.RequestType;

public class CheckCruise implements IRequest {
    private static int delay = 100;
    private final String requestBit = "7C";
    private final RequestType requestType = RequestType.CRUISE;
    private long startTime;

    public CheckCruise() {
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
                .setRW(NbCommands.READ)
                .setPosition(0x7C)
                .setPayload(0x02)
                .build();
    }

    @Override
    public String getRequestBit() {
        return requestBit;
    }

    @Override
    public String handleResponse(String[] request) {
        if (request[6].equals("01")) {
            Statistics.setCruiseActive(true);
        } else {
            Statistics.setCruiseActive(false);
        }
        return "";
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
