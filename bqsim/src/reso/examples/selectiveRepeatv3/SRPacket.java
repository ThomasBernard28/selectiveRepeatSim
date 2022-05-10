package reso.examples.selectiveRepeatv3;

import reso.common.Message;

public class SRPacket implements Message {

    public final int data;
    public final int seqNumber;
    public final boolean isAnAck;

    /**
     * Constructor for a packet
     * @param data
     * @param seqNumber
     */
    public SRPacket(int data, int seqNumber){
        this.data = data;
        this.isAnAck = false;
        this.seqNumber = seqNumber;
    }

    /**
     * Constructor for an ACK
     */
    public SRPacket(int seqNumber){
        this.seqNumber = seqNumber;
        this.isAnAck = true;
        this.data = -1;
    }

    public String toString(){
        return "Packet [seq number = " + seqNumber + ", Is an ACK = " + isAnAck + "]";
    }

    @Override
    public int getByteLength(){
        //Selective Repeat Packet carries a single integer in our case;
        return Integer.SIZE/8;
    }
}
