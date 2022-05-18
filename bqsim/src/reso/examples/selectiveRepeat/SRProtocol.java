package reso.examples.selectiveRepeat;

import reso.common.AbstractTimer;
import reso.ip.*;
import reso.scheduler.AbstractScheduler;

import java.io.FileWriter;
import java.util.Random;

/**
 * Selective Repeat Protocol class. This class represent an implementation of the Transport layer of
 * this protocol.
 * The class is composed of two constructors. One is used for the Sender part of the protocol and another one is
 * used for the Receiver part of the protocol.
 * The class also contains an inner private class called SRTimer which is a timer designer for the protocol and which
 * is an extension of the AbstractTimer class
 */
public class SRProtocol implements IPInterfaceListener{

    // IP VARIABLES /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\
    /**
     * A protocol number to identify this particular protocol
     */
    public static final int IP_SR_PROTOCOL = Datagram.allocateProtocolNumber("SELECTIVE_REPEAT");

    /**
     * Host of the Selective Repeat Protocol
     */
    private final IPHost host;

    //PACKET VARIABLES /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\
    /**
     * Sequence number of the current packet being treated
     */
    public int seqNumber = 0;

    /**
     * Sequence number of the first packet in the emission window.
     */
    public int sendBase = 0;

    /**
     * Sequence number of the first packet expected in the reception window
     */
    public int recvBase = 0;

    /**
     * List containing all the packets to be processed by the protocol
     */
    public SRPacket[] packetLst;

    /**
     * Out-of-Order buffer used when the sequence number of the received packet is not
     * the expected one defined by recvBase.
     */
    public SRPacket[] buffer;

    //LOSS PROBABILITY VARIABLES /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\

    private static Random random = new Random();

    /**
     * Loss probability determined by the user.
     */
    private double lossProb;

    //TIMER VARIABLES /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\

    /**
     * Array of timers containing the timers of all the different packets
     */
    private SRTimer[] timers;

    /**
     * Array that contains on each index the number of ACKs received for each packet in order
     * to detect an eventual triple ACK.
     */
    private int[] tripleAck;


    // RTP VARIABLES /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\

    /**
     * SRTT
     */
    private double srtt;

    /**
     * DevRTT
     */
    private double devRtt;

    /**
     * RTO initialized to 3 seconds
     */
    private double rto = 3;

    //DATA EXPORT VARIABLES /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\

    /**
     * The string containing all window sizes by time to export in a .csv file
     */
    private String dataExport = "";

    //CONGESTION WINDOW CONTROL /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\

    /**
     * Slow start maximal window size before using additive increase
     */
    private double slowStartThresh = 25;

    /**
     * Base window size set to 1
     */
    public double windowSize = 1;

    /**
     * Previous size stored in order to determine the offset
     */
    public double oldSize;

    /**
     * Difference between old size and current window size
     */
    public double offset;

    //TIMER CLASS /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\

    /**
     * Timer inner class extending the AbstractTimer class in order
     * to be adapted to this particular protocol
     */
    private class SRTimer extends AbstractTimer {

        /**
         * Scheduler time on which the timer was started
         */
        private double startTime;

        /**
         * Scheduler timer on which the timer was stopped
         */
        private double stopTime;

        /**
         * IP adress of the destination
         */
        private IPAddress dst;

        /**
         * Sequence number of the packet on which is set the Timer.
         */
        private int seqNumber;

        /**
         * SRTimer constructor
         * @param scheduler the current scheduler used in the protocol
         * @param interval the interval of time on which the timer works
         * @param dst IP address of the destination
         * @param seqNumber Sequence number of the packet on which is set the Timer
         */
        public SRTimer(AbstractScheduler scheduler, double interval, IPAddress dst, int seqNumber){
            super(scheduler, interval, false);
            this.dst =dst;
            this.seqNumber = seqNumber;
        }

        /**
         * Method call as the timer expires
         * @throws Exception This exception is caught in the timeout method.
         */
        protected void run() throws Exception{
            // Stop the timer only if the packet is not Acknowledged
            if(!packetLst[seqNumber].isAcknowledged()){
                stop();
                timeout(dst, seqNumber);
                System.out.println("App=[" + host.name + "] Packet : " +seqNumber + " time= " +scheduler.getCurrentTime());
            }

        }

        /**
         * Method to start the timer and set its start time to the current one
         */
        @Override
        public void start(){
            super.start();
            startTime = scheduler.getCurrentTime();
        }

