package reso.examples.selectiveRepeatv3;

import reso.common.AbstractTimer;
import reso.ip.Datagram;
import reso.ip.IPAddress;
import reso.ip.IPHost;
import reso.ip.IPInterfaceListener;
import reso.scheduler.AbstractScheduler;

import java.util.Random;

public class SRProtocol extends IPInterfaceListener {


    //IP ADDRESS VARIABLES -------------------------------------

    public final IPHost host;

    public static final int IP_SR_PROTOCOL = Datagram.allocateProtocolNumber("SELECTIVE_REPEAT");

    // SELECTIVE REPEAT ----------------------------------------

    /**
     * Sequence number of the current packet being treated
     */
    public int seqNumber = 0;

    /**
     * Sequence number of the first packet in the emission window
     */
    public int sendBase = 0;

    /**
     * Sequence number of the first packet in the reception window
     */
    public int recvBase = 0;

    /**
     * Size of the window
     */
    public double size = 1;

    // PACKET RELATED ------------------------------------------

    private Datagram ack = null;

    private SRPacket[] packetLst;

    private SRPacket[] buffer;

    private static Random random = new Random();

    private double lossProb;

    // RTP CALCULATION -----------------------------------------

    private static double ALPHA = 0.125;

    private static double BETA  = 0.25;

    private double SRTT;

    private double DevRTT;

    private double RTO = 3;

    // CONGESTION CONTROL --------------------------------------

    private double oldSize;

    private double newSize;

    private final int MSS = 1;

    private double sstresh = 20;


    // DATA EXPORT -------------------------------------------

    private String dataExport;



    //TIMER CLASS --------------------------------------------
    private class SRTimer extends AbstractTimer{

        private IPAddress dst;

        private double startTime;

        private double stopTime;

        private int seqNumber;

        public SRTimer(AbstractScheduler scheduler, double interval, IPAddress dst, int seqNumber){
            super(scheduler, interval, false);
            this.dst = dst;
            this.seqNumber = seqNumber;
        }

        protected void run() throws Exception{
            //timeout(seqNumber, dst);
            System.out.println("app=[" + host.name + "]" +
                    " time" + scheduler.getCurrentTime());

        }

        @Override
        public void start(){
            if (seqNumber < packetLst.length){
                super.start();
                startTime = scheduler.getCurrentTime();
            }
        }

        @Override
        public void stop(){
            super.stop();
            stopTime = scheduler.getCurrentTime();
        }

        public double getR(){
            return stopTime - startTime;
        }
    }
    //receiver
    public SRProtocol(IPHost host, double lossProb, SRPacket[] buffer) throws Exception{
        this.host = host;
        host.getIPLayer().addListener(this.IP_SR_PROTOCOL, this);
        this.lossProb = lossProb;
        this.buffer = buffer;
    }

    //sender
    public SRProtocol(IPHost host, SRPacket[] packetLst, double lossProb){
        this.host = host;
        this.packetLst = packetLst;
        host.getIPLayer().addListener(this.IP_SR_PROTOCOL, this);
        this.lossProb = lossProb;
    }

}
