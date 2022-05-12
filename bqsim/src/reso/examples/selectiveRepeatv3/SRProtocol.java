package reso.examples.selectiveRepeatv3;

import reso.common.AbstractTimer;
import reso.examples.selectiverepeat.SRMessage;
import reso.ip.*;
import reso.scheduler.AbstractScheduler;

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

    private Datagram[] acks;

    //LOSS PROBABILITY VARIABLES

    private static Random random = new Random();

    private double lossProb;

    //TIMER VARIABLES

    private SRTimer[] timers;

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

    public SRProtocol(IPHost host, double lossProb) throws Exception{
        this.host = host;
        host.getIPLayer().addListener(this.IP_SR_PROTOCOL, this);
        this.lossProb = lossProb;
    }

    public SRProtocol(IPHost host, SRPacket[] packetLst, double lossProb){
        this.host = host;
        this.packetLst = packetLst;
        this.buffer = new SRPacket[packetLst.length];
        this.acks = new Datagram[packetLst.length];
        host.getIPLayer().addListener(this.IP_SR_PROTOCOL, this);
        this.lossProb = lossProb;
    }

    public void timeout(IPAddress dst, int seqNumber)throws Exception{
        //TODO ADD RTT AND CONGESTION
        timers[seqNumber] = new SRTimer(host.getNetwork().getScheduler(), /*RTO*/ 3, dst, seqNumber);
        timers[seqNumber].start();

        //TODO CHECK CORRUPTION

        System.out.println(" WARNING : Resend packet N° : " + seqNumber);
        host.getIPLayer().send(IPAddress.ANY, dst, IP_SR_PROTOCOL, packetLst[seqNumber]);

        //TODO CHANGE SSTHRESH
        size = 1;
        //TODO MANAGE DATA EXPORT
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
            }
            else{
                System.out.println(" WARNING : PACKET N° : " +packet.seqNumber + " IS LOST");
            }

            //If the current packet is the first packet to be sent in the emission window then run a timer on it
            //before sending it.
            if (seqNumber == sendBase){
                if (timers[seqNumber] != null){
                    //TODO CHANGE THE RTO
                }
                timers[seqNumber] = new SRTimer(host.getNetwork().getScheduler(), /*RT0*/ 3, dst, seqNumber);
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
        acks[seqNumber] = datagram;
    }

    @Override
    public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception{

        SRPacket packet = (SRPacket) datagram.getPayload();

        //first check if its an ACK

        if (packet.isAnAck()){
            //First check for corruption
            System.out.println(" RECEIVED ACK N° : " +packet.seqNumber);
            //TODO
        }


        //reciever side
        else{
            System.out.println(" RECEIVED PACKET N° : " + packet.seqNumber);
            if (packet.seqNumber >= recvBase && packet.seqNumber <= recvBase + size -1){
                buffer[seqNumber] = packet;
                //TODO sendACK(datagram);
                if (seqNumber == recvBase){
                    //TODO DELIVER DATA TO APP
                    recvBase ++;
                    while(buffer[recvBase] != null){
                        //deliverData(buffer[recvBase].data)
                        recvBase ++;
                    }
                }
                else if (packet.seqNumber >= recvBase - size && packet.seqNumber <= recvBase -1){
                    //sendACK(datagram)
                }
            }
        }
    }
}
