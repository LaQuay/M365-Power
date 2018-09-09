package laquay.M365.dashboard.lib.requests;

import java.util.concurrent.TimeUnit;

import laquay.M365.dashboard.lib.NbCommands;
import laquay.M365.dashboard.lib.NbMessage;
import laquay.M365.dashboard.lib.Statistics;

public class DistanceRequest implements IRequest {
    private static int delay = 500;
    private final String requestBit = "B9";
    private final RequestType requestType = RequestType.DISTANCE;
    private long startTime;

    public DistanceRequest() {
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
                .setPosition(0xB9)
                .setPayload(0x02)
                .build();
    }

    @Override
    public String getRequestBit() {
        return requestBit;
    }

    @Override
    public String handleResponse(String[] request) {
        String temp = request[7] + request[6];
        int distance = (short) Integer.parseInt(temp, 16);

        double v = distance;
        v = v / 100;
        //Log.d("Dist","distance:"+v);
        Statistics.setDistanceTravelled(v);
        return v + " km";
    }

    public long getDelay(TimeUnit timeUnit) {
        long diff = startTime - System.currentTimeMillis();
        return timeUnit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
