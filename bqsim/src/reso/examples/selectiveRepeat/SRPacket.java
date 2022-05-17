package reso.examples.selectiveRepeat;

import reso.common.Message;

public class SRPacket implements Message {

    public final int data;
    public final int seqNumber;
    public final boolean isAnAck;
    public int recvBase;

    public boolean acknowledged = false;

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
    public SRPacket(int seqNumber, int recvBase, boolean isAnAck){
        this.seqNumber = seqNumber;
        this.isAnAck = true;
        this.data = -1;
        this.recvBase = recvBase;
    }

    public boolean isAnAck() {
        return this.isAnAck;
    }

    public boolean isAcknowledged() { return this.acknowledged; }

    public void setAsAcknowledged() {
        this.acknowledged = true;
    }

    public String toString(){
        return "Packet [seq number = " + seqNumber + ", Is an ACK = " + isAnAck + "]";
    }

    @Override
    public int getByteLength(){
        //Selective Repeat Packet carries a single integer in our case;
        return isAnAck ?  4 : Integer.SIZE/8 + 4;
    }
}