        /**
         * Method to stop the timer and set its stop time to the current one
         */
        @Override
        public void stop(){
            super.stop();
            stopTime = scheduler.getCurrentTime();
        }

        /**
         * This method calculate the RTT of a specific packet based on its Timer
         * @return the RTT of the timer for a specific packet
         */
        public double getR(){
            return stopTime - startTime;
        }
    }
    // CONSTRUCTORS AND METHODS /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\

    /**
     * Receiver SRProtocol constructor
     * @param host Host of the protocol
     * @param lossProb Probability that has a packet or ack to be lost
     * @throws Exception
     */
    public SRProtocol(IPHost host, double lossProb) throws Exception{
        this.host = host;
        host.getIPLayer().addListener(IP_SR_PROTOCOL, this);
        this.lossProb = lossProb;
        this.buffer = new SRPacket[50];
    }


    /**
     * Sender SRProtocol constructor
     * @param host host of the protocol
     * @param packetLst the packet list to send to the receiver part
     * @param lossProb Probability that has a packet or ack to be lost
     */
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

    /**
     * Getter for the current SRTT on packet's timer
     * @param seqNumber The sequence number packet's timer
     * @return the current SRTT
     */
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

    /**
     * Getter for the current DevRTT on packet's timer
     * @param seqNumber The sequence number of the packet's timer
     * @return the current DevRTT
     */
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

    /**
     * Method to compute the new RTO of packet's timer
     * @param seqNumber The sequence number of the packet's timer
     */
    private void changeRTO(int seqNumber){
        rto = 4*getDevRtt(seqNumber) + getSRTT(seqNumber);
    }

    /**
     * Method to timeout the timer of a specific packet in case the packet is lost or corrupted
     * @param dst the destination of the protocol
     * @param seqNumber The sequence number of the concerned packet for which we want the timer to timeout
     * @throws Exception
     */
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
        slowStartThresh = oldSize / 2;

