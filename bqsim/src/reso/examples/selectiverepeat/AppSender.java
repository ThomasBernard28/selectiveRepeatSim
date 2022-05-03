package reso.examples.selectiverepeat;

import reso.common.AbstractApplication;
import reso.common.Host;
import reso.ip.IPAddress;
import reso.ip.IPHost;
import reso.ip.IPLayer;

import java.util.List;

public class AppSender extends AbstractApplication {


    private final IPLayer ip;
    private final IPAddress dst;
    //List containing all packets to send
    private final List<Integer> data;
    //Number of the 1st packet in the sending window
    private int sendBase;
    //Number of the next packet to send
    private int nextSeqNum;

    public AppSender(IPHost host, String name, IPAddress dst, List<Integer> data, int sendBase, int nextSeqNum) {
        super(host, "sender");
        this.dst = dst;
        this.data = data;
        this.sendBase = sendBase;
        this.nextSeqNum = nextSeqNum;
        ip = host.getIPLayer();
    }

    public void start() throws Exception{
        ip.addListener(SelectiveRepeatProtocol.IP_PROTO_SR, new SelectiveRepeatProtocol((IPHost) host));
        ip.send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, new SelectiveRepeatMessage(data.get(nextSeqNum)));
    }

    public void stop(){}
}
