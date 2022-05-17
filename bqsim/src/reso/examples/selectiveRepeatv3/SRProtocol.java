package reso.examples.selectiveRepeatv3;

import reso.common.AbstractTimer;
import reso.ip.*;
import reso.scheduler.AbstractScheduler;

import java.io.FileWriter;
import java.util.Random;

public class SRProtocol implements IPInterfaceListener{

    // IP VARIABLES
    public static final int IP_SR_PROTOCOL = Datagram.allocateProtocolNumber("SELECTIVE_REPEAT");

    private final IPHost host;

    //PACKET VARIABLES
    public int seqNumber = 0;

    public int sendBase = 0;

    public int recvBase = 0;

    public SRPacket[] packetLst;

    public SRPacket[] buffer;

    //LOSS PROBABILITY VARIABLES

    private static Random random = new Random();

    private double lossProb;

    //TIMER VARIABLES

    private SRTimer[] timers;

    private int[] tripleAck;


    // RTP VARIABLES

    private double srtt;

    private double devRtt;

    private double rto = 3;

    //DATA EXPORT VARIABLES

    private String dataExport = "";

    //CONGESTION WINDOW CONTROL

    private double slowStartTresh = 20;

    public double windowSize = 1;

    public double oldSize;

    public double offset;



    //TIMER CLASS

    private class SRTimer extends AbstractTimer {

        //Time at the start of the timer
        private double startTime;

        //Time at the end of the timer
        private double stopTime;

        private IPAddress dst;

        private int seqNumber;

        public SRTimer(AbstractScheduler scheduler, double interval, IPAddress dst, int seqNumber){
            super(scheduler, interval, false);
            this.dst =dst;
            this.seqNumber = seqNumber;
        }


        protected void run() throws Exception{
            if(!packetLst[seqNumber].isAcknowledged()){
                stop();
                timeout(dst, seqNumber);
                System.out.println("App=[" + host.name + "] Packet : " +seqNumber + " time= " +scheduler.getCurrentTime());
            }
        }

