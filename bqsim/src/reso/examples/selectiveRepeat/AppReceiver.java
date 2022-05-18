package reso.examples.selectiveRepeat;

import reso.common.AbstractApplication;
import reso.ip.IPHost;
import reso.ip.IPLayer;

/**
 * This class is the receiver part of the Application layer.
 * The class is designed to receive message from the sender through the transport layer which
 * in this case is represented by the SRProtocol that is called in the start() method.
 */
public class AppReceiver extends AbstractApplication {

    /**
     * The ip address of the host
     */
    private final IPLayer ip;

    /**
     * The probability of a packet or ack has to be lost during the transfer.
     */
    private final double lossProb;

    /**
     * Constructor of the Receiver Application
     * @param host Host whose used in the protocol
     * @param lossProb The probability of a packet or ack has to be lost during the transfer
     */
    public AppReceiver(IPHost host, double lossProb){
        super(host, "receiver");
        this.ip = host.getIPLayer();
        this.lossProb = lossProb;
    }

    /**
     * Method to start the Receiver Application
     * @throws Exception thrown in SRProtocol class
     */
    @Override
    public void start() throws Exception{
        new SRProtocol((IPHost) host, lossProb);
    }

    /**
     * Method to stop the Receiver Application
     */
    @Override
    public void stop(){}
}
