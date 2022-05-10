package reso.examples.selectiveRepeatv3;

import reso.common.AbstractApplication;
import reso.ip.IPHost;
import reso.ip.IPLayer;

public class AppReceiver extends AbstractApplication {

    private final IPLayer ip;

    private final double lossProb;

    public AppReceiver(IPHost host, double lossProb){
        super(host, "receiver");
        this.ip = host.getIPLayer();
        this.lossProb = lossProb;
    }

    public void start() throws Exception{
        //TODO call the trnasport layer protocol
    }

    public void stop(){}
}
