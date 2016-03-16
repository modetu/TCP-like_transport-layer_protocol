JC = javac
JFLAGS = -g
default: receiver.class sender.class

receiver.class: receiver.java
	$(JC) $(JFLAGS) receiver.java
sender.class: sender.java
	$(JC) $(JFLAGS) sender.java

clean:
	$(RM) *.class
	$(JC) $(JFLAGS) sender.java
	$(JC) $(JFLAGS) receiver.java

		