        //data export management
        dataExport += host.getNetwork().getScheduler().getCurrentTime() + ", " + windowSize + "\n";
    }

    /**
     * Method to send a packet to the receiver part of the protocol.
     * @param data The data of the packet we want to send
     * @param dst The destination of the protocol
     * @throws Exception
     */
    public void send(int data, IPAddress dst) throws Exception{
        //Check if the packet is in the emission window
        if (seqNumber < sendBase + windowSize && seqNumber < packetLst.length){
            SRPacket packet = new SRPacket(data, seqNumber);
            packetLst[seqNumber] = packet;


            double x = random.nextDouble();
            //In this case we can send the data because x is greater than the loss prob
            if (x > lossProb){
                System.out.println(" SENDING PACKET N° : " + seqNumber);
                host.getIPLayer().send(IPAddress.ANY, dst, IP_SR_PROTOCOL, packet);
                timers[seqNumber] = new SRTimer(host.getNetwork().getScheduler(), rto, dst, seqNumber);
            }
            //else
            else{
                //Warn the user that the packet is lost
                System.out.println(" WARNING : PACKET N° : " + packet.seqNumber + " IS LOST");
                //if the sequence number = 0 we need to manually set a timer on it and resend
                if (packet.seqNumber == 0){
                    System.out.println(" RESEND PKT N° 0");
                    host.getIPLayer().send(IPAddress.ANY, dst, IP_SR_PROTOCOL, packet);
                    timers[seqNumber] = new SRTimer(host.getNetwork().getScheduler(), rto, dst, packet.seqNumber);
                }

            }

            //If the current packet is the first packet to be sent in the emission window then run a timer on it
            //before sending it.
            if (seqNumber == sendBase && seqNumber != 0){
                //if there is already a timer change the RTO
                if (timers[seqNumber] != null){
                    changeRTO(seqNumber);
                }
                timers[seqNumber] = new SRTimer(host.getNetwork().getScheduler(), rto, dst, seqNumber);
                timers[seqNumber].start();
            }
            seqNumber ++;
        }
    }

    /**
     * Method to send an ACK in the SRProtocol
     * @param datagram the datagram of the ACK
     * @param recvBase the sequence number of the packet expected in the reception window.
     * @throws Exception
     */
    public void sendACK(Datagram datagram, int recvBase) throws Exception{
        SRPacket packet = new SRPacket(((SRPacket) datagram.getPayload()).seqNumber, recvBase, true);

        //In order to update the  recvBase in the sender instance of the protocol
        this.recvBase = recvBase;

        double x = random.nextDouble();

        //If x is greater than the loss probability then send the ack with the protocol
        if(x > lossProb){
            System.out.println("SENDING ACK FOR PACKET N° : " + packet.seqNumber);
            host.getIPLayer().send(IPAddress.ANY, datagram.src, IP_SR_PROTOCOL, packet);
        }
        else{
            System.out.println("ACK FOR PKT N° : " + packet.seqNumber + " IS LOST");
        }
    }

    /**
     * Receive method of the SRProtocol. This method catches packet in the receiver instance and
     * acks in the sender instance of the protocol. This method is also monitoring the window congestion control
     * and the progression of the emission and reception window in both instance.
     * @param src Source of the protocol
     * @param datagram Datagram of the packet that is received
     * @throws Exception
     */
    @Override
    public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception{

        SRPacket rcvdPacket = (SRPacket) datagram.getPayload();

        //if the received packet is an ack
        if (rcvdPacket.isAnAck){
            //Update the recv base in the sender instance
            this.recvBase = rcvdPacket.recvBase + 1;
            //stop the timer to prevent timeout while processing update of windows
            timers[rcvdPacket.seqNumber].stop();
            changeRTO(rcvdPacket.seqNumber);

            System.out.println("RECEIVED ACK FOR PKT N° " + rcvdPacket.seqNumber);

            //ACK corruption check in case of triple ACKs.
            //Implemented but shouldn't happen in this protocol
            tripleAck[rcvdPacket.seqNumber]++;
            if(tripleAck[rcvdPacket.seqNumber] == 3) {
                System.out.println("WARN: Triple ack");

                //Timeout and refresh packet
                timeout(datagram.src, rcvdPacket.seqNumber);
                tripleAck[rcvdPacket.seqNumber] = 0;

                //update window
                windowSize = windowSize / 2;
                slowStartThresh = windowSize;
                dataExport += host.getNetwork().getScheduler().getCurrentTime() + ", " + windowSize + "\n";
            }


            oldSize = windowSize;
            //Slow start
            if (windowSize < slowStartThresh){
                windowSize ++;
            }
            //Additive increase part
            else {
                windowSize = windowSize + 1/windowSize;
            }
            offset = windowSize - oldSize;

            dataExport += host.getNetwork().getScheduler().getCurrentTime() + ", " + windowSize + "\n";

            //If the ackd packet is in the emission window then set then we can accept it
            if(sendBase <= rcvdPacket.seqNumber && rcvdPacket.seqNumber < sendBase + windowSize) {
                packetLst[sendBase].setAsAcknowledged();

                //If the ackd packet is the first packet in the emission window
                if(rcvdPacket.seqNumber == sendBase) {
                    //then while the packet in the emission window are ackd send data to the application and make the window progress
                    while (packetLst[sendBase].isAcknowledged() && sendBase < packetLst.length - 1) {
                        sendBase++;
                        //start the sending of the new packet.
                        send(packetLst[sendBase].data, datagram.src);
                    }

                }
            }
            //If the sequence number of the expected packet is the last then it's the end of the protocol
            if(recvBase == packetLst.length) {
                FileWriter fw = new FileWriter("SizeOfWindow.csv");
                fw.write(dataExport);
                fw.close();
                System.out.println("THE END");
            }

        }
        //If the received packet is a data packet
        else{
            System.out.println(" RECEIVED PKT N° " + rcvdPacket.seqNumber);
            //if the sequence number of the packet is inside the reception window.
            if (rcvdPacket.seqNumber >= recvBase && rcvdPacket.seqNumber <= recvBase + windowSize - 1){
                //send the ACK for the packet
                sendACK(datagram, recvBase);

                //If the current packet is the first packet in the reception window
                if(recvBase == rcvdPacket.seqNumber){
                    //method to deliver data to the application
                    //make the reception window progress
                    recvBase ++;

                    //Then while there are packets out of orders
                    while (buffer[recvBase - rcvdPacket.seqNumber] != null){
                        //method to deliver data to the application
                        buffer[recvBase - rcvdPacket.seqNumber] = null;
                        recvBase ++;
                    }
                }
                //Else packet is out of order so store it in the buffer
                else{
                    buffer[rcvdPacket.seqNumber - recvBase] = rcvdPacket;
                }
            }
            //Else if the received packet is from a precedent window size then resend the ACK. It means the ack was lost.
            else if (rcvdPacket.seqNumber >= recvBase - windowSize && rcvdPacket.seqNumber <= recvBase - 1){
                sendACK(datagram, recvBase);
            }
        }
    }
}
