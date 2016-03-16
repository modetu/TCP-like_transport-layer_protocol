import java.util.*;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;


public class sender{

	@SuppressWarnings("unchecked")
	public static void main(String[] args){
		 ArrayList<byte[]> packetdata = new ArrayList<byte[]>();
		 ArrayList<byte[]> packetList = new ArrayList<byte[]>();

		 String filename = args[0];		 
		 int remote_port = Integer.valueOf(args[2]);
		 int ack_port_num = Integer.valueOf(args[3]);
		 String log_filename = args[4];
		 int window_size;
		 InetAddress remote_IP = null;
		 InetAddress proxy_IP = null;
		 FileOutputStream logFileWriterS = null; 

		try{
			logFileWriterS = new FileOutputStream(log_filename);
		} catch(FileNotFoundException e1){
			e1.printStackTrace();
		}

		try{
		 	remote_IP = InetAddress.getByName(args[1]);
		 	proxy_IP = InetAddress.getByName("localhost");
		 }catch(UnknownHostException e){
		 	System.out.println("unknow receiver IP!");
		 	e.printStackTrace();
		 }
		 

		if(args[5]!=null){
		 	window_size = Integer.valueOf(args[5]);
		 }
		 else {
		 	window_size = 1;
		 }

         File inputfile = new File(filename);
         byte[] fileInByte = fileToByte(inputfile);

         // calculate the number of packets
         int numberOfPacket = 0;
         if(fileInByte.length % 556 ==0){
         	numberOfPacket = fileInByte.length/556;
         }else{
         	numberOfPacket = fileInByte.length/556 + 1;
         }

        // seperate file to data segements
        for(int i = 0; i < numberOfPacket; i++){
        	byte[] data = null;
        	if((i+1)*556<=fileInByte.length){
        		data = Arrays.copyOfRange(fileInByte, 556*i, 556*(i+1));
        		packetdata.add(data);
        	}else{
        		data = Arrays.copyOfRange(fileInByte, 556*i, fileInByte.length);
        		packetdata.add(data);
        	}

        	int sizeofList = packetdata.size();

        }

        // build packets one by one
        for(int i = 0; i < numberOfPacket; i++){
        	byte fin = 0;
        	if (i == (numberOfPacket-1))
        		fin = 1 ;
        	byte[] tsar = createPacket(ack_port_num, remote_port, i, 
        		window_size, packetdata.get(i), fin, numberOfPacket);
        	packetList.add(tsar);
        }


        // send packets and receive feedback
        DatagramSocket sendersocket = null;
       		
		try {
			sendersocket = new DatagramSocket(ack_port_num);
			
		} catch (SocketException e1) {
			e1.printStackTrace();
		}

         sendersocket.connect(remote_IP, remote_port);
         
         DatagramPacket senderpacket = null;
         DatagramPacket getFeedbackPkt = null;
         byte[] getFeedback = new byte[600];
         getFeedbackPkt = new DatagramPacket(getFeedback, getFeedback.length);
         int sendbase = 0;
         int indexnow = 0;
         String feedbackMsg = null;
         byte[] msgInByte = null;
         byte[] feedbackACKNumberInByte = null;
         int feedbackACKNumber = -1;
         int expectACKNumber = 0;
         int retransmitionNum = 0;

         // initial timeout variables
         long timeOut = 2000;
         long estimatedRTT = 2000;
         long devRTT = 0;
         long timeSend = 0;
         long timeReceive = 0;
         HashMap<Integer, Long> timeSendList = new HashMap<Integer, Long>();
         HashMap<Integer, Long> timeReceiveList = new HashMap<Integer, Long>();
         HashMap<Integer, DatagramPacket> waitingPackets = new HashMap<Integer,DatagramPacket>();

         try{
			sendersocket.setSoTimeout((int)estimatedRTT);
         }catch(SocketException e){
         	e.printStackTrace();
         }

         
         // connectiong.....
         while(true){

         	//  timeout estimation 
         	if(!waitingPackets.isEmpty()){
         		long currenttime = System.currentTimeMillis();
         		List sortedPackets = new ArrayList(waitingPackets.keySet());
				Collections.sort(sortedPackets);
				
				for(int i = 0; i < sortedPackets.size(); i++){
					long timePast = (long)(currenttime - timeSendList.get(i));
					if (timePast > timeOut){

						try{
							int num = (int) sortedPackets.get(i);
							
							sendersocket.connect(proxy_IP, 41192);
							sendersocket.send(waitingPackets.get(num));
							timeSend = System.currentTimeMillis();
							timeSendList.put(num, timeSend);
							retransmitionNum++;

							// write logfile
							Date ds = new Date();
							SimpleDateFormat times = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							String timeSends = times.format(ds);
							String logMsgS = "TimeStamp: " + timeSends + ", " + " Source: "
												+ " IP " + sendersocket.getInetAddress() +" Port " 
												+ ack_port_num + "," +" Destination: " + "IP "
												+ remote_IP + " Port " + remote_port + " Sequence #: "
											    + num + " EstimatedRTT: " + estimatedRTT + "\n" ;
							byte[] logMsgInbyteS = logMsgS.getBytes();

							logFileWriterS.write(logMsgInbyteS);

						}catch(IOException e){
							e.printStackTrace();
						}
												
					}
				}
         	}

      
         	// send packets         		
         	if(indexnow<=(sendbase+window_size)){
         		byte[] packet = packetList.get(indexnow);
         		senderpacket = new DatagramPacket(packet, packet.length);
         										       
		        try{
		         	sendersocket.connect(proxy_IP, 41192);
		         	sendersocket.send(senderpacket);
		       		timeSend = System.currentTimeMillis();
		       		timeSendList.put(indexnow, timeSend);
		       		waitingPackets.put(indexnow, senderpacket);
		       		
	        		// write logfile
					Date dso = new Date();
					SimpleDateFormat timeso = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String timeSendo = timeso.format(dso);
					String logMsgSo = "TimeStamp: " + timeSendo + ", " + " Source: "
								+ " IP " + sendersocket.getInetAddress() +" Port " 
					 			+ ack_port_num + "," +" Destination: " + "IP "
								+ remote_IP + " Port " + remote_port + " Sequence #: "
								+ indexnow  + " EstimatedRTT: " + estimatedRTT + "\n";
					byte[] logMsgInbyteSo = logMsgSo.getBytes();

					logFileWriterS.write(logMsgInbyteSo);
		         		
		         	}catch(IOException e){
		         		e.printStackTrace();
		         	}
		         	if(indexnow!=(numberOfPacket-1)){
		         		indexnow++;
		         	}
		         	
         	}
         		
         		// receive feedback packet
         	try{
         		sendersocket.connect(remote_IP, remote_port);
         		try{
					sendersocket.receive(getFeedbackPkt);
         		}catch(SocketTimeoutException e){

         		}
         		
         	}catch(IOException e){
         		e.printStackTrace();
         	}
         		
	       	// seperate feedback message received from receiver
	       	byte[] feedbackMsgInByte = getFeedbackPkt.getData();
	       	feedbackACKNumberInByte = Arrays.copyOfRange(feedbackMsgInByte, 0, 4); 
	       	feedbackACKNumber = byteArrayToInt(feedbackACKNumberInByte);
	       	msgInByte = Arrays.copyOfRange(feedbackMsgInByte, 4, 
	        		feedbackMsgInByte.length-1);
	       	String feedbackMsgini = new String(msgInByte, 0, msgInByte.length);
	       	feedbackMsg = feedbackMsgini.substring(0,3);
	         	
	       	// Analyze feedback message
        	if((feedbackMsg.equals("ACK"))&&(feedbackACKNumber == expectACKNumber)){
	       		if(expectACKNumber!=(numberOfPacket-1)){
	       			expectACKNumber++;
	       		}
	         		
	      		sendbase++;

	         	// calculate RTT
	         	timeReceive = System.currentTimeMillis();
	         	timeReceiveList.put(feedbackACKNumber, timeReceive);
	       		long timeSendFlag = timeReceiveList.get(feedbackACKNumber);
	         	estimatedRTT = (long)(0.875 * estimatedRTT + 0.125 * (timeReceive - timeSendFlag));
	       		devRTT = (long)(0.75 * devRTT + 0.25 * Math.abs((timeReceive - 
	       			timeSendFlag)-estimatedRTT));
	         	timeOut = (long)(estimatedRTT + 4 * devRTT);

	         	if(waitingPackets.containsKey(feedbackACKNumber)){
	         		waitingPackets.remove(feedbackACKNumber);
	         	}

	         	// transmit finish!
	         	int totalByte = 576*(retransmitionNum+numberOfPacket);
	         	if(feedbackACKNumber==(numberOfPacket-1)){
	         		System.out.println("Delivery completed successfully");
         			System.out.println("Total bytes sent = " + totalByte);
         			System.out.println("Segments sent: " + numberOfPacket);
         			System.out.println("Segments retransmitted = "+retransmitionNum);

         			try{
						sendersocket.close();
	         			logFileWriterS.close();

         			}catch(IOException e){
         				e.printStackTrace();
         			}
         			
         			break;
	         	}
	         			

         	}
         	         	
        }
                                
	}


