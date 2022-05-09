package reso.examples.selectiveRepeatv2;

import reso.common.AbstractApplication;
import reso.common.Host;
import reso.ip.IPAddress;

import java.util.Random;

public class AppSender extends AbstractApplication {

    private final IPAddress dst;

    /**
     * Total number of packet to send determined by the user
     */
    private final int packetNumber;

    private double lossProb;

    public AppSender(Host host, IPAddress dst, int packetNumber, double lossProb){
        super(host, "sender");
        this.dst = dst;
        this.packetNumber = packetNumber;
        this.lossProb = lossProb;
    }

    public void star() throws Exception{
        Random random = new Random();
        SRPacket[] packetLst = new SRPacket[packetNumber];
        for(int i = 0; i < packetNumber; i++){
            packetLst[i] = new SRPacket(random.nextInt(), i);
        }
        //TODO call the protocol here to use the transport layer;
    }
}
