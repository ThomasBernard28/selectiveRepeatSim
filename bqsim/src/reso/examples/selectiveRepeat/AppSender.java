package reso.examples.selectiveRepeat;

import reso.common.AbstractApplication;
import reso.common.Host;
import reso.ip.IPAddress;
import reso.ip.IPHost;

import java.util.Random;

public class AppSender extends AbstractApplication {

    private final IPAddress dst;

    /**
     * Total number of packet to send determined by the user
     */
    private final int totalPacketNumber;

    private double lossProb;

    public AppSender(Host host, IPAddress dst, int totalPacketNumber, double lossProb){
        super(host, "sender");
        this.dst = dst;
        this.totalPacketNumber = totalPacketNumber;
        this.lossProb = lossProb;
    }
    @Override
    public void start() throws Exception{
        Random random = new Random();
        SRPacket[] packetLst = new SRPacket[totalPacketNumber];
        for(int i = 0; i < totalPacketNumber; i++){
            packetLst[i] = new SRPacket(random.nextInt(), i);
        }
        SRProtocol transport = new SRProtocol((IPHost) host, packetLst, lossProb);

        for (int packetToSend = 0 ; packetToSend < totalPacketNumber; packetToSend ++){
            transport.send(packetLst[packetToSend].data, dst);
        }
    }
    @Override
    public void stop(){}
}
