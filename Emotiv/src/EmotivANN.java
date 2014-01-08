
/**
 * Created with IntelliJ IDEA.
 * User: Phips
 * Date: 17.12.13
 * Time: 11:53
 * To change this template use File | Settings | File Templates.
 */


import com.dkriesel.snipe.core.NeuralNetwork;
import com.dkriesel.snipe.core.NeuralNetworkDescriptor;
import com.dkriesel.snipe.training.TrainingSampleLesson;
import com.dkriesel.snipe.neuronbehavior.NeuronBehavior;

import java.text.DecimalFormat;


public class EmotivANN {
    private NeuralNetworkDescriptor desc;
    private int indexOutput, lastHiddenNeuron;
    private NeuralNetwork net;
    private boolean isInitialized, isTrained;

   /*
   * Plan:
   * 3 Layer
   * input 4 - 5 neuronen (4 Input electrodes / & 1 State Input?)
   * 1-n @hidden layer for states
   * 1 output layer (=> returns state or 0)
   * better method for tracking Outputneuron is needed!!
   * --------------------------------------
   * startet mit 0 hidden layer neutron
   * neutron enthält state + microvolt + abweichung --> wie neuron konfigurieren?
   * hiddenlayer neutron schlägt an wenn volt pattern ähnlich (gleich?) ist und gibt state an
   * output layer weiter
   * während training werden neutronen eingefügt
   * ---------------------------------------
   * methoden für training:
   * isTrainingDone
   * TrainingStart / Init
   * setTrainingDone (ANN umbauen --> 4 input)
   * insertStateNeutronWhileTraining
   *
   * !!! only inner neurons removable !!!
   * */
    /* Todo:
    *  - configure inner neurones while training
    *   -- set synapses???   input -> new hidden -> output?
    *   -- set inner neurones behavior
    *   -- multiple values  possible?
    *   -- initialize values of newly added neuron with parameters via training
    *
    *  - GUI
    * */

    EmotivANN(int inputNeurons){
            this.initANN(inputNeurons);
    }

    public int resetANN(int inputNeurons){
        try{
            this.initANN(inputNeurons);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return -1;
        }
        return 1;
    }

    private void initANN(int inputNeurons){
        isInitialized = false;
        /* Initialize ANN with 5 Input, 0 hidden and  1 output neuron */
        desc = new NeuralNetworkDescriptor(inputNeurons,0,1);
        desc.setInitializeAllowedSynapses(false);      // |--> needed??
        desc.setSettingsTopologyFeedForward();     // |

        // Create Network
        net = desc.createNeuralNetwork();

        net.createSynapsesAllowed();

        for(int i=0;i < inputNeurons*10; i++) this.addNeuron();
        isInitialized = true;
        isTrained = false;

        this.refreshOutputIndex();
    }
    private void refreshOutputIndex(){
        indexOutput = net.countNeuronsInLayer(0) + net.countNeuronsInLayer(1) + 1;

        if(net.countNeuronsInLayer(0) + net.countNeuronsInLayer(1) > 0)
        {
           //net.createSynapsesFromLayerToLayer(0,1);
           //net.createSynapsesFromLayerToLayer(1,2);
        }

        //Debug:
      /* System.out.println("in:" + net.countNeuronsInLayer(0) + " hidden:" + net.countNeuronsInLayer(1) + " out:" + net.countNeuronsInLayer(2) + " Index:" + indexOutput);
        System.out.println("Synapses: " + net.countSynapses());  */
    }
    public void setTrainingDone(){
        if(!isTrained && isInitialized){
            isTrained = true;
        }
    }

    /*return:
    * 0 => all went well
    * -1 => error
    * */
     private int removeLastNeuron(){
         try
         {
             if( net.countNeuronsInLayer(1) > 0 ){
                 /*net.removeSynapsesFromLayerToLayer(0,1);
                 net.removeSynapsesFromLayerToLayer(1,2); */
                 net.removeNeuron(lastHiddenNeuron);
                 lastHiddenNeuron = net.countNeuronsInLayer(0) + net.countNeuronsInLayer(1);
             }
         }
         catch(Exception e)
         {
             e.printStackTrace();
             return -1;
         }
         this.refreshOutputIndex();
         return 1;
     }
     private int addNeuron(){
       try
       {
            lastHiddenNeuron = net.createNeuronInLayer(1);          //'save' indices of hidden neurons ?      */

            this.refreshOutputIndex();
       }
       catch( Exception e)
       {
           e.printStackTrace();
           return -1;
       }

       return 0;
    }
    public int trainingLesson(double[][] values, double[][] state){

        /* values/state ... 1st D. lessons; 2nd D. values
        *  values ... double values from EMOTIV
        *  state ... code for result => STATE (excited, etc.)
        * */
        if(isInitialized){

            TrainingSampleLesson TrainingLesson = new TrainingSampleLesson(invertDoubleArray(values),state);

            net.trainResilientBackpropagation(TrainingLesson, 128, true);

            return 0;
        }
        return -1;
    }
    private double[][] invertDoubleArray(double[][] array){
        double[][] tempArray = new double[array[0].length][array.length];
        for(int i = 0; i < array.length; i++){
            for(int j = 0; j < array[0].length; j++){
                tempArray[j][i] = array[i][j];
            }
        }
        return tempArray;
    }
    public void getResultStateOfMightyMagicInputYo(double[][] input) {
        DecimalFormat df = new DecimalFormat("#.#");
        input = invertDoubleArray(input);
        for (double[] anInput : input) {
            double[] output = net.propagate(anInput);
            for (double anOutput : output) {
                System.out.print(df.format(anOutput) + "\t");
            }
            System.out.println("");
        }
        /*
        NeuralNetwork net2 = desc.createNeuralNetwork();
        try {
            String test =  net.exportToString();
            System.out.println(test);
            net2.importFromString(test);
            System.out.println(net2.exportToString());
        } catch (Exception e) {
            e.printStackTrace();
        }*/


    }
    public String getState(double alpha, double beta, double delta, double Theta, String state){
        if (isTrained)    {
            state = null;   //state after training no more necessary
            //do stuff with ANN
            return "result";
        }

        return "Network is not trained or initialized yet!";
    }
    public String getNet(){
        return net.exportToString();
    }
    public void setNet(String s) throws Exception {
        net.importFromString(s);
        isInitialized = true;
    }


}
