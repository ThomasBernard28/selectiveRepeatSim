package reso.examples.selectiverepeat;

import reso.common.Message;

public class SelectiveRepeatMessage implements Message {

    protected final int data;

    public SelectiveRepeatMessage(int data){
        this.data = data;
    }

    public String toString(){
        return "SelectiveRepeat [data=" + data + "]";
    }

    @Override
    public int getByteLength(){
        // The selective repeat carries a single 'int'
        return Integer.SIZE  / 8;
    }
}
