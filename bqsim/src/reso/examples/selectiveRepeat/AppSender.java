package reso.examples.selectiveRepeat;

import reso.common.AbstractApplication;
import reso.common.Host;
import reso.ip.IPAddress;
import reso.ip.IPHost;

import java.util.Random;

/**
 * This class is the Sender part of the Application layer.
 * The class is designed to send a message to the Sender through the transport layer which
 * in this case is represented by the SRProtocol that is called in the start() method.
 */
public class AppSender extends AbstractApplication {

    /**
     * The IP address of the destination on which packets have to be sent.
     */
    private final IPAddress dst;

    /**
     * Total number of packet to send determined by the user
     */
    private final int totalPacketNumber;

    /**
     * Probability that has a packet or ack to be lost during the transfer.
     */
    private double lossProb;

    /**
     * Constructor of the Sender Application
     * @param host Host of the protocol
     * @param dst IP address of the destination on which packets have to be sent
     * @param totalPacketNumber Total number of packet to send
     * @param lossProb Probability that has a packet or ack to be lost during the transfer.
     */
    public AppSender(Host host, IPAddress dst, int totalPacketNumber, double lossProb){
        super(host, "sender");
        this.dst = dst;
        this.totalPacketNumber = totalPacketNumber;
        this.lossProb = lossProb;
    }

    /**
     * Method to start the Sender Application
     * @throws Exception Thrown in send method of the SRProtocol
     */
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

    /**
     * Method to stop the Sender Application
     */
    @Override
    public void stop(){}
}
