
package reso.examples.selectiverepeat;

import reso.common.Message;

public class SRMessage implements Message {

    public final int[] data;
    public final int sequenceNumber;
    public final boolean ackD;

    public SRMessage(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
        this.ackD = true;
        this.data = new int[]{};
    }

    public SRMessage(int[] data, int sequenceNumber){
        this.data = data;
        this.ackD = false;
        this.sequenceNumber = sequenceNumber;
    }

    public String toString(){
        return "Packet [seq number=" + sequenceNumber + ", AckD=" + ackD + "]";
    }

    public boolean isAckD(){
        return this.ackD;
    }

    @Override
    public int getByteLength(){
        // Selective Repeat Message carries an array of int through TCP.
        return 4*this.data.length+1;
    }
}

