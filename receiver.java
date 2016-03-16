import java.util.*;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;


public class receiver{
	@SuppressWarnings("unchecked")
	public static void main(String[] args){

		String filename = args[0];
		int listing_port = Integer.valueOf(args[1]);
		int sender_port = Integer.valueOf(args[3]);
		String log_filename = args[4];
		InetAddress sender_IP = null;
		InetAddress proxy_IP = null;
		int expectseq = 0;
		FileOutputStream finalFile = null;
		FileOutputStream logFileWriterR = null; 

		try{
			finalFile = new FileOutputStream(filename);
			logFileWriterR = new FileOutputStream(log_filename);

		} catch(FileNotFoundException e1){
			System.out.println("Output File not found! ");
			e1.printStackTrace();
		}

		try{
			sender_IP = InetAddress.getByName(args[2]);
			proxy_IP = InetAddress.getByName("localhost");

		}catch(UnknownHostException e){
			System.out.println("Unknown sender IP! ");
			e.printStackTrace();

		}

		byte[] receirverBuffer = new byte[576];
		DatagramSocket receiverSocket = null;
		
		try {
			receiverSocket = new DatagramSocket(listing_port);
			
		} catch (SocketException e) {
			e.printStackTrace();
		}

		DatagramPacket receiverPacket = new DatagramPacket(receirverBuffer, 
			receirverBuffer.length);
		DatagramPacket feedback=null;
		String feedbackMessage = null;
		int ackNumber = 0;
		byte[] combined = null;
		HashMap<Integer, byte[]> bufferOfPackets = new HashMap<Integer, byte[]>();
		HashMap<Integer, byte[]> bufferOfFeedbackPackets = new HashMap<Integer, byte[]>();
		HashMap<Integer, byte[]> bufferOfData = new HashMap<Integer, byte[]>();
		int packetNumber = 1;
		
		receiverSocket.connect(sender_IP, sender_port);
		

		// test connectioning...
		LOOPWHILE:
		while(true){

			byte fin;
			if(!bufferOfPackets.isEmpty()){
				List sortedKeys=new ArrayList(bufferOfPackets.keySet());
				Collections.sort(sortedKeys);
			
				for(int i = 0; i<sortedKeys.size(); i++){
				  	int num = (int)sortedKeys.get(i);
					
					if(num == (expectseq)){
						try{
							finalFile.write(bufferOfPackets.get(num));				

						}catch(IOException e){
							e.printStackTrace();
						}
						
						feedbackMessage = "ACK";
						fin = (byte)(bufferOfData.get(num)[12]);
						
						if(expectseq!=(packetNumber-1)){
							expectseq++;
						}
						combined = createFeedbackPacket(feedbackMessage, num);
						feedback = new DatagramPacket(combined, combined.length);
						bufferOfPackets.remove(num);

						try{
							receiverSocket.connect(sender_IP, sender_port);
							receiverSocket.send(feedback);

						}catch(IOException e ){
							e.printStackTrace();
						}

						if(num==(packetNumber-1)){
							System.out.println("Delivery completed successfully");
							try{
								receiverSocket.close();
								finalFile.close();
			 					logFileWriterR.close();
							}catch(IOException e){
								e.printStackTrace();
							}
							
		 					break LOOPWHILE; 
						}
					
					}
										
				}
			}

			try{
				
				receiverSocket.connect(proxy_IP, 41193);
				receiverSocket.receive(receiverPacket);
				byte[] receiveFile = receiverPacket.getData();
				byte[] fileMsg = Arrays.copyOfRange(receiveFile, 20, receiveFile.length);

				// verify whether squence number == number we expect
				byte[] packetSquInByte = Arrays.copyOfRange(receiveFile, 4, 8);
				byte[] packetNumberInByte = Arrays.copyOfRange(receiveFile, 8, 12);
				packetNumber = byteArrayToInt(packetNumberInByte);
				int packetSqu = byteArrayToInt(packetSquInByte);
				fin = receiveFile[12];
				
				//  write logfile
				Date dr = new Date();
                SimpleDateFormat timer = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String timeReceive = timer.format(dr);

                try{
					String logMsgR = "TimeStamp: " + timeReceive + ", " + " Source: "
				 	 + " IP " + sender_IP +" Port " + sender_port + "," +" Destination: " + "IP "
				 	 + receiverSocket.getInetAddress() + " Port " + listing_port + " Sequence #: "
				 	 + packetSqu + "\n" ;
				 	 byte[] logMsgInbyteR = logMsgR.getBytes();

				 	logFileWriterR.write(logMsgInbyteR);

				}catch(IOException e){
					e.printStackTrace();
				}


				if(packetSqu == expectseq){
					feedbackMessage = "ACK";
					ackNumber = packetSqu;
			
					finalFile.write(fileMsg);
					
					if(packetSqu<(packetNumber-1)){
							expectseq++;
						}
					combined = createFeedbackPacket(feedbackMessage, packetSqu);					 
		 			feedback = new DatagramPacket(combined, combined.length);
					try{
						receiverSocket.connect(sender_IP, sender_port);
						receiverSocket.send(feedback);

					}catch(IOException e ){
						e.printStackTrace();
					}

		 			if(packetSqu == (packetNumber-1)){
		 				System.out.println("Delivery completed successfully");
		 				try{
							receiverSocket.close();
							finalFile.close();
			 				logFileWriterR.close();
						}catch(IOException e){
							e.printStackTrace();
						}

		 				break; 
		 			}
		 			 	
				}else{
					if(!bufferOfPackets.containsKey(packetSqu)){
						if(packetSqu>expectseq){
							bufferOfPackets.put(packetSqu, fileMsg);
							bufferOfData.put(packetSqu,receiveFile);
						}
						
					}
	
					feedbackMessage = "NCK";
					combined = createFeedbackPacket(feedbackMessage, expectseq - 1);
		
				}

				// send feedback packet
				receiverSocket.connect(sender_IP, sender_port);
				List sortedACK = new ArrayList(bufferOfFeedbackPackets.keySet());
				Collections.sort(sortedACK);
				for(int i = 0; i < sortedACK.size(); i++){
					int j = (int)sortedACK.get(i);
					feedback = new DatagramPacket(bufferOfFeedbackPackets.get(j), 
						bufferOfFeedbackPackets.get(j).length);
					receiverSocket.send(feedback);

				}			
			
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
	}


	public static byte[] createFeedbackPacket(String feedbackMessage, int ackNumber){
		byte[] fbMsginByte = feedbackMessage.getBytes();
		byte[] ackNumberInByte = intToByte(ackNumber, 4);
		byte[] combined = new byte[ackNumberInByte.length + fbMsginByte.length];
		System.arraycopy(ackNumberInByte, 0, combined, 0, 4);
		System.arraycopy(fbMsginByte, 0, combined, 4, fbMsginByte.length);

		return combined;
	}


	//   convert byte array to int 
	public static int byteArrayToInt(byte[] b) {
    	return   b[3] & 0xFF |
            	(b[2] & 0xFF) << 8 |
            	(b[1] & 0xFF) << 16 |
            	(b[0] & 0xFF) << 24;
	}


	//  convert int to byte array
	public static byte[] intToByte(int itg, int bytelength ){
		byte[] itb = new byte[bytelength];
		for (int i = 0; i < bytelength; i++) {
		    itb[bytelength-i-1] = (byte)(itg >>> (i * 8));
		}
		return itb;
	}

}

