package reso.examples.selectiveRepeatv3;

import reso.examples.selectiverepeat.SRMessage;
import reso.ip.*;

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

    // CONSTRUCTORS AND METHODS

    public SRProtocol(IPHost host) throws Exception{
        this.host = host;
        host.getIPLayer().addListener(this.IP_SR_PROTOCOL, this);
        //this.lossProb = lossProb;
    }

    public SRProtocol(IPHost host, SRPacket[] packetLst){
        this.host = host;
        this.packetLst = packetLst;
        this.buffer = new SRPacket[packetLst.length];
        this.acks = new Datagram[packetLst.length];
        host.getIPLayer().addListener(this.IP_SR_PROTOCOL, this);
        //this.lossProb = lossProb;
    }


    public void send(int data, IPAddress dst) throws Exception{
        if (seqNumber < sendBase + size && seqNumber < packetLst.length){
            SRPacket packet = new SRPacket(data, seqNumber);
            packetLst[seqNumber] = packet;

            //determine loss prob

            System.out.println("SENDING PACKET N° : " + seqNumber);
            host.getIPLayer().send(IPAddress.ANY, dst, IP_SR_PROTOCOL, packet);

            //démarer le timer dans le cas où le seqNumber est le premier packet de la window

            if (seqNumber == sendBase){
                //TODO create timer for the packet
                //TODO start timer
            }
            seqNumber ++;
        }
    }

    public void sendACK(Datagram datagram) throws Exception{
        SRPacket packet = new SRPacket(((SRPacket) datagram.getPayload()).seqNumber);

        //loss prob for the ack

        System.out.println("SENDING ACK FOR PACKET N° : " + packet.seqNumber);
        host.getIPLayer().send(IPAddress.ANY, datagram.src, IP_SR_PROTOCOL, packet);

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
