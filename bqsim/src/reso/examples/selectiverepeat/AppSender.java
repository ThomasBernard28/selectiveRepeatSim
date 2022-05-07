package reso.examples.selectiverepeat;

import reso.common.AbstractApplication;
import reso.common.Host;
import reso.ip.IPAddress;
import reso.ip.IPHost;

import java.util.Random;

public class AppSender extends AbstractApplication {

    private final IPAddress dst;

    private final int packetNumber;

    private double lossProb;


    public AppSender(Host host, IPAddress dst, int packetNumber, double lossProb) {
        super(host, "sender");
        this.dst = dst;
        this.packetNumber = packetNumber;
        this.lossProb = lossProb;
    }


    public void start() throws Exception{
        Random rand = new Random();
        SelectiveRepeat[] packetLst = new SelectiveRepeat[packetNumber];
        for (int i = 0; i < packetNumber; i++){
            packetLst[i] = new SelectiveRepeat(new int[] {rand.nextInt()}, i);
        }
        SelectiveRepeatProtocol transport = new SelectiveRepeatProtocol((IPHost) host, packetLst, lossProb);
        for (int i = 0; i < packetNumber; i++){
            transport.send(packetLst[i].data[0], dst);
        }
    }

    public void stop(){}
}