	// convert file to byte array
   	public static byte[] fileToByte(File inputfile){

         byte[] wholeData;
         InputStream is = null;
         ByteArrayOutputStream bos = null;
         try{
	         byte[] buffer = new byte[4096];
	         is = new FileInputStream(inputfile);
	         bos = new ByteArrayOutputStream();
	         int read = 0;
	         while((read = is.read(buffer)) != -1){
	         	bos.write(buffer, 0, read);
	         }
         }catch(FileNotFoundException e){
         	System.out.println("File not found!");
         	e.printStackTrace();
         }catch(IOException e){
         	e.printStackTrace();

         }finally{
         	try{
         		is.close();
         		bos.close();
         	}catch(IOException e){
         		e.printStackTrace();
         	}
         	
         }

         wholeData = bos.toByteArray();

         return wholeData; 

   	}


   	// build one packet
	public static byte[] createPacket(int srcpt, int destpt, 
		int squenum, int wdsz, byte[] data, byte fin, int packetN){
		byte[] packet = new byte[576];
		byte[] sourcePort = intToByte(srcpt, 2);
		byte[] destPort = intToByte(destpt, 2);
		byte[] sequenceNumber = intToByte(squenum, 4);
		byte[] windowSize = intToByte(wdsz, 2);
		byte[] packetNum = intToByte(packetN, 4);
		packet[12] = fin;

		try{
			System.arraycopy(sourcePort, 0, packet, 0, 2);
			System.arraycopy(destPort, 0, packet, 2, 2);
			System.arraycopy(sequenceNumber, 0, packet, 4, 4);
			System.arraycopy(windowSize, 0, packet, 14, 2);
			System.arraycopy(data, 0, packet, 20, data.length);
			System.arraycopy(packetNum, 0, packet, 8, 4);

		}catch(IndexOutOfBoundsException e){
			System.out.println("Index Out Of Bound! Please Check!");
			e.printStackTrace();
		}catch(ArrayStoreException e){
			System.out.println("Copy Error! "
					+ "Element in the src array could not be stored into the dest array "
					+ "because of a type mismatch!");
			e.printStackTrace();
		}catch(NullPointerException e){
			System.out.println("Either src array or dest array is null!");
			e.printStackTrace();
		}
		
		return packet;


	}
 

	// convert int to byte array
	public static byte[] intToByte(int itg, int bytelength ){
		byte[] itb = new byte[bytelength];
		for (int i = 0; i < bytelength; i++) {
		    itb[bytelength-i-1] = (byte)(itg >>> (i * 8));
		}
		return itb;

	}


	//   convert byte array to int 
	public static int byteArrayToInt(byte[] b) {
    	return   b[3] & 0xFF |
            	(b[2] & 0xFF) << 8 |
            	(b[1] & 0xFF) << 16 |
            	(b[0] & 0xFF) << 24;
	}



}