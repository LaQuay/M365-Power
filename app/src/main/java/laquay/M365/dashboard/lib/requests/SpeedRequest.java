package laquay.M365.dashboard.lib.requests;

import java.util.concurrent.TimeUnit;

import laquay.M365.dashboard.lib.NbCommands;
import laquay.M365.dashboard.lib.NbMessage;
import laquay.M365.dashboard.lib.Statistics;

public class SpeedRequest implements IRequest {
    private static int delay = 500;
    private final String requestBit = "B5";
    private final RequestType requestType = RequestType.SPEED;
    private long startTime;

    public SpeedRequest() {
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
                .setPosition(0xB5)
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

        int speed = (short) Integer.parseInt(temp, 16);

        double v = speed / 1000.0;
        Statistics.setSpeed(v);
        v = Statistics.round(v, 1);

        return v + "";
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
