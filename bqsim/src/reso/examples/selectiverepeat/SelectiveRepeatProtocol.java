package reso.examples.selectiverepeat;

import reso.common.AbstractTimer;
import reso.ip.Datagram;
import reso.ip.IPAddress;
import reso.ip.IPHost;
import reso.ip.IPInterfaceAdapter;
import reso.ip.IPInterfaceListener;
import reso.scheduler.AbstractScheduler;


import java.io.FileWriter;
import java.util.Random;
import java.lang.Math;

public class SelectiveRepeatProtocol implements IPInterfaceListener {

    // IP RELATED #############################################################

    public static final int IP_SR_PROTOCOL = Datagram.allocateProtocolNumber("SELECTIVE_REPEAT");

    private final IPHost host;


    // SELECTIVE REPEAT RELATED ###############################################

    //current packet number
    public int seqNbr = 0;

    //window size
    public double size = 1;

    // first packet to send in the window
    public int sendBase = 0;

    // SELECTIVE REPEAT TIMER  ################################################

    protected SelectiveRepeatTimer timer;

    private int tripleAck = 0;

    private int rptdAck = -1;

    // PACKET RELATED #########################################################

    private Datagram ack = null;

    private SRMessage[] packetLst;

    private static Random random = new Random();

    private double lossProb;

    // RTP CALCULATION ########################################################

    private static double ALPHA = 0.125;

    private static double BETA  = 0.25;

    private double SRTT;

    private double DevRTT;

    private double RTO = 3;

    // CONGESTION CONTROL ######################################################

    private double oldSize;

    private double newSize;

    private final int MSS = 1;

    private double sstresh = 20;

    // DATA EXPORT ##############################################################

    private String dataExport;

    private class SelectiveRepeatTimer extends AbstractTimer{

        private IPAddress dst;

        private double startTime;

        private double stopTime;

        public SelectiveRepeatTimer(AbstractScheduler scheduler, double interval, IPAddress dst) {
            super(scheduler, interval, false);
            this.dst = dst;
        }


        protected void run() throws Exception{
            timeout(dst);
            System.out.println("app=[" + host.name + "]" +
                    " time=" + scheduler.getCurrentTime());
        }

        @Override
        public void start(){
            if (seqNbr < packetLst.length){
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

    public SelectiveRepeatProtocol(IPHost host, double lossProb) throws Exception{
        this.host = host;
        host.getIPLayer().addListener(this.IP_SR_PROTOCOL, this);
        this.lossProb = lossProb;
    }

    public SelectiveRepeatProtocol(IPHost host, SRMessage[] packetLst, double lossProb){
        this.host = host;
        this.packetLst = packetLst;
        host.getIPLayer().addListener(this.IP_SR_PROTOCOL, this);
        this.lossProb = lossProb;
    }

    private double getSRTT(){
        if (SRTT > 0) {
            SRTT = ((1 - ALPHA) * this.SRTT + ALPHA * timer.getR());
        }
        else{
            SRTT = timer.getR();
        }
        return SRTT;
    }

    private double getDevRTT(){
        if (DevRTT > 0){
            DevRTT = ((1-BETA) * DevRTT + BETA * Math.abs(getSRTT() - timer.getR()));
        }
        else{
            DevRTT = timer.getR()/2;
        }
        return DevRTT;
    }

    private void changeRTO(){
        double devRTT = getDevRTT();
        RTO = 4*devRTT + getSRTT();
    }

    public void timeout(IPAddress dst) throws Exception{
        changeRTO();
        timer = new SelectiveRepeatTimer(host.getNetwork().getScheduler(), RTO, dst);
        timer.start();
        if(tripleAck == 3){
            System.out.println(" WARNING : Triple Ack");
        }
        else{
            System.out.println(" WARNING : TIMEOUT");
        }
        for (int i = sendBase; i < seqNbr; i++){
            System.out.println(" WARNING : Resend pkt N°" + i);
            host.getIPLayer().send(IPAddress.ANY, dst, IP_SR_PROTOCOL, packetLst[i]);
        }

        sstresh = size /2;
        size = 1;
        dataExport += "Current Time : " + host.getNetwork().getScheduler().getCurrentTime() + ", Window size :" + size + "\n";
    }


    public void send(int data, IPAddress dst) throws Exception{
        if(seqNbr < sendBase + size && seqNbr < packetLst.length){
            int[] segmentData = new int[]{data};
            SRMessage packet = new SRMessage(segmentData, seqNbr);
            packetLst[seqNbr] = packet;


            double x = random.nextDouble();
            //If x is greater than the loss probability then the packet is sent.
            if(x > lossProb){
                System.out.println("SENDING PACKET N° : " + seqNbr);
                host.getIPLayer().send(IPAddress.ANY, dst, IP_SR_PROTOCOL, packet);
            }
            //Else the packet is lost
            else{
                System.out.println(" WARNING : PACKET N° : "  + packet.sequenceNumber + " IS LOST");
            }

            if (sendBase == seqNbr){
                if (timer != null){
                    changeRTO();
                }
                timer = new SelectiveRepeatTimer(host.getNetwork().getScheduler(), RTO, dst);
                timer.start();
            }
            seqNbr += 1;
        }
    }

    public void sendACK(Datagram datagram) throws Exception{
        SRMessage packet = new SRMessage(((SRMessage) datagram.getPayload()).sequenceNumber);

        double x = random.nextDouble();
        if(x > lossProb){
            System.out.println("SENDING ACK FOR PACKET N°" + packet.sequenceNumber);
            host.getIPLayer().send(IPAddress.ANY, datagram.src, IP_SR_PROTOCOL, packet);
        }
        ack = datagram;
    }

    @Override
    public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception{
        SRMessage segment = (SRMessage) datagram.getPayload();

        //If ack segment its part of sender side
        if (segment.isAckD()){
            //Detect triple ack
            System.out.println(" RECEIVED ACK N°"  + segment.sequenceNumber);
            if (rptdAck >= 0){
                if (seqNbr == rptdAck){
                    tripleAck += 1;
                }
                else{
                    tripleAck = 0;
                }
            }
            rptdAck = segment.sequenceNumber;

            //if the packet is 3 acked
            if(tripleAck == 3){
                timeout(datagram.src);
                tripleAck = 0;
                //Multiplicative decrease
                size = size/2;
            }

            //Additive increase.
            oldSize = size;
            if (size <= sstresh){
                size += MSS;
            }

            else{
                size = size + MSS/size;
            }
            newSize = size;
            dataExport += "Current Time : " + host.getNetwork().getScheduler().getCurrentTime() + ", Window size :" + size + "\n";

            double offset = newSize - oldSize;

            //Check corruption
            sendBase = segment.sequenceNumber + 1;
            if(sendBase == seqNbr){
                System.out.println("--------STOP--------");
                timer.stop();
                FileWriter fw = new FileWriter("SizeOfWindow.csv");
                fw.write(dataExport);
                fw.close();
            }
            else{
                changeRTO();
                timer = new SelectiveRepeatTimer(host.getNetwork().getScheduler(), RTO, datagram.src);
                timer.start();
            }
            for (int i = 0; i <= offset; i++){
                send(packetLst[seqNbr].data[0], datagram.src);
            }
        }
        // Not an ack segment so its receiver side
        else{
            System.out.println("RECEIVED PK N° : " + segment.sequenceNumber);
            //Check corruption
            if(segment.sequenceNumber == seqNbr){
                //find how do we deliver data to application
                sendACK(datagram);
                seqNbr += 1;
            }
            else{
                if(ack != null){
                    sendACK(ack);
                }
            }
        }
    }

}
