import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.omg.CORBA.MARSHAL;

import java.awt.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;


public class EEGLog {

    public static final int MAX_COUNT = 128;
    public static final int SENSOR_COUNT = 14;
    private static EmotivANN net;


    public void startEEG() throws IOException {
        int choice;
        Scanner s = new Scanner(System.in);

        net = new EmotivANN(SENSOR_COUNT);

        while(true){
            System.out.println("1 - Run mode\n2 - Learn mode (relaxed)\n3 - Learn mode (not relaxed)\n4 - Export\n" +
                    "5 - Import\n6 - Exit\nEnter your command: ");
            choice = s.nextInt();

            switch(choice){
                case 1:
                case 2:
                case 3: run(choice);break;
                case 4: this.exportANN(); break;
                case 5: this.importANN(); break;
                case 6: return;
                default: System.out.println("Wrong Option\n");
            }

        }
    }
    public void run(int mode){

        Pointer eEvent				= Edk.INSTANCE.EE_EmoEngineEventCreate();
        Pointer eState				= Edk.INSTANCE.EE_EmoStateCreate();
        IntByReference userID 		= null;
        IntByReference nSamplesTaken= null;
        short composerPort			= 1726;
        int option 					= 1;
        int state  					= 0;
        float secs 					= 1;
        boolean readytocollect 		= false;
        boolean firstLog            = true;
        userID 			= new IntByReference(0);
        nSamplesTaken	= new IntByReference(0);
        double[][] dataArray = new double[SENSOR_COUNT][MAX_COUNT];
        double[] imagArray = new double[128];
        int valCount = 0, i;
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        Date startDate, currentDate;
        startDate = new Date();

        for(i = 0; i < MAX_COUNT; i++){
            imagArray[i] = 0.0;
        }

        switch (option) {
            case 1:
            {
                if (Edk.INSTANCE.EE_EngineConnect("Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
                    System.out.println("Emotiv Engine start up failed.");
                    return;
                }
                break;
            }
            case 2:
            {
                System.out.println("Target IP of EmoComposer: [127.0.0.1] ");

                if (Edk.INSTANCE.EE_EngineRemoteConnect("127.0.0.1", composerPort, "Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
                    System.out.println("Cannot connect to EmoComposer on [127.0.0.1]");
                    return;
                }
                System.out.println("Connected to EmoComposer on [127.0.0.1]");
                break;
            }
            default:
                System.out.println("Invalid option...");
                return;
        }

        Pointer hData = Edk.INSTANCE.EE_DataCreate();
        Edk.INSTANCE.EE_DataSetBufferSizeInSec(secs);
        System.out.print("Buffer size in secs: ");
        System.out.println(secs);

        System.out.println("Start receiving EEG Data at "+ df.format(startDate) +"!");
        while (true)
        {
            state = Edk.INSTANCE.EE_EngineGetNextEvent(eEvent);

            // New event needs to be handled
            if (state == EdkErrorCode.EDK_OK.ToInt())
            {
                int eventType = Edk.INSTANCE.EE_EmoEngineEventGetType(eEvent);
                Edk.INSTANCE.EE_EmoEngineEventGetUserId(eEvent, userID);

                // Log the EmoState if it has been updated
                if (eventType == Edk.EE_Event_t.EE_UserAdded.ToInt())
                    if (userID != null)
                    {
                        System.out.println("User added");
                        Edk.INSTANCE.EE_DataAcquisitionEnable(userID.getValue(),true);
                        readytocollect = true;
                    }
            }
            else if (state != EdkErrorCode.EDK_NO_EVENT.ToInt()) {
                System.out.println("Internal error in Emotiv Engine!");
                break;
            }

            if (readytocollect && !firstLog)
            {
                Edk.INSTANCE.EE_DataUpdateHandle(0, hData);

                Edk.INSTANCE.EE_DataGetNumberOfSample(hData, nSamplesTaken);

                if (nSamplesTaken != null)
                {
                    if (nSamplesTaken.getValue() != 0) {



                        double[] data = new double[nSamplesTaken.getValue()];
                        for (int sampleIdx=0 ; sampleIdx<nSamplesTaken.getValue() ; ++ sampleIdx) {
                            for (i = 0 ; i < SENSOR_COUNT ; i++) {

                                Edk.INSTANCE.EE_DataGet(hData, i, data, nSamplesTaken.getValue());
                                //Subtract 4200 to show positive/negative values
                                dataArray[i][valCount] = data[sampleIdx] - 4200;
                            }
                        }
                    }
                }
                if(++valCount == MAX_COUNT){

                    if(mode != 1){
                    currentDate = new Date();
                        if((currentDate.getTime() - startDate.getTime()) >= 8000){
                            System.out.println("Training ended at " + df.format(currentDate));
                            return; //8000 milliseconds -> 8 seconds yo
                        }
                    }
                    double[][] stateArray = new double[MAX_COUNT][SENSOR_COUNT];
                    for(i = 0; i < MAX_COUNT; i++) Arrays.fill(stateArray[i], (double) ((mode==3)?1:0));
                    for(i = 0; i < SENSOR_COUNT; i++){
                        FastFourierTransform.fastFT(dataArray[i], imagArray, true);

                    }
                    if(mode == 1) net.getResultStateOfMightyMagicInputYo(dataArray); // stuff
                    else net.trainingLesson(dataArray, stateArray);

                    for(i = 0; i < MAX_COUNT; i++){
                        for(int j = 0; j < SENSOR_COUNT; j++){
                            //f.write(String.valueOf(dataArray[j][i]) + ",");
                            dataArray[j][i] = 0.0;
                        }
                        //f.write("\n");
                    }
                    valCount = 0;
                }
            }
            firstLog = false;
        }

        //f.close();

        Edk.INSTANCE.EE_EngineDisconnect();
        Edk.INSTANCE.EE_EmoStateFree(eState);
        Edk.INSTANCE.EE_EmoEngineEventFree(eEvent);
        System.out.println("Disconnected!");

    }
    public void exportANN(){
        try {

            File logFile;
            FileWriter f;
            Scanner s = new Scanner(System.in);
            System.out.println("Enter file destination: ");
            logFile = new File(s.nextLine());
            f = new FileWriter(logFile);
            f.write(net.getNet());
            f.close();
        } catch (IOException e) {
            System.out.println("File not found!");
        }
    }
    public void importANN(){
        Scanner s = new Scanner(System.in);
        System.out.println("Enter file source: ");
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(s.nextLine()));
            net.setNet(reader.readLine());
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }

}
