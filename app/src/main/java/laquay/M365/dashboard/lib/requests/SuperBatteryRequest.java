package laquay.M365.dashboard.lib.requests;

import laquay.M365.dashboard.lib.NbCommands;
import laquay.M365.dashboard.lib.NbMessage;
import laquay.M365.dashboard.lib.Statistics;

public class SuperBatteryRequest implements IRequest {
    private final String requestBit = "31";
    private final RequestType requestType = RequestType.SUPERBATTERY;

    @Override
    public int getDelay() {
        return 0;
    }

    @Override
    public String getRequestString() {
        //55 aa 03 22 01 31 0a 9e ff
        return new NbMessage()
                .setDirection(NbCommands.MASTER_TO_BATTERY)
                .setRW(NbCommands.READ)
                .setPosition(0x31)
                .setPayload(0x0a)
                .build();
    }

    @Override
    public String getRequestBit() {
        return requestBit;
    }

    @Override
    public String handleResponse(String[] request) {
        String tempCapacity = request[7] + request[6];
        String tempLife = request[9] + request[8];
        String tempAmpere = request[11] + request[10];
        String tempVoltage = request[13] + request[12];
        String tempBat1 = request[14];
        String tempBat2 = request[15];

        int remainingCapacity = (short) Integer.parseInt(tempCapacity, 16);
        Statistics.setRemainingCapacity(remainingCapacity);

        int batteryLife = (short) Integer.parseInt(tempLife, 16);
        Statistics.setBatteryLife(batteryLife);

        int amps = (short) Integer.parseInt(tempAmpere, 16);
        double c = amps / 100.0;
        Statistics.setCurrentAmpere(c);

        int voltage = (short) Integer.parseInt(tempVoltage, 16);
        double v = voltage / 100;
        Statistics.setCurrentVoltage(v);

        int battTemp1 = (short) Integer.parseInt(tempBat1, 16);

        int battTemp2 = (short) Integer.parseInt(tempBat2, 16);

        int maxBattTemp = Math.max(battTemp1, battTemp2) - 20; // TODO ???
        Statistics.setBatteryTemperature(maxBattTemp);

        return c + " A"; // TODO ???
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
