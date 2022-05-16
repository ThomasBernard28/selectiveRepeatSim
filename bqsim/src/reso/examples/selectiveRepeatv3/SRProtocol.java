package reso.examples.selectiveRepeatv3;

import reso.common.AbstractTimer;
import reso.ip.*;
import reso.scheduler.AbstractScheduler;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;

public class SRProtocol implements IPInterfaceListener{

    // IP VARIABLES
    public static final int IP_SR_PROTOCOL = Datagram.allocateProtocolNumber("SELECTIVE_REPEAT");

    private final IPHost host;

    //PACKET VARIABLES
    public int seqNumber = 0;

    public double size = 1;

    public int sendBase = 0;

    public int recvBase = 0;

    public SRPacket[] packetLst;

    public SRPacket[] buffer;

    //LOSS PROBABILITY VARIABLES

    private static Random random = new Random();

    private double lossProb;

    //TIMER VARIABLES

    private SRTimer[] timers;

    private int tripleAck[];

    private int rpdtAck = -1;

    // RTP VARIABLES

    private static double alpha = 0.125;

    private static double beta = 0.25;

    private double srtt;

    private double devRtt;

    private double rto = 3;

    //DATA EXPORT VARIABLES

    private String dataExport;

    //CONGESTION WINDOW CONTROL

    private final int MSS = 1;

    private double slowStartTresh = 20;

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
        @Override
        protected void run() throws Exception{
            timeout(dst, seqNumber);
            System.out.println("App=[" + host.name + "] Packet : " +seqNumber + " time= " +scheduler.getCurrentTime());
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
    }

    private double getSRTT(int seqNumber){
        if (srtt > 0){
            srtt = ((1-alpha) * this.srtt + (alpha * timers[seqNumber].getR()));
        }
        else{
            srtt = timers[seqNumber].getR();
        }
        return srtt;
    }

    private double getDevRtt(int seqNumber){
        if (devRtt > 0){
            devRtt = ((1-beta) * devRtt + (beta * Math.abs(getSRTT(seqNumber) - timers[seqNumber].getR())));
        }
        else{
            devRtt = timers[seqNumber].getR();
        }
        return devRtt;
    }

    private void changeRTO(int seqNumber){
        double devRTT = getDevRtt(seqNumber);
        rto = 4 * devRTT + getSRTT(seqNumber);
    }

    public void timeout(IPAddress dst, int seqNumber)throws Exception{
        changeRTO(seqNumber);
        timers[seqNumber] = new SRTimer(host.getNetwork().getScheduler(), /*RTO*/ 3, dst, seqNumber);
        timers[seqNumber].start();

        if (tripleAck[seqNumber] == 3){
            System.out.println(" WARNING : Triple Ack");
        }

        else{
            System.out.println(" WARNING : TIMEOUT");
        }

        //System.out.println(" WARNING : Resend packet N° : " + seqNumber);
        host.getIPLayer().send(IPAddress.ANY, dst, IP_SR_PROTOCOL, packetLst[seqNumber]);

        slowStartTresh = size / 2;
        size = 1;
        dataExport += "Current Time : " + host.getNetwork().getScheduler().getCurrentTime() + ", Window size :" + size + "\n";
    }


    public void send(int data, IPAddress dst) throws Exception{
        if (seqNumber < sendBase + size && seqNumber < packetLst.length){
            SRPacket packet = new SRPacket(data, seqNumber);
            packetLst[seqNumber] = packet;


            double x = random.nextDouble();
            //In this case we can send the data because x is greater than the loss prob
            if (x > lossProb){
                System.out.println("SENDING PACKET N° : " + seqNumber);
                host.getIPLayer().send(IPAddress.ANY, dst, IP_SR_PROTOCOL, packet);
                timers[seqNumber] = new SRTimer(host.getNetwork().getScheduler(), rto, dst, seqNumber);
            }
            else{
                System.out.println(" WARNING : PACKET N° : " +packet.seqNumber + " IS LOST");
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

    public void sendACK(Datagram datagram) throws Exception{
        SRPacket packet = new SRPacket(((SRPacket) datagram.getPayload()).seqNumber);

        double x = random.nextDouble();

        if(x > lossProb){
            System.out.println("SENDING ACK FOR PACKET N° : " + packet.seqNumber);
            host.getIPLayer().send(IPAddress.ANY, datagram.src, IP_SR_PROTOCOL, packet);
        }
    }

    @Override
    public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception{

        SRPacket packet = (SRPacket) datagram.getPayload();

        //first check if its an ACK

        if(packet.isAnAck()){
            System.out.println(" RECEIVED ACK N° : " +packet.seqNumber);

            final int notSent = sendBase + Double.valueOf(size).intValue();

            //System.out.println("Stopped at packet nb " + unsent);

            // triple ack control
            tripleAck[packet.seqNumber]++;
            if(tripleAck[packet.seqNumber] == 3) {
                System.out.println("WARN: Triple ack");
                timeout(datagram.src, packet.seqNumber);
                size = size / 2;
                tripleAck[packet.seqNumber] = 0;
            }

            //final double oldSize = size;
            if(size <= slowStartTresh) {
                size += MSS;
            } else {
                size += MSS/size;
            }

            //final double offset = size - oldSize;

            if(sendBase <= packet.seqNumber && packet.seqNumber < sendBase + size) {
               timers[packet.seqNumber].stop();
               packetLst[sendBase].setAsAcknowledged();
                if(packet.seqNumber == sendBase) {
                    while (packetLst[sendBase].isAcknowledged() && sendBase < packetLst.length - 1)
                        sendBase ++;
                }
            }

            if(sendBase == packetLst.length - 1) {
                System.out.println("THE END");
            }

            for (int i = notSent; i < sendBase + size; i++){
                if(i < packetLst.length) {
                    System.out.println("Progressing window");
                    send(packetLst[i].data, datagram.src);
                }
            }
        }


        //reciever side
        else{
            if(packet.seqNumber - recvBase >= buffer.length) {
                System.out.println("out of window " + (packet.seqNumber - recvBase));
                return;
            }

            System.out.println("RECEIVED PACKET N° : " + packet.seqNumber);
            if (recvBase <= packet.seqNumber && packet.seqNumber < recvBase + size){

                sendACK(datagram);
                if (packet.seqNumber == recvBase){
                    //for(int i = 0; i < 10; i++) System.out.print(buffer[i]);
                    recvBase++;
                    while(buffer[recvBase - packet.seqNumber] != null){
                        //deliverData(buffer[recvBase].data);
                        recvBase ++;
                    }
                } else {
                    buffer[packet.seqNumber - recvBase] = packet;
                }

            } else if (recvBase - size <= packet.seqNumber && packet.seqNumber < recvBase){
                sendACK(datagram);
            }
        }
    }
}
