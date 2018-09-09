package laquay.M365.dashboard.lib.requests.SwitchRequests.Locking;

import laquay.M365.dashboard.lib.NbCommands;
import laquay.M365.dashboard.lib.NbMessage;
import laquay.M365.dashboard.lib.Statistics;
import laquay.M365.dashboard.lib.requests.IRequest;
import laquay.M365.dashboard.lib.requests.RequestType;

public class CheckLock implements IRequest {
    private static int delay = 100;
    private final String requestBit = "B2";
    private final RequestType requestType = RequestType.LOCK;
    private long startTime;

    public CheckLock() {
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
                .setPosition(0xB2)
                .setPayload(0x02)
                .build();
    }

    @Override
    public String getRequestBit() {
        return requestBit;
    }

    @Override
    public String handleResponse(String[] request) {
        if (request[6].equals("02")) {
            Statistics.setScooterLocked(true);
        } else {
            Statistics.setScooterLocked(false);
        }
        return "";
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
