package laquay.M365.dashboard.lib.requests;

import laquay.M365.dashboard.lib.NbCommands;
import laquay.M365.dashboard.lib.NbMessage;
import laquay.M365.dashboard.lib.Statistics;

public class SuperMasterRequest implements IRequest {
    private final String requestBit = "B0";
    private final RequestType requestType = RequestType.SUPERMASTER;

    @Override
    public int getDelay() {
        return 0;
    }

    @Override
    public String getRequestString() {
        return new NbMessage()
                .setDirection(NbCommands.MASTER_TO_M365)
                .setRW(NbCommands.READ)
                .setPosition(0xb0)
                .setPayload(0x20)
                .build();
    }

    @Override
    public String getRequestBit() {
        return requestBit;
    }

    @Override
    public String handleResponse(String[] request) {
        String tempSpeed = request[17] + request[16];
        String tempDistance = request[25] + request[24];
        String tempTemperature = request[29] + request[28];

        int speed = (short) Integer.parseInt(tempSpeed, 16);
        double v = speed / 1000.0;
        Statistics.setSpeed(v);
        v = Statistics.round(v, 1);

        int distance = (short) Integer.parseInt(tempDistance, 16);
        double dist = distance / 100.0;
        Statistics.setDistanceTravelled(dist);

        int temperature = (short) Integer.parseInt(tempTemperature, 16);
        double temp = temperature / 10.0;
        Statistics.setMotorTemperature(temp);

        return v + ""; // TODO ???
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
