package reso.examples.selectiverepeat;

import reso.common.AbstractApplication;
import reso.ip.IPHost;
import reso.ip.IPLayer;

public class AppReceiver extends AbstractApplication {

    private final IPLayer ip;

    private double lossProb;

    public AppReceiver(IPHost host, double lossProb) {
        super(host, "receiver");
        this.ip = host.getIPLayer();
        this.lossProb = lossProb;
    }

    public void start() throws Exception {
        SelectiveRepeatProtocol transport = new SelectiveRepeatProtocol((IPHost) host, lossProb);
    }

    public void stop(){}
}
