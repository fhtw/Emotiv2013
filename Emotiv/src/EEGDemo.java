
/**
 * Created with IntelliJ IDEA.
 * User: Phips
 * Date: 25.11.13
 * Time: 22:13
 * To change this template use File | Settings | File Templates.
 */

import com.dkriesel.snipe.core.NeuralNetwork;
import com.dkriesel.snipe.core.NeuralNetworkDescriptor;
import com.dkriesel.snipe.util.GraphVizEncoder;

import java.io.IOException;

public class EEGDemo {
    public static void main(String[] args) throws IOException {
        System.setProperty("jna.library.path", "C:\\Users\\Phips\\Documents\\FH\\Laptop\\Semester3\\Emotiv\\src\\Emo" +
                "tiv_filter\\dlls");

        EEGLog log = new EEGLog();
        log.startEEG();
    }
}
