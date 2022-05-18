package reso.examples.selectiveRepeat;

import reso.common.Message;

/**
 * This class defines a Selective Repeat Packet which contains
 * all information about the packet
 */
public class SRPacket implements Message {

    /**
     * The data contained in the packet.
     */
    public final int data;
    /**
     * The sequence number of the packet
     */
    public final int seqNumber;

    /**
     * A boolean to know if the packet is a data packet or and ack
     */
    public final boolean isAnAck;

    /**
     * The sequence number of the packet expected in the receiver part
     */
    public int recvBase;

    /**
     * A boolean to know if the packet has been acknowledged or not.
     */
    public boolean acknowledged = false;

    /**
     * Constructor for a data packet
     * @param data the data
     * @param seqNumber the sequence number of the packet
     */
    public SRPacket(int data, int seqNumber){
        this.data = data;
        this.isAnAck = false;
        this.seqNumber = seqNumber;
    }

    /**
     * Constructor for an ACK
     * @param seqNumber sequence number of the ack packet
     * @param recvBase sequence number of the packet expected in the receiver window.
     * @param isAnAck boolean to know if the packet is an ack, always true for the constructor
     */
    public SRPacket(int seqNumber, int recvBase, boolean isAnAck){
        this.seqNumber = seqNumber;
        this.isAnAck = true;
        this.data = -1;
        this.recvBase = recvBase;
    }

    /**
     * Method to check if the packet is ack packet or a data packet
     * @return true if so, else return false
     */
    public boolean isAnAck() {
        return this.isAnAck;
    }

    /**
     * Method to check if the packet has been acknowledged
     * @return true if so, false if not
     */
    public boolean isAcknowledged() { return this.acknowledged; }

    /**
     * Setter
     */
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
