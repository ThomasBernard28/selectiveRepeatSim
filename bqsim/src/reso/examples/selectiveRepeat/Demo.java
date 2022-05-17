package reso.examples.selectiveRepeat;

import reso.common.Link;
import reso.common.Network;
import reso.ethernet.EthernetAddress;
import reso.ethernet.EthernetFrame;
import reso.ethernet.EthernetInterface;
import reso.ip.IPAddress;
import reso.ip.IPEthernetAdapter;
import reso.ip.IPHost;
import reso.scheduler.AbstractScheduler;
import reso.scheduler.Scheduler;
import reso.utilities.NetworkBuilder;

import java.util.Scanner;

public class Demo {

    public static void main(String[] args) {
        AbstractScheduler scheduler = new Scheduler();
        Network network = new Network(scheduler);


        System.out.println("#################################");
        System.out.println("|                               |");
        System.out.println("|   Selective Repeat Protocol   |");
        System.out.println("|           Simulator           |");
        System.out.println("|                               |");
        System.out.println("|                               |");
        System.out.println("|Bernard Thomas & Houba Augustin|");
        System.out.println("|                               |");
        System.out.println("#################################\n\n");

        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter the number of packet you want to send : ");
        int packetNbr = scanner.nextInt();
        //int packetNbr = 2000;

        System.out.println("Enter the link length (in km) : ");
        int linkLength = scanner.nextInt();
        //int linkLength = 1000;

        System.out.println("Enter the bitrate : ");
        int bitrate = scanner.nextInt();
        //int bitrate = 1000000;

        System.out.println("Enter the loss probability of packets (from 0,00 to 1,00) : ");
        double lossProb = scanner.nextDouble();
        //double lossProb = 0.1;

        try{
            final EthernetAddress MAC_ADDR1= EthernetAddress.getByAddress(0x00, 0x26, 0xbb, 0x4e, 0xfc, 0x28);
            final EthernetAddress MAC_ADDR2= EthernetAddress.getByAddress(0x00, 0x26, 0xbb, 0x4e, 0xfc, 0x29);
            final IPAddress IP_ADDR1= IPAddress.getByAddress(192, 168, 0, 1);
            final IPAddress IP_ADDR2= IPAddress.getByAddress(192, 168, 0, 2);

            IPHost host1= NetworkBuilder.createHost(network, "H1", IP_ADDR1, MAC_ADDR1);
            host1.getIPLayer().addRoute(IP_ADDR2, "eth0");
            host1.addApplication(new AppSender(host1, IP_ADDR2, packetNbr, lossProb));

            IPHost host2= NetworkBuilder.createHost(network,"H2", IP_ADDR2, MAC_ADDR2);
            host2.getIPLayer().addRoute(IP_ADDR1, "eth0");
            host2.addApplication(new AppReceiver(host2, lossProb));

            EthernetInterface h1_eth0= (EthernetInterface) host1.getInterfaceByName("eth0");
            EthernetInterface h2_eth0= (EthernetInterface) host2.getInterfaceByName("eth0");

            //Connect both interface with custom parameters from the user
            new Link<EthernetFrame>(h1_eth0, h2_eth0, linkLength, bitrate);

            ((IPEthernetAdapter) host2.getIPLayer().getInterfaceByName("eth0")).addARPEntry(IP_ADDR1, MAC_ADDR1);
            ((IPEthernetAdapter) host1.getIPLayer().getInterfaceByName("eth0")).addARPEntry(IP_ADDR2, MAC_ADDR2);

            host1.start();
            host2.start();

            scheduler.run();
        }catch (Exception e){
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