        @Override
        public void start(){
            super.start();
            startTime = scheduler.getCurrentTime();
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
    // CONSTRUCTORS AND METHODS

    //Receiver constructor
    public SRProtocol(IPHost host, double lossProb) throws Exception{
        this.host = host;
        host.getIPLayer().addListener(IP_SR_PROTOCOL, this);
        this.lossProb = lossProb;
        this.buffer = new SRPacket[50];
    }



    public SRProtocol(IPHost host, SRPacket[] packetLst, double lossProb){
        this.host = host;
        this.packetLst = packetLst;
        this.buffer = new SRPacket[packetLst.length];
        this.timers = new SRTimer[packetLst.length];
        this.tripleAck = new int[packetLst.length];
        this.lossProb = lossProb;
        host.getIPLayer().addListener(IP_SR_PROTOCOL, this);
        dataExport += host.getNetwork().getScheduler().getCurrentTime() + ", " + windowSize + "\n";
    }

    private double getSRTT(int seqNumber){
        double alpha = 0.125;
        if (srtt > 0){
            srtt = (1-alpha) * srtt + alpha * timers[seqNumber].getR();
        }
        else{
            srtt = timers[seqNumber].getR();
        }
        return srtt;
    }

    private double getDevRtt(int seqNumber){
        double beta = 0.25;
        if (devRtt > 0){
            devRtt = (1 - beta) * devRtt + beta * Math.abs((srtt - timers[seqNumber].getR()));
        }
        else{
            devRtt = timers[seqNumber].getR() / 2;
        }
        return devRtt;
    }

    private void changeRTO(int seqNumber){
        rto = 4*getDevRtt(seqNumber) + getSRTT(seqNumber);
    }

    public void timeout(IPAddress dst, int seqNumber)throws Exception{
        changeRTO(seqNumber);
        timers[seqNumber] = new SRTimer(host.getNetwork().getScheduler(), rto, dst, seqNumber);
        timers[seqNumber].start();

        if (tripleAck[seqNumber] == 3){
            System.out.println(" WARNING : Triple Ack");
        }

        else{
            System.out.println(" WARNING : TIMEOUT");
        }

        //Resending pkt
        host.getIPLayer().send(IPAddress.ANY, dst, IP_SR_PROTOCOL, packetLst[seqNumber]);

        //congestion window management
        oldSize = windowSize;
        windowSize = 1;
        slowStartTresh = oldSize / 2;

        //data export management
        dataExport += host.getNetwork().getScheduler().getCurrentTime() + ", " + windowSize + "\n";
    }


    public void send(int data, IPAddress dst) throws Exception{
        if (seqNumber < sendBase + windowSize && seqNumber < packetLst.length){
            SRPacket packet = new SRPacket(data, seqNumber);
            packetLst[seqNumber] = packet;
            //timers[seqNumber] = new SRTimer(host.getNetwork().getScheduler(), rto, dst, seqNumber);

            double x = random.nextDouble();
            //In this case we can send the data because x is greater than the loss prob
            if (x > lossProb){
                System.out.println(" SENDING PACKET N° : " + seqNumber);
                host.getIPLayer().send(IPAddress.ANY, dst, IP_SR_PROTOCOL, packet);
                timers[seqNumber] = new SRTimer(host.getNetwork().getScheduler(), rto, dst, seqNumber);
            }
            else{
                System.out.println(" WARNING : PACKET N° : " + packet.seqNumber + " IS LOST");
                if (packet.seqNumber == 0){
                    System.out.println(" RESEND PKT N° 0");
                    host.getIPLayer().send(IPAddress.ANY, dst, IP_SR_PROTOCOL, packet);
                    timers[seqNumber] = new SRTimer(host.getNetwork().getScheduler(), rto, dst, packet.seqNumber);
                }

            }

            //If the current packet is the first packet to be sent in the emission window then run a timer on it
            //before sending it.
            if (seqNumber == sendBase && seqNumber != 0){
                if (timers[seqNumber] != null){
                    changeRTO(seqNumber);
                }
                timers[seqNumber] = new SRTimer(host.getNetwork().getScheduler(), rto, dst, seqNumber);
                timers[seqNumber].start();
            }
            seqNumber ++;
        }
    }

    public void sendACK(Datagram datagram, int recvBase) throws Exception{
        SRPacket packet = new SRPacket(((SRPacket) datagram.getPayload()).seqNumber, recvBase, true);
        this.recvBase = recvBase;

        double x = random.nextDouble();

        if(x > lossProb){
            System.out.println("SENDING ACK FOR PACKET N° : " + packet.seqNumber);
            host.getIPLayer().send(IPAddress.ANY, datagram.src, IP_SR_PROTOCOL, packet);
        }
        else{
            sendACK(datagram, recvBase);
        }
    }
    @Override
    public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception{

        SRPacket rcvdPacket = (SRPacket) datagram.getPayload();

        //if ack
        if (rcvdPacket.isAnAck){
            this.recvBase = rcvdPacket.recvBase + 1;
            timers[rcvdPacket.seqNumber].stop();
            changeRTO(rcvdPacket.seqNumber);

            System.out.println("RECEIVED ACK FOR PKT N° " + rcvdPacket.seqNumber);


            tripleAck[rcvdPacket.seqNumber]++;
            if(tripleAck[rcvdPacket.seqNumber] == 3) {
                System.out.println("WARN: Triple ack");

                //Timeout and refresh packet
                timeout(datagram.src, rcvdPacket.seqNumber);
                tripleAck[rcvdPacket.seqNumber] = 0;

                //update window
                windowSize = windowSize / 2;
                slowStartTresh = windowSize;
                dataExport += host.getNetwork().getScheduler().getCurrentTime() + ", " + windowSize + "\n";
            }

            oldSize = windowSize;
            if (windowSize < slowStartTresh){
                windowSize ++;
            }
            else {
                windowSize = windowSize + 1/windowSize;
            }
            offset = windowSize - oldSize;

            dataExport += host.getNetwork().getScheduler().getCurrentTime() + ", " + windowSize + "\n";

            if(sendBase <= rcvdPacket.seqNumber && rcvdPacket.seqNumber < sendBase + windowSize) {
                packetLst[sendBase].setAsAcknowledged();

                if(rcvdPacket.seqNumber == sendBase) {
                    while (packetLst[sendBase].isAcknowledged() && sendBase < packetLst.length - 1) {
                        //deliver data
                        sendBase++;
                        send(packetLst[sendBase].data, datagram.src);
                    }

                }
            }
            if(recvBase == packetLst.length) {
                FileWriter fw = new FileWriter("SizeOfWindow.csv");
                fw.write(dataExport);
                fw.close();
                System.out.println("THE END");
            }

        }
        else{
            System.out.println(" RECEIVED PKT N° " + rcvdPacket.seqNumber);
            if (rcvdPacket.seqNumber >= recvBase && rcvdPacket.seqNumber <= recvBase + windowSize - 1){
                sendACK(datagram, recvBase);

                if(recvBase == rcvdPacket.seqNumber){
                    //deliver data
                    recvBase ++;
                    while (buffer[recvBase - rcvdPacket.seqNumber] != null){
                        //deliver data
                        buffer[recvBase - rcvdPacket.seqNumber] = null;
                        recvBase ++;
                    }
                }
                else{
                    buffer[rcvdPacket.seqNumber - recvBase] = rcvdPacket;
                }
            }
            else if (rcvdPacket.seqNumber >= recvBase - windowSize && rcvdPacket.seqNumber <= recvBase - 1){
                sendACK(datagram, recvBase);
            }
        }
    }
}
