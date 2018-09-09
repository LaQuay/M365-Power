package laquay.M365.dashboard.lib.requests;

import laquay.M365.dashboard.lib.NbCommands;
import laquay.M365.dashboard.lib.NbMessage;
import laquay.M365.dashboard.lib.Statistics;

public class VoltageRequest implements IRequest {
    private static int delay = 500;
    private final String requestBit = "34";
    private final RequestType requestType = RequestType.VOLTAGE;
    private long startTime;

    public VoltageRequest() {
        this.startTime = System.currentTimeMillis() + delay;
    }

    @Override
    public int getDelay() {
        return delay;
    }

    @Override
    public String getRequestString() {
        return new NbMessage()
                .setDirection(NbCommands.MASTER_TO_BATTERY)
                .setRW(NbCommands.READ)
                .setPosition(0x34)
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

        int voltage = (short) Integer.parseInt(temp, 16);
        double v = voltage / 100.0;
        Statistics.setCurrentVoltage(v);

        return v + " V";
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
