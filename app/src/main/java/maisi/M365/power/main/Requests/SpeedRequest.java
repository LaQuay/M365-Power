package maisi.M365.power.main.Requests;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import maisi.M365.power.main.IRequest;
import maisi.M365.power.main.RequestType;
import maisi.M365.power.main.Statistics;
import maisi.M365.power.util.NbCommands;
import maisi.M365.power.util.NbMessage;

public class SpeedRequest implements IRequest {
    private static int delay = 500;
    private long startTime;
    private final String requestBit = "B5";
    private final RequestType requestType = RequestType.SPEED;

    public SpeedRequest(){
        this.startTime = System.currentTimeMillis() + delay;
    }

    @Override
    public int getDelay() {
        return delay;
    }

    @Override
    public String getRequestString() {
        String ctrlVersion = new NbMessage()
                .setDirection(NbCommands.MASTER_TO_M365)
                .setRW(NbCommands.READ)
                .setPosition(0xB5)
                .setPayload(0x02)
                .build();
        return ctrlVersion;
    }

    @Override
    public String getRequestBit() {
        return requestBit;
    }

    @Override
    public String handleResponse(String[] request) {
        String temp = request[7] + request[6];
        int speed = (short) Integer.parseInt(temp, 16);

        double v = speed;
        v=v/1000;
        //Log.d("Speed","speed:"+v);
        Statistics.setSpeed(v);
        return v + " km/h";
        //return textViews;
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
