package reso.examples.selectiverepeat;

import reso.ip.Datagram;
import reso.ip.IPAdress;
import reso.IPHost;
import reso.ip.IPInterfaceAdapter;
import reso.ip.IPInterfaceListener;


public class SelectiveRepeatProtocol implements IPInterfaceListener{

	private final IPHost host;

	public SelecticeRepeatProtocol(IPHost host){
		this.host = host
	}
}
