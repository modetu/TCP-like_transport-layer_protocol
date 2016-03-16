**********************************************
        Name: Yaxin Wang
          UNI: yw2770
**********************************************
  Demo video: https://youtu.be/A4nFShL5_Uw
  

**********************************************
	Development Enviroment
**********************************************
Java version Message:
  * Java version "1.8.0_60”;
  * Java(TM) SE Runtime Environment (build 1.8.0_60-b27);
  * Java HotSpot(TM) 64-Bit Server VM (build 25.60-b23, mixed mode);
Proxy Message:
  * newudpl-1.4, a Network Emulator With UDP Link
  * More information at: http://www.cs.columbia.edu/~hgs/research/projects/newudpl/newudpl-1.4/newudpl.html


***********************************************
        How to compile and run
***********************************************
1, First of all, you need to install proxy provided by cs.columbia: http://www.cs.columbia.edu/~hgs/research/projects/newudpl/

2, In terminal, cd to the folder of my assignment.
3, Type “make” or type “javac *.java” in terminal to compile.

4,To run receiver, type “java receiver <filename> <listening_port> <sender_IP> <sender_port> <log_filename>”;
  * sender_IP is the IP of sender, NOT proxy;
  * sender_port is the port of sender, NOT proxy;
  * filename is the file you want to write of the packets you received, this file will be created in the current directory;
  * For example: type “java receiver out.txt 20000 localhost 20001 logfiler.txt” in
    terminal;

5,To run sender, type “java sender <filename> <remote_IP> <remote_port> <ack_port_num> <log_filename> <window_size>”;
  * remote_IP is the IP of receiver, NOT proxy;
  * remote_port is the IP of receiver, NOT proxy;
  * filename if the file which you want to send, this file should be in the current directory;
  * For example, type “java sender testp localhost 20000 20001 logfiles.txt 5” in
    terminal;


************************************************
        Functions realized
************************************************
1,In order delivery;
2,Packet loss handled;
3,Packet delays handled;
4,Duplicate packets handled;
5,Log file on sender and receiver;
6,Statistics on sender and receiver printed on terminal when transmission finished;
7,Variable window size supported;


*************************************************
        About this TCP protocol
*************************************************
1,TCP Segment Structure:
  * 20-bytes long in header and 556-bytes long in data, one segment has 576 bytes in 	    
    total;
  * For each segment byte array packet[576]: 
       packet[0] to packet[1] is source port number;
       packet[2] to packet[3] is dest port number;
       packet[4] to packet[7] is sequence number;
       packet[8] to packet[11] is packet number;
       packet[12] is fin;
       packet[14] to packet[15] is window size;
        
2,Use model similar to Go-Back-N to maintain the order of packets;


**************************************************
 	Typical States of Sender and Receiver
**************************************************
1,Sender:
  * Checking timeout of packets
  * Waiting for ACK and send packets 

2,Receiver:
  * Checking ACKPackets buffer and send feedback packets and write file if ACKnumber is  
    correct in order;
  * Waiting for packets and choose to send feedback and write file immediately or put it 
    into buffer according to the correction of order in sequence number;


**************************************************
       Loss Recovery Mechanism
**************************************************
1, If an packet get lost, wait for timeout and this packet will retransmit;


**************************************************
	Other Tips of this Simple TCP Protocol
**************************************************
1, This program is set the IP of proxy is localhost, and the ports of proxy is 41192 and 
   41193, which means that the IP and ports are fixed and cannot change if we do not 
   change the code. Thus, If your the IP and ports number proxy are not listed above, 
   please change the value of proxy_IP and port number in sendersocket.connect(),
   receiverSocket.connect() methods. 

2, The sequence number of packets indicate the order of packets. For example, we separate 
   data to 14 packets and then the sequence number of each packet is 0, 1, 2, 3, ……, 13;

3, We can set the window size corresponds to the number of packets. It is of no 
   significance to set window size larger than the number of total packets. For example, 
   we can set window size of 1, 2, ……, the number of total packets;

4, The proxy I use is newudpl-1.4, please see the details in this link: http://www.cs.columbia.edu/~hgs/research/projects/newudpl/newudpl-1.4/newudpl.html

5, Please ignore the SocketTimeoutException in sender side, it has NO influence on the 
   correctness of output.

6, Both sender and receiver will write logfiles in current directory. 


Thanks a lot.
	
